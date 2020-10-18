/*
 * Copyright © 2020 Mark Raynsford <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.ironstrata.printer.vanilla.internal;

import com.io7m.ironstrata.printer.api.ISPrinterCommandGCode;
import com.io7m.ironstrata.printer.api.ISPrinterCommandQueueType;
import com.io7m.ironstrata.printer.api.ISPrinterCommandType;
import com.io7m.ironstrata.printer.api.ISPrinterEventCommandFailed;
import com.io7m.ironstrata.printer.api.ISPrinterEventCommandSucceeded;
import com.io7m.ironstrata.printer.api.ISPrinterEventFatalError;
import com.io7m.ironstrata.printer.api.ISPrinterEventOnlineStateChanged;
import com.io7m.ironstrata.printer.api.ISPrinterEventTemperaturesChanged;
import com.io7m.ironstrata.printer.api.ISPrinterEventType;
import com.io7m.ironstrata.printer.api.ISPrinterException;
import com.io7m.ironstrata.printer.api.ISPrinterExceptionUnsupported;
import com.io7m.ironstrata.printer.api.ISPrinterTemperatures;
import com.io7m.ironstrata.printer.vanilla.ISSerialPrinterMessages;
import com.io7m.ironstrata.serialport.api.ISSerialPortType;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static com.io7m.ironstrata.printer.api.ISPrinterGCodeCommandStyle.COMMAND_WITHOUT_LINE;
import static java.util.regex.Pattern.CASE_INSENSITIVE;

public final class ISSerialPrinterEngine implements Runnable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(ISSerialPrinterEngine.class);

  private static final Pattern OK_ANY_PATTERN =
    Pattern.compile(
      "^OK.*",
      CASE_INSENSITIVE
    );
  private static final Pattern RESEND_PATTERN =
    Pattern.compile(
      "^(RS|RESEND):\\s*([0-9]+)",
      CASE_INSENSITIVE
    );
  private static final ISPrinterCommandGCode TEMPERATURE_COMMAND =
    ISGCode.compile(
      -1,
      "M105",
      COMMAND_WITHOUT_LINE
    );
  private final AtomicBoolean online;
  private final AtomicBoolean stopped;
  private final BlockingQueue<ISPrinterCommandGCode> commandQueue;
  private final Clock clock;
  private final ISGCodeCommandQueue queue;
  private final ISSerialPortType port;
  private final ISSerialPrinterMessages messages;
  private final ISTemperatureParser temperatureParser;
  private final ISTimeOut offlineTimeout;
  private final ISTimeOut onlineTimeout;
  private final PublishSubject<ISPrinterEventType> events;
  private OffsetDateTime timeLastReceived;

  public ISSerialPrinterEngine(
    final ISSerialPrinterMessages inMessages,
    final ISSerialPortType inPort,
    final Clock inClock,
    final BlockingQueue<ISPrinterCommandGCode> inQueue)
  {
    this.messages =
      Objects.requireNonNull(inMessages, "inMessages");
    this.port =
      Objects.requireNonNull(inPort, "port");
    this.clock =
      Objects.requireNonNull(inClock, "clock");
    this.commandQueue =
      Objects.requireNonNull(inQueue, "queue");

    this.offlineTimeout =
      new ISTimeOut(this.clock, Duration.ofSeconds(10L));
    this.onlineTimeout =
      new ISTimeOut(this.clock, Duration.ofSeconds(10L));

    this.events =
      PublishSubject.create();
    this.online =
      new AtomicBoolean(false);
    this.stopped =
      new AtomicBoolean(false);
    this.queue =
      new ISGCodeCommandQueue(this.clock, this.events, this.commandQueue);
    this.temperatureParser =
      new ISTemperatureParser();
  }

  private static boolean isOKResponse(
    final String line)
  {
    return OK_ANY_PATTERN.matcher(line).matches();
  }

  private static boolean isErrorResponse(
    final String line)
  {
    return ISGCodeErrors.isError(line);
  }

  private static boolean isResendResponse(
    final String line)
  {
    return RESEND_PATTERN.matcher(line).matches();
  }

  private static boolean isINT4(
    final String line)
  {
    return Objects.equals(line, "INT4");
  }

  @Override
  public void run()
  {
    LOG.debug("starting");

    try {
      while (this.isStillRunning()) {
        this.runOffline();

        if (!this.isStillRunning()) {
          break;
        }

        try {
          this.runOnline();
        } catch (final PrinterWentOffline ex) {
          // Continue...
        }
      }
    } catch (final Throwable e) {
      LOG.error("fatal error: ", e);
      this.events.onNext(ISPrinterEventFatalError.of(this.now(), e));
      this.wentOffline();
    } finally {
      LOG.debug("finished");
    }
  }

  private boolean isStillRunning()
  {
    return !this.stopped.get();
  }

  private void runOffline()
    throws IOException
  {
    while (this.isStillRunning()) {
      if (this.offlineTimeout.isTimedOut()) {
        this.port.writeLine(TEMPERATURE_COMMAND.text());
      }

      final var line = this.port.readLine();
      if (line == null) {
        continue;
      }

      if (isINT4(line)) {
        this.onReceivedINT4();
        continue;
      }

      this.timeLastReceived = this.now();
      this.wentOnline();
      this.enqueueFirmwareVersionCommand();
      this.enqueueTemperatureCommand();
      break;
    }
  }

  private PrinterWentOffline onReceivedINT4()
  {
    LOG.debug("received INT4");
    return this.wentOffline();
  }

  private void runOnline()
    throws PrinterWentOffline, IOException
  {
    while (this.isStillRunning()) {
      ISPrinterCommandGCode command = null;
      try {
        command = this.commandQueue.poll(10L, TimeUnit.MILLISECONDS);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      if (command != null) {
        this.onlineTimeout.reset();
        this.runOnlineCommand(command);
        continue;
      }

      if (this.onlineTimeout.isTimedOut()) {
        LOG.debug(
          "no commands sent in the last {}, sending temperature command",
          this.onlineTimeout.duration()
        );
        this.enqueueTemperatureCommand();
      }
    }
  }

  private ISPrinterCommandGCode enqueueTemperatureCommand()
  {
    return this.queue.enqueueCompile("M105");
  }

  private ISPrinterCommandGCode enqueueFirmwareVersionCommand()
  {
    return this.queue.enqueueCompile("M115");
  }

  private void runOnlineCommand(
    final ISPrinterCommandGCode command)
    throws PrinterWentOffline, IOException
  {
    LOG.debug("command executing: {}", command.show());

    for (int sendAttempt = 0; sendAttempt < 30; ++sendAttempt) {
      if (!this.isStillRunning()) {
        return;
      }

      LOG.debug("command send attempt {}", Integer.valueOf(sendAttempt));
      this.port.writeLine(command.text());
      this.onlineTimeout.reset();

      boolean needResend = false;
      boolean failed = false;

      while (this.isStillRunning()) {
        final var line = this.port.readLine();
        if (line == null) {
          if (this.onlineTimeout.isTimedOut()) {
            LOG.debug(
              "nothing received in last {}; printer must be offline",
              this.onlineTimeout.duration()
            );
            throw this.wentOffline();
          }
          continue;
        }

        if (isINT4(line)) {
          throw this.onReceivedINT4();
        }

        this.onlineTimeout.reset();

        if (isResendResponse(line)) {
          LOG.debug("command {} must be resent", command.show());
          this.queue.incrementResends();
          needResend = true;
          continue;
        }

        if (isErrorResponse(line)) {
          failed = true;
          this.handleErrorResponse(command, line);
          continue;
        }

        if (isOKResponse(line)) {
          this.temperatureParser.parseOK(line).ifPresent(this::onTemperature);
          if (!needResend) {
            LOG.debug("command {} done", command.show());
            if (!failed) {
              this.events.onNext(
                ISPrinterEventCommandSucceeded.of(this.now(), command)
              );
            }
            return;
          }
          break;
        }
      }
    }

    LOG.error("command {} could not be re-sent", command.show());
    throw new IOException("Command resubmission failure");
  }

  private OffsetDateTime now()
  {
    return OffsetDateTime.now(this.clock);
  }

  private void handleErrorResponse(
    final ISPrinterCommandType command,
    final String line)
  {
    this.queue.incrementErrors();
    this.events.onNext(
      ISPrinterEventCommandFailed.of(this.now(), command, line)
    );
  }

  private void onTemperature(
    final ISPrinterTemperatures temperatures)
  {
    this.events.onNext(
      ISPrinterEventTemperaturesChanged.of(this.now(), temperatures)
    );
  }

  private void wentOnline()
  {
    LOG.debug("printer came online");
    this.online.set(true);
    this.events.onNext(
      ISPrinterEventOnlineStateChanged.of(this.timeLastReceived, true)
    );
  }

  private PrinterWentOffline wentOffline()
  {
    LOG.debug("printer went offline");
    this.queue.reset();
    this.online.set(false);
    this.events.onNext(
      ISPrinterEventOnlineStateChanged.of(this.now(), false)
    );
    return new PrinterWentOffline();
  }

  public void close()
  {
    if (this.stopped.compareAndSet(false, true)) {
      this.events.onComplete();
    }
  }

  public Observable<ISPrinterEventType> events()
  {
    return this.events;
  }

  public boolean isOnline()
  {
    return this.online.get();
  }

  public <T extends ISPrinterCommandQueueType> T commandQueue(
    final Class<T> clazz)
    throws ISPrinterException
  {
    final var currentClass = this.queue.getClass();
    if (clazz.isAssignableFrom(currentClass)) {
      return clazz.cast(this.queue);
    }

    throw new ISPrinterExceptionUnsupported(
      this.messages.format(
        "errorUnsupportedCommandQueue",
        currentClass.getSimpleName(),
        clazz.getSimpleName()
      )
    );
  }

  private static final class PrinterWentOffline extends Exception
  {
    PrinterWentOffline()
    {

    }
  }
}

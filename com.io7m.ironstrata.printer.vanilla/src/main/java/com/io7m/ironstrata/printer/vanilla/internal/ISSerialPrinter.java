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
import com.io7m.ironstrata.printer.api.ISPrinterEventType;
import com.io7m.ironstrata.printer.api.ISPrinterException;
import com.io7m.ironstrata.printer.api.ISSerialPrinterConfiguration;
import com.io7m.ironstrata.printer.api.ISSerialPrinterType;
import com.io7m.ironstrata.printer.vanilla.ISSerialPrinterMessages;
import com.io7m.ironstrata.serialport.api.ISSerialPortType;
import io.reactivex.rxjava3.core.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ISSerialPrinter implements ISSerialPrinterType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(ISSerialPrinter.class);

  private final AtomicBoolean closed;
  private final ISSerialPortType port;
  private final ExecutorService executor;
  private final ISSerialPrinterEngine engine;

  private ISSerialPrinter(
    final ISSerialPrinterMessages inMessages,
    final ISSerialPortType inPort,
    final Clock inClock,
    final ExecutorService inExecutor,
    final ISSerialPrinterEngine inEngine)
  {
    this.port =
      Objects.requireNonNull(inPort, "port");
    this.executor =
      Objects.requireNonNull(inExecutor, "executor");
    this.engine =
      Objects.requireNonNull(inEngine, "engine");
    this.closed =
      new AtomicBoolean(false);
  }

  public static ISSerialPrinterType create(
    final ISSerialPrinterMessages messages,
    final ISSerialPrinterConfiguration configuration,
    final Clock clock,
    final ISSerialPortType inPort)
  {
    Objects.requireNonNull(messages, "messages");
    Objects.requireNonNull(configuration, "configuration");
    Objects.requireNonNull(clock, "clock");
    Objects.requireNonNull(inPort, "inPort");

    final ExecutorService executor =
      Executors.newSingleThreadExecutor(runnable -> {
        final var thread = new Thread(runnable);
        thread.setName(String.format(
          "com.io7m.ironstrata.printer.%d",
          Long.valueOf(thread.getId()))
        );
        return thread;
      });

    final var queue =
      new ArrayBlockingQueue<ISPrinterCommandGCode>(100);

    final var engine =
      new ISSerialPrinterEngine(messages, inPort, clock, queue);
    executor.execute(engine);
    return new ISSerialPrinter(messages, inPort, clock, executor, engine);
  }

  @Override
  public Observable<ISPrinterEventType> events()
  {
    return this.engine.events();
  }

  @Override
  public boolean isOnline()
  {
    return this.engine.isOnline();
  }

  @Override
  public <T extends ISPrinterCommandQueueType> T commandQueue(
    final Class<T> clazz)
    throws ISPrinterException
  {
    Objects.requireNonNull(clazz, "clazz");
    return this.engine.commandQueue(clazz);
  }

  @Override
  public void close()
    throws ISPrinterException
  {
    if (this.closed.compareAndSet(false, true)) {
      this.engine.close();

      try {
        this.port.close();
      } catch (final IOException e) {
        LOG.error("close: ", e);
      }

      this.executor.shutdown();

      try {
        this.executor.awaitTermination(30L, TimeUnit.SECONDS);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public String toString()
  {
    return String.format(
      "[ISSerialPrinter 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}

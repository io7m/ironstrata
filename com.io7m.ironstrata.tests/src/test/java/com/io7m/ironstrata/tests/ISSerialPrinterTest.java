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

package com.io7m.ironstrata.tests;

import com.io7m.ironstrata.printer.api.ISPrinterCommandQueueGCodeType;
import com.io7m.ironstrata.printer.api.ISPrinterCommandQueueType;
import com.io7m.ironstrata.printer.api.ISPrinterEventCommandFailed;
import com.io7m.ironstrata.printer.api.ISPrinterEventCommandSubmitted;
import com.io7m.ironstrata.printer.api.ISPrinterEventCommandSucceeded;
import com.io7m.ironstrata.printer.api.ISPrinterEventFatalError;
import com.io7m.ironstrata.printer.api.ISPrinterEventOnlineStateChanged;
import com.io7m.ironstrata.printer.api.ISPrinterEventTemperaturesChanged;
import com.io7m.ironstrata.printer.api.ISPrinterEventType;
import com.io7m.ironstrata.printer.api.ISPrinterExceptionUnsupported;
import com.io7m.ironstrata.printer.api.ISSerialPrinterConfiguration;
import com.io7m.ironstrata.printer.api.ISSerialPrinterType;
import com.io7m.ironstrata.printer.vanilla.ISSerialPrinterFactory;
import com.io7m.ironstrata.printer.vanilla.ISSerialPrinterMessages;
import com.io7m.ironstrata.serialport.api.ISSerialPortConfiguration;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class ISSerialPrinterTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(ISSerialPrinterTest.class);

  private ISFakeSerialPorts ports;
  private ISFakeClock clock;
  private ISSerialPrinterFactory printers;
  private ISSerialPrinterType printer;
  private ISFakeSerialPort port;
  private Disposable eventSub;
  private Observable<ISPrinterEventOnlineStateChanged> onlines;
  private Observable<ISPrinterEventCommandSucceeded> successes;
  private Observable<ISPrinterEventFatalError> fatals;
  private ArrayList<ISPrinterEventType> events;
  private Observable<ISPrinterEventCommandSubmitted> submissions;
  private Observable<ISPrinterEventCommandFailed> failures;

  private ISPrinterEventType logEvent(
    final ISPrinterEventType e)
  {
    LOG.debug("event: {}", e);
    this.events.add(e);
    return e;
  }

  @BeforeEach
  public void setup()
    throws Exception
  {
    this.port = new ISFakeSerialPort();
    this.ports = new ISFakeSerialPorts();
    this.ports.ports.add(this.port);
    this.clock = new ISFakeClock();

    this.printers =
      new ISSerialPrinterFactory(
        ISSerialPrinterMessages.create(),
        this.ports,
        this.clock
      );

    this.printer =
      this.printers.open(
        ISSerialPrinterConfiguration.builder()
          .setPort(
            ISSerialPortConfiguration.builder()
              .setDeviceName("/dev/null")
              .setBaudRate(100_000)
              .build()
          ).build()
      );

    this.events =
      new ArrayList<>();

    this.eventSub =
      this.printer.events()
        .subscribe(this::logEvent);

    this.onlines =
      this.printer.events()
        .ofType(ISPrinterEventOnlineStateChanged.class);

    this.submissions =
      this.printer.events()
        .ofType(ISPrinterEventCommandSubmitted.class);

    this.successes =
      this.printer.events()
        .ofType(ISPrinterEventCommandSucceeded.class);

    this.failures =
      this.printer.events()
        .ofType(ISPrinterEventCommandFailed.class);

    this.fatals =
      this.printer.events()
        .ofType(ISPrinterEventFatalError.class);
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
    this.printer.close();
    this.eventSub.dispose();
  }

  /**
   * Printers start out offline.
   *
   * @throws Exception On errors
   */

  @Test
  public void testOffline()
    throws Exception
  {
    Assertions.assertFalse(this.printer.isOnline());
  }

  /**
   * Printers stay offline when they receive INT4.
   *
   * @throws Exception On errors
   */

  @Test
  public void testOfflineINT4()
    throws Exception
  {
    this.port.addLine("INT4");
    Assertions.assertFalse(this.printer.isOnline());
  }

  /**
   * Printers move to the online state when they receive input.
   *
   * @throws Exception On errors
   */

  @Test
  public void testWentOnline0()
    throws Exception
  {
    this.port.addLine("start");
    this.waitForOnlineChange();
    Assertions.assertTrue(this.printer.isOnline());
  }

  /**
   * Printers move to the online state when they receive input.
   *
   * @throws Exception On errors
   */

  @Test
  public void testWentOnline1()
    throws Exception
  {
    this.port.addLine("ok");
    this.waitForOnlineChange();
    Assertions.assertTrue(this.printer.isOnline());
  }

  private ISPrinterEventOnlineStateChanged waitForOnlineChange()
  {
    return this.onlines.blockingFirst();
  }

  /**
   * Printers go offline when they receive INT4.
   *
   * @throws Exception On errors
   */

  @Test
  public void testWentOffline0()
    throws Exception
  {
    this.port.addLine("start");
    this.waitForOnlineChange();
    Assertions.assertTrue(this.printer.isOnline());

    this.port.addLine("ok");
    this.port.addLine("ok");
    this.port.addLine("INT4");

    this.successes.subscribe(e -> this.clock.tick(60L));
    this.waitForOnlineChange();
    Assertions.assertFalse(this.printer.isOnline());
  }

  /**
   * Printers go offline when they see I/O errors.
   *
   * @throws Exception On errors
   */

  @Test
  public void testWentOfflineCrashed0()
    throws Exception
  {
    this.port.addLine("start");
    this.waitForOnlineChange();
    Assertions.assertTrue(this.printer.isOnline());

    this.port.addLine("ok");
    this.port.addLine("ok");
    this.port.addLine("X");

    this.successes.subscribe(e -> this.clock.tick(60L));
    this.waitForOnlineChange();
    Assertions.assertFalse(this.printer.isOnline());

    final var fatalError =
      this.events.stream()
        .filter(e -> e instanceof ISPrinterEventFatalError)
        .map(ISPrinterEventFatalError.class::cast)
        .findFirst()
        .orElseThrow();

    assertEquals("Fatal read error", fatalError.exception().getMessage());
  }

  /**
   * Printers go offline when they see I/O errors.
   *
   * @throws Exception On errors
   */

  @Test
  public void testWentOfflineCrashed1()
    throws Exception
  {
    this.port.addLine("start");
    this.waitForOnlineChange();
    Assertions.assertTrue(this.printer.isOnline());

    this.port.addLine("ok");
    this.port.addLine("ok");

    final var commands =
      this.printer.commandQueue(ISPrinterCommandQueueGCodeType.class);

    commands.enqueueCompile("X");

    this.successes.subscribe(e -> this.clock.tick(60L));
    this.waitForOnlineChange();
    Assertions.assertFalse(this.printer.isOnline());

    final var fatalError =
      this.events.stream()
        .filter(e -> e instanceof ISPrinterEventFatalError)
        .map(ISPrinterEventFatalError.class::cast)
        .findFirst()
        .orElseThrow();

    assertEquals("Fatal write error", fatalError.exception().getMessage());
  }

  /**
   * Printers go offline when they fail to receive any input after submitting
   * a command.
   *
   * @throws Exception On errors
   */

  @Test
  public void testWentOfflineTimedOut()
    throws Exception
  {
    this.port.addLine("start");
    this.waitForOnlineChange();
    Assertions.assertTrue(this.printer.isOnline());

    this.port.addLine("ok");
    this.port.addLine("ok");

    this.port.reads().subscribe(line -> this.clock.tick(60L));
    this.port.writes().subscribe(line -> this.clock.tick(60L));

    final var commands =
      this.printer.commandQueue(ISPrinterCommandQueueGCodeType.class);

    commands.enqueueCompile("M1000");

    this.waitForOnlineChange();
    Assertions.assertFalse(this.printer.isOnline());

    assertEquals(3L, commands.statistics().commandSubmissions());
    assertEquals(0L, commands.statistics().commandResends());
    assertEquals(0L, commands.statistics().commandErrors());
  }

  /**
   * Commands are re-sent if the printer asks for it.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCommandResend()
    throws Exception
  {
    this.port.addLine("start");
    this.waitForOnlineChange();
    Assertions.assertTrue(this.printer.isOnline());

    this.port.addLine("ok");
    this.port.addLine("ok");

    this.successes.blockingFirst();
    this.successes.blockingFirst();

    final var commands =
      this.printer.commandQueue(ISPrinterCommandQueueGCodeType.class);

    commands.enqueueCompile("M1000");

    this.port.addLine("Error:What?");
    this.port.addLine("fatal:What?");
    this.port.addLine("!!:What?");
    this.port.addLine("Invalid M Code: M1000");
    this.port.addLine("Invalid G Code: M1000");
    this.port.addLine("Invalid D Code: M1000");
    this.port.addLine("Unknown M Code: M1000");
    this.port.addLine("Unknown G Code: M1000");
    this.port.addLine("Unknown D Code: M1000");
    this.port.addLine("Resend: 1");
    this.port.addLine("ok");
    this.port.addLine("ok");

    this.failures.blockingFirst();
    this.successes.blockingFirst();

    assertEquals(3L, commands.statistics().commandSubmissions());
    assertEquals(1L, commands.statistics().commandResends());
    assertEquals(9L, commands.statistics().commandErrors());
  }

  /**
   * Commands are re-sent if the printer asks for it.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCommandBadChecksumResend()
    throws Exception
  {
    this.port.addLine("start");
    this.waitForOnlineChange();
    Assertions.assertTrue(this.printer.isOnline());

    this.port.addLine("ok");
    this.port.addLine("ok");

    this.successes.blockingFirst();
    this.successes.blockingFirst();

    final var commands =
      this.printer.commandQueue(ISPrinterCommandQueueGCodeType.class);

    commands.enqueueCompile("M1000");

    this.port.addLine("Error:checksum mismatch, Last Line:0");
    this.port.addLine("Resend: 1");
    this.port.addLine("ok");
    this.port.addLine("ok");

    this.failures.blockingFirst();
    this.successes.blockingFirst();

    assertEquals(3L, commands.statistics().commandSubmissions());
    assertEquals(1L, commands.statistics().commandResends());
    assertEquals(1L, commands.statistics().commandErrors());
  }

  /**
   * Commands are re-sent if the printer asks for it.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCommandNoChecksumResend()
    throws Exception
  {
    this.port.addLine("start");
    this.waitForOnlineChange();
    Assertions.assertTrue(this.printer.isOnline());

    this.port.addLine("ok");
    this.port.addLine("ok");

    this.successes.blockingFirst();
    this.successes.blockingFirst();

    final var commands =
      this.printer.commandQueue(ISPrinterCommandQueueGCodeType.class);

    commands.enqueueCompile("M1000");

    this.port.addLine("Error:No line number with checksum, Last Line:0");
    this.port.addLine("Resend: 1");
    this.port.addLine("ok");
    this.port.addLine("ok");

    this.failures.blockingFirst();
    this.successes.blockingFirst();

    assertEquals(3L, commands.statistics().commandSubmissions());
    assertEquals(1L, commands.statistics().commandResends());
    assertEquals(1L, commands.statistics().commandErrors());
  }

  /**
   * Commands are re-sent if the printer asks for it.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCommandResendCrash()
    throws Exception
  {
    this.port.addLine("start");
    this.waitForOnlineChange();
    Assertions.assertTrue(this.printer.isOnline());

    this.port.addLine("ok");
    this.port.addLine("ok");

    this.successes.blockingFirst();
    this.successes.blockingFirst();

    final var commands =
      this.printer.commandQueue(ISPrinterCommandQueueGCodeType.class);

    commands.enqueueCompile("M1000");

    for (int index = 0; index < 40; ++index) {
      this.port.addLine("Error:No line number with checksum, Last Line:0");
      this.port.addLine("Resend: 1");
      this.port.addLine("ok");
    }

    this.fatals.blockingFirst();

    final var fatalError =
      this.events.stream()
        .filter(e -> e instanceof ISPrinterEventFatalError)
        .map(ISPrinterEventFatalError.class::cast)
        .findFirst()
        .orElseThrow();

    assertEquals(
      "Command resubmission failure",
      fatalError.exception().getMessage());

    assertEquals(3L, commands.statistics().commandSubmissions());
    assertEquals(30L, commands.statistics().commandResends());
    assertEquals(30L, commands.statistics().commandErrors());
  }

  /**
   * Temperatures are parsed.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCommandTemperatures()
    throws Exception
  {
    this.port.addLine("start");
    this.waitForOnlineChange();
    Assertions.assertTrue(this.printer.isOnline());

    this.port.addLine("ok T:25.0 A:30.0");
    this.port.addLine("ok T:31.0/120.0 A:36.0");

    this.successes.blockingFirst();
    this.successes.blockingFirst();

    final var temperatures =
      this.events.stream()
        .filter(e -> e instanceof ISPrinterEventTemperaturesChanged)
        .map(ISPrinterEventTemperaturesChanged.class::cast)
        .collect(Collectors.toList());

    final var t0 = temperatures.get(0);
    final var t0t = t0.temperatures();
    assertEquals(25.0, t0t.extruder().currentCelsius());
    assertEquals(30.0, t0t.ambient().get().currentCelsius());
    final var t1 = temperatures.get(1);
    final var t1t = t1.temperatures();
    assertEquals(31.0, t1t.extruder().currentCelsius());
    assertEquals(120.0, t1t.extruder().targetCelsius().getAsDouble());
    assertEquals(36.0, t1t.ambient().get().currentCelsius());
  }

  /**
   * Unsupported command queues are unsupported!
   *
   * @throws Exception On errors
   */

  @Test
  public void testUnsupportedCommandQueue()
    throws Exception
  {
    this.port.addLine("start");
    this.waitForOnlineChange();
    Assertions.assertTrue(this.printer.isOnline());

    assertThrows(ISPrinterExceptionUnsupported.class, () -> {
      this.printer.commandQueue(UnsupportedType.class);
    });
  }

  interface UnsupportedType extends ISPrinterCommandQueueType
  {

  }
}

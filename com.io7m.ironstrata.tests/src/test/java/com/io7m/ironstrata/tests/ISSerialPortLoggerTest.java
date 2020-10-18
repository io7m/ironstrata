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

import com.io7m.ironstrata.serialport.logging.ISSerialPortLogger;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ISSerialPortLoggerTest
{
  @Test
  public void testLogger()
    throws IOException
  {
    final var fakeClock = new ISFakeClock();
    fakeClock.timeNow = Instant.parse("2000-01-01T00:00:00Z");

    final var portBase = new ISFakeSerialPort();
    final var logReadW = new StringWriter();
    final var logRead = new BufferedWriter(logReadW);
    final var logWriteW = new StringWriter();
    final var logWrite = new BufferedWriter(logWriteW);

    try (var ignored = new ISSerialPortLogger(fakeClock, portBase, logRead, logWrite)) {
      portBase.addLine("Read 0");
      portBase.addLine("Read 1");
      portBase.addLine("Read 2");

      portBase.writeLine("Alpha");
      fakeClock.tick(10L);

      portBase.readLine();
      fakeClock.tick(10L);

      portBase.writeLine("Beta");
      fakeClock.tick(10L);

      portBase.readLine();
      fakeClock.tick(10L);

      portBase.writeLine("Delta");
      fakeClock.tick(10L);

      portBase.readLine();
      fakeClock.tick(10L);
    }

    final var logReadLines =
      List.of(logReadW.toString().split("\n"));
    final var logWriteLines =
      List.of(logWriteW.toString().split("\n"));

    assertEquals("2000-01-01T00:00:10.000000100Z Read 0", logReadLines.get(0));
    assertEquals("2000-01-01T00:00:30.000000300Z Read 1", logReadLines.get(1));
    assertEquals("2000-01-01T00:00:50.000000500Z Read 2", logReadLines.get(2));

    assertEquals("2000-01-01T00:00:00Z Alpha", logWriteLines.get(0));
    assertEquals("2000-01-01T00:00:20.000000200Z Beta", logWriteLines.get(1));
    assertEquals("2000-01-01T00:00:40.000000400Z Delta", logWriteLines.get(2));
  }
}

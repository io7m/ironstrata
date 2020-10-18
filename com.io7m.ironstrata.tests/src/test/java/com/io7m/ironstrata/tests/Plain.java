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

import com.io7m.ironstrata.serialport.api.ISSerialPortConfiguration;
import com.io7m.ironstrata.serialport.plain.ISerialPortsPlain;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static java.nio.charset.StandardCharsets.*;

public final class Plain
{
  private static final Logger LOG =
    LoggerFactory.getLogger(Plain.class);

  private Plain()
  {

  }

  public static void main(final String[] args)
    throws Exception
  {
    final var ports = new ISerialPortsPlain();
    final var configuration =
      ISSerialPortConfiguration.builder()
        .setDeviceName("/dev/ttyACM0")
        .setBaudRate(115_200)
        .build();

    try (var port = ports.open(configuration)) {
      while (true) {
        port.writeLine("M105");

        while (true) {
          final var line = port.readLine();
          if (line == null) {
            continue;
          }

          LOG.debug("read: {}", line);
          if (line.startsWith("ok")
            || "INT4".equals(line)
            || "start".equals(line)) {
            break;
          }

          throw new IOException("Corrupt data: " + dump(line));
        }
      }
    }
  }

  private static String dump(
    final String line)
  {
    return String.format("\"%s\" | %s", line, Hex.encodeHexString(line.getBytes(US_ASCII)));
  }
}

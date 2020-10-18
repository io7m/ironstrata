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

package com.io7m.ironstrata.printer.vanilla;

import com.io7m.ironstrata.printer.api.ISPrinterException;
import com.io7m.ironstrata.printer.api.ISPrinterExceptionIO;
import com.io7m.ironstrata.printer.api.ISSerialPrinterConfiguration;
import com.io7m.ironstrata.printer.api.ISSerialPrinterFactoryType;
import com.io7m.ironstrata.printer.api.ISSerialPrinterType;
import com.io7m.ironstrata.printer.vanilla.internal.ISSerialPrinter;
import com.io7m.ironstrata.serialport.api.ISSerialPortFactoryType;
import com.io7m.ironstrata.serialport.api.ISSerialPortType;

import java.io.IOException;
import java.time.Clock;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * The basic serial port printer factory.
 */

public final class ISSerialPrinterFactory
  implements ISSerialPrinterFactoryType
{
  private final ISSerialPrinterMessages messages;
  private final ISSerialPortFactoryType serialPorts;
  private final Clock clock;

  /**
   * Construct a factory, loading dependencies from {@link ServiceLoader}.
   */

  public ISSerialPrinterFactory()
  {
    this(
      ISSerialPrinterMessages.create(),
      ServiceLoader.load(ISSerialPortFactoryType.class)
        .findFirst()
        .orElseThrow(ISSerialPrinterFactory::serviceNotFound),
      Clock.systemUTC()
    );
  }

  /**
   * Construct a factory.
   *
   * @param inClock       The clock
   * @param inMessages    Message resources
   * @param inSerialPorts A serial port factory
   */

  public ISSerialPrinterFactory(
    final ISSerialPrinterMessages inMessages,
    final ISSerialPortFactoryType inSerialPorts,
    final Clock inClock)
  {
    this.messages =
      Objects.requireNonNull(inMessages, "messages");
    this.serialPorts =
      Objects.requireNonNull(inSerialPorts, "inSerialPorts");
    this.clock =
      Objects.requireNonNull(inClock, "clock");
  }

  private static ServiceConfigurationError serviceNotFound()
  {
    return new ServiceConfigurationError(String.format(
      "No available services of type %s",
      ISSerialPortFactoryType.class));
  }

  @Override
  public ISSerialPrinterType open(
    final ISSerialPrinterConfiguration configuration)
    throws ISPrinterException
  {
    Objects.requireNonNull(configuration, "selection");

    final ISSerialPortType port;
    try {
      port = this.serialPorts.open(configuration.port());
    } catch (final IOException e) {
      throw new ISPrinterExceptionIO(e);
    }

    return ISSerialPrinter.create(
      this.messages,
      configuration,
      this.clock,
      port
    );
  }

  @Override
  public String toString()
  {
    return String.format(
      "[ISSerialPrinterFactory 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}

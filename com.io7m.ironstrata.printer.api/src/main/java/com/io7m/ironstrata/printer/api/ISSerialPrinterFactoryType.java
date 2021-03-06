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

package com.io7m.ironstrata.printer.api;

import com.io7m.ironstrata.serialport.api.ISSerialPortType;

/**
 * A factory used to instantiate serial port printers.
 */

public interface ISSerialPrinterFactoryType
{
  /**
   * Open a printer using the given configuration.
   *
   * @param configuration A configuration
   *
   * @return An open printer
   *
   * @throws ISPrinterException On errors
   */

  ISSerialPrinterType open(
    ISSerialPrinterConfiguration configuration)
    throws ISPrinterException;

  /**
   * Open a printer using the given configuration and serial port.
   *
   * @param configuration A configuration
   * @param port          The serial port
   *
   * @return An open printer
   *
   * @throws ISPrinterException On errors
   */

  ISSerialPrinterType open(
    ISSerialPrinterConfiguration configuration,
    ISSerialPortType port)
    throws ISPrinterException;
}

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

import com.io7m.ironstrata.strings.api.ISAbstractStrings;

import java.util.ResourceBundle;

/**
 * Serial port printer messages.
 */

public final class ISSerialPrinterMessages extends ISAbstractStrings
{
  private ISSerialPrinterMessages(
    final ResourceBundle inResources)
  {
    super(inResources);
  }

  public static ISSerialPrinterMessages create()
  {
    return new ISSerialPrinterMessages(
      ofXMLResource(
        ISSerialPrinterMessages.class,
        "/com/io7m/ironstrata/printer/vanilla/internal/Messages.xml")
    );
  }

  @Override
  public String toString()
  {
    return String.format(
      "[ISSerialPrinterMessages 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}

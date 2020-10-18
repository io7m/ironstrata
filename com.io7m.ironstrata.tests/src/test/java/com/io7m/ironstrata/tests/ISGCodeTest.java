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

import com.io7m.ironstrata.printer.vanilla.internal.ISGCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.io7m.ironstrata.printer.api.ISPrinterGCodeCommandStyle.COMMAND_WITH_LINE;
import static com.io7m.ironstrata.printer.api.ISPrinterGCodeCommandStyle.COMMAND_WITH_LINE_AND_CHECKSUM;

public final class ISGCodeTest
{
  @Test
  public void testChecksum()
  {
    Assertions.assertEquals(39, ISGCode.checksum("N1 M115"));
  }

  @Test
  public void testCompileLineAndChecksum()
  {
    final var command =
      ISGCode.compile(1, "M115", COMMAND_WITH_LINE_AND_CHECKSUM);

    Assertions.assertTrue(command.checksum());
    Assertions.assertEquals(1, command.lineNumber().getAsInt());
    Assertions.assertEquals("N1 M115*39", command.text());
  }

  @Test
  public void testCompileLine()
  {
    final var command =
      ISGCode.compile(1, "M115", COMMAND_WITH_LINE);

    Assertions.assertFalse(command.checksum());
    Assertions.assertEquals(1, command.lineNumber().getAsInt());
    Assertions.assertEquals("N1 M115", command.text());
  }
}

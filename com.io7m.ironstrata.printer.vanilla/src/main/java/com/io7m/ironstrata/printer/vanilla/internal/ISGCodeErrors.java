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

public final class ISGCodeErrors
{
  private ISGCodeErrors()
  {

  }

  public static boolean isError(
    final String line)
  {
    final var lineUpper = line.trim().toUpperCase();
    if (lineUpper.startsWith("ERROR")) {
      return true;
    }
    if (lineUpper.startsWith("FATAL")) {
      return true;
    }
    if (lineUpper.startsWith("!!")) {
      return true;
    }

    return isObnoxiousPrusaErrorCode(lineUpper);
  }

  /**
   * Prusa Merlin. Who needs standardized error reporting when you
   * can just respond with any nonsense? Clearly starting a line with
   * "Error:" is just too difficult.
   */

  private static boolean isObnoxiousPrusaErrorCode(
    final String lineUpper)
  {
    if (lineUpper.startsWith("INVALID M CODE")) {
      return true;
    }
    if (lineUpper.startsWith("UNKNOWN M CODE")) {
      return true;
    }
    if (lineUpper.startsWith("INVALID G CODE")) {
      return true;
    }
    if (lineUpper.startsWith("UNKNOWN G CODE")) {
      return true;
    }
    if (lineUpper.startsWith("INVALID D CODE")) {
      return true;
    }
    return lineUpper.startsWith("UNKNOWN D CODE");
  }
}

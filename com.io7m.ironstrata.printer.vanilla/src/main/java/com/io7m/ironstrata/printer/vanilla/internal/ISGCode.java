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
import com.io7m.ironstrata.printer.api.ISPrinterGCodeCommandStyle;
import com.io7m.junreachable.UnreachableCodeException;

import java.util.Objects;
import java.util.OptionalInt;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.US_ASCII;

public final class ISGCode
{
  private ISGCode()
  {

  }

  public static int checksum(
    final String text)
  {
    Objects.requireNonNull(text, "text");

    int checksum = 0;
    final var bytes = text.getBytes(US_ASCII);
    for (int index = 0; index < bytes.length; ++index) {
      checksum = (checksum ^ (int) bytes[index]) & 0xff;
    }
    return checksum;
  }

  public static ISPrinterCommandGCode compile(
    final int lineNumber,
    final String text,
    final ISPrinterGCodeCommandStyle style)
  {
    Objects.requireNonNull(text, "text");
    Objects.requireNonNull(style, "style");

    final var commandBuilder = new StringBuilder(text.length() + 16);

    switch (style) {
      case COMMAND_WITHOUT_LINE: {
        break;
      }
      case COMMAND_WITH_LINE:
      case COMMAND_WITH_LINE_AND_CHECKSUM: {
        commandBuilder.append('N');
        commandBuilder.append(lineNumber);
        commandBuilder.append(' ');
        break;
      }
    }

    commandBuilder.append(text);

    final var commandBase = commandBuilder.toString();
    switch (style) {
      case COMMAND_WITH_LINE:
      case COMMAND_WITHOUT_LINE: {
        break;
      }
      case COMMAND_WITH_LINE_AND_CHECKSUM: {
        final var commandChecksum = checksum(commandBase);
        commandBuilder.append('*');
        commandBuilder.append(commandChecksum);
        break;
      }
    }

    final var commandText = commandBuilder.toString();
    switch (style) {
      case COMMAND_WITHOUT_LINE:
        return ISPrinterCommandGCode.builder()
          .setId(UUID.randomUUID())
          .setLineNumber(OptionalInt.empty())
          .setChecksum(false)
          .setText(commandText)
          .build();
      case COMMAND_WITH_LINE:
        return ISPrinterCommandGCode.builder()
          .setId(UUID.randomUUID())
          .setLineNumber(lineNumber)
          .setChecksum(false)
          .setText(commandText)
          .build();
      case COMMAND_WITH_LINE_AND_CHECKSUM:
        return ISPrinterCommandGCode.builder()
          .setId(UUID.randomUUID())
          .setLineNumber(lineNumber)
          .setChecksum(true)
          .setText(commandText)
          .build();
    }

    throw new UnreachableCodeException();
  }
}

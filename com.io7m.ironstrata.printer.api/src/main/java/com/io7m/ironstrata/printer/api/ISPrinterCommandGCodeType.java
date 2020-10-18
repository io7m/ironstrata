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

import com.io7m.immutables.styles.ImmutablesStyleType;
import com.io7m.jaffirm.core.Preconditions;
import org.immutables.value.Value;

import java.util.OptionalInt;
import java.util.UUID;

/**
 * A compiled G-Code command.
 */

@Value.Immutable
@ImmutablesStyleType
public interface ISPrinterCommandGCodeType extends ISPrinterCommandType
{
  @Override
  @Value.Default
  default UUID id()
  {
    return UUID.randomUUID();
  }

  /**
   * @return The line number, if one was supplied
   */

  @Value.Parameter
  OptionalInt lineNumber();

  /**
   * @return The text of the compiled command
   */

  @Value.Parameter
  String text();

  /**
   * @return {@code true} if the command contains a checksum
   */

  @Value.Parameter
  boolean checksum();

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    final var text = this.text();
    if (this.lineNumber().isPresent()) {
      Preconditions.checkPrecondition(
        text,
        text.startsWith("N"),
        s -> "Command text must start with N"
      );
    }
    if (this.checksum()) {
      Preconditions.checkPrecondition(
        text,
        text.contains("*"),
        s -> "Command text must contain *"
      );
    }
  }

  /**
   * @return A humanly-readable description of the command
   */

  default String show()
  {
    return String.format(
      "[%s %d : \"%s\"]",
      this.id(),
      Integer.valueOf(this.lineNumber().orElse(-1)),
      this.text().trim()
    );
  }
}

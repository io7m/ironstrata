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

import com.io7m.ironstrata.printer.api.ISPrinterTemperatures;
import com.io7m.ironstrata.printer.api.ISTemperature;

import java.util.HashMap;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.regex.Pattern;

public final class ISTemperatureParser
{
  private static final Pattern OK_PATTERN =
    Pattern.compile("^ok\\s*");
  private static final Pattern SLASH_PATTERN =
    Pattern.compile("\\s+/");
  private static final Pattern TEMPERATURE_BASIC =
    Pattern.compile("([a-zA-Z0-9@]+):([0-9\\.]+)");
  private static final Pattern TEMPERATURE_AND_TARGET =
    Pattern.compile("([a-zA-Z0-9@]+):([0-9\\.]+)/([0-9\\.]+)");
  private static final Pattern WHITESPACE =
    Pattern.compile("\\s+");

  public ISTemperatureParser()
  {

  }

  private static Optional<ISPrinterTemperatures> parseCore(
    final String text)
  {
    if (text.isBlank()) {
      return Optional.empty();
    }

    final var temperatures = new HashMap<String, ISTemperature>(8);
    final var items = WHITESPACE.split(normalize(text));
    for (final var item : items) {
      {
        final var matcher = TEMPERATURE_AND_TARGET.matcher(item);
        if (matcher.matches()) {
          recordTemperature(
            temperatures,
            matcher.group(1),
            Double.parseDouble(matcher.group(2)),
            OptionalDouble.of(Double.parseDouble(matcher.group(3)))
          );
          continue;
        }
      }

      {
        final var matcher = TEMPERATURE_BASIC.matcher(item);
        if (matcher.matches()) {
          recordTemperature(
            temperatures,
            matcher.group(1),
            Double.parseDouble(matcher.group(2)),
            OptionalDouble.empty()
          );
          continue;
        }
      }
    }

    return Optional.of(
      ISPrinterTemperatures.builder()
        .setTemperatures(temperatures)
        .build()
    );
  }

  private static void recordTemperature(
    final HashMap<String, ISTemperature> temperatures,
    final String device,
    final double currentTemp,
    final OptionalDouble targetTemp)
  {
    temperatures.put(device, ISTemperature.of(device, currentTemp, targetTemp));
  }

  private static String normalize(
    final String text)
  {
    return SLASH_PATTERN.matcher(text)
      .replaceAll("/");
  }

  public Optional<ISPrinterTemperatures> parseOK(
    final String text)
  {
    return parseCore(
      OK_PATTERN.matcher(text)
        .replaceAll("")
        .trim()
    );
  }

  public Optional<ISPrinterTemperatures> parse(
    final String text)
  {
    return parseCore(text);
  }
}

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
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A set of temperature values reported by a printer.
 */

@ImmutablesStyleType
@Value.Immutable
public interface ISPrinterTemperaturesType
{
  /**
   * @return The map of temperature values
   */

  Map<String, ISTemperature> temperatures();

  /**
   * @return The primary extruder temperature
   */

  default ISTemperature extruder()
  {
    return this.temperatures().get("T");
  }

  /**
   * @return The extruder temperatures
   */

  default List<ISTemperature> extruders()
  {
    return this.temperatures()
      .entrySet()
      .stream()
      .filter(e -> e.getKey().startsWith("T"))
      .map(Map.Entry::getValue)
      .collect(Collectors.toList());
  }

  /**
   * @return The bed temperature
   */

  default Optional<ISTemperature> bed()
  {
    return Optional.ofNullable(this.temperatures().get("B"));
  }

  /**
   * @return The chamber temperature
   */

  default Optional<ISTemperature> chamber()
  {
    return Optional.ofNullable(this.temperatures().get("C"));
  }

  /**
   * @return The ambient temperature
   */

  default Optional<ISTemperature> ambient()
  {
    return Optional.ofNullable(this.temperatures().get("A"));
  }

  /**
   * @return The temperature of any PINDAv2 probe
   */

  default Optional<ISTemperature> pinda()
  {
    return Optional.ofNullable(this.temperatures().get("P"));
  }
}

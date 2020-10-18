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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public final class ISFakeClock extends Clock
{
  private static final Logger LOG =
    LoggerFactory.getLogger(ISFakeClock.class);

  public volatile ZoneId zone = ZoneId.of("UTC");
  public volatile Instant timeNow = Instant.ofEpochSecond(0L);

  public ISFakeClock()
  {

  }

  public void tick(
    final long seconds)
  {
    final Instant next =
      this.timeNow.plusSeconds(seconds).plusNanos(100L);

    LOG.debug("tick {} -> {}", this.timeNow, next);
    this.timeNow = next;
  }

  @Override
  public ZoneId getZone()
  {
    return this.zone;
  }

  @Override
  public Clock withZone(
    final ZoneId zone)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public Instant instant()
  {
    return this.timeNow;
  }
}

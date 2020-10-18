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
import com.io7m.ironstrata.printer.api.ISPrinterCommandQueueGCodeType;
import com.io7m.ironstrata.printer.api.ISPrinterCommandQueueStatistics;
import com.io7m.ironstrata.printer.api.ISPrinterEventCommandSubmitted;
import com.io7m.ironstrata.printer.api.ISPrinterEventType;
import com.io7m.ironstrata.printer.api.ISPrinterGCodeCommandStyle;
import io.reactivex.rxjava3.subjects.Subject;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Queue;

final class ISGCodeCommandQueue implements ISPrinterCommandQueueGCodeType
{
  private final Clock clock;
  private final Subject<ISPrinterEventType> events;
  private final Queue<ISPrinterCommandGCode> queue;
  private ISPrinterCommandQueueStatistics statistics;
  private int lineNumber;

  ISGCodeCommandQueue(
    final Clock inClock,
    final Subject<ISPrinterEventType> inEvents,
    final Queue<ISPrinterCommandGCode> inCommandQueue)
  {
    this.clock =
      Objects.requireNonNull(inClock, "clock");
    this.events =
      Objects.requireNonNull(inEvents, "inEvents");
    this.queue =
      Objects.requireNonNull(inCommandQueue, "commandQueue");

    this.statistics =
      ISPrinterCommandQueueStatistics.builder()
        .build();
  }

  @Override
  public ISPrinterCommandGCode enqueueCompile(
    final String text,
    final ISPrinterGCodeCommandStyle style)
  {
    final var command =
      ISGCode.compile(this.lineNumber, text, style);

    switch (style) {
      case COMMAND_WITHOUT_LINE: {
        break;
      }
      case COMMAND_WITH_LINE:
      case COMMAND_WITH_LINE_AND_CHECKSUM: {
        ++this.lineNumber;
        break;
      }
    }

    return this.enqueue(command);
  }

  @Override
  public ISPrinterCommandGCode enqueue(
    final ISPrinterCommandGCode command)
  {
    this.queue.add(command);
    final var x = this.statistics.commandSubmissions();
    this.statistics = this.statistics.withCommandSubmissions(x + 1L);

    this.events.onNext(
      ISPrinterEventCommandSubmitted.of(
        OffsetDateTime.now(this.clock),
        command
      )
    );
    return command;
  }

  @Override
  public ISPrinterCommandQueueStatistics statistics()
  {
    return this.statistics;
  }

  public void reset()
  {
    this.queue.clear();
    this.lineNumber = 0;
  }

  public void incrementErrors()
  {
    final var x = this.statistics.commandErrors();
    this.statistics = this.statistics.withCommandErrors(x + 1L);
  }

  public void incrementResends()
  {
    final var x = this.statistics.commandResends();
    this.statistics = this.statistics.withCommandResends(x + 1L);
  }
}

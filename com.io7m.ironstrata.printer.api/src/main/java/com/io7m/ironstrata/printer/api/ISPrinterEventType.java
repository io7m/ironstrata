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

import java.time.OffsetDateTime;

import static com.io7m.ironstrata.printer.api.ISPrinterEventType.Kind.COMMAND_FAILED;
import static com.io7m.ironstrata.printer.api.ISPrinterEventType.Kind.COMMAND_SUBMITTED;
import static com.io7m.ironstrata.printer.api.ISPrinterEventType.Kind.COMMAND_SUCCEEDED;
import static com.io7m.ironstrata.printer.api.ISPrinterEventType.Kind.FATAL_ERROR;
import static com.io7m.ironstrata.printer.api.ISPrinterEventType.Kind.ONLINE_STATE_CHANGED;
import static com.io7m.ironstrata.printer.api.ISPrinterEventType.Kind.TEMPERATURES_CHANGED;

/**
 * The type of events published by printers.
 */

public interface ISPrinterEventType
{
  /**
   * @return The event kind
   */

  Kind kind();

  /**
   * @return The event timestamp
   */

  OffsetDateTime time();

  /**
   * The kinds of events.
   */

  enum Kind
  {
    /**
     * @see com.io7m.ironstrata.printer.api.ISPrinterEventCommandFailed
     */
    COMMAND_FAILED,
    /**
     * @see com.io7m.ironstrata.printer.api.ISPrinterEventCommandSubmitted
     */
    COMMAND_SUBMITTED,
    /**
     * @see com.io7m.ironstrata.printer.api.ISPrinterEventCommandSucceeded
     */
    COMMAND_SUCCEEDED,
    /**
     * @see com.io7m.ironstrata.printer.api.ISPrinterEventFatalError
     */
    FATAL_ERROR,
    /**
     * @see com.io7m.ironstrata.printer.api.ISPrinterEventOnlineStateChanged
     */
    ONLINE_STATE_CHANGED,
    /**
     * @see com.io7m.ironstrata.printer.api.ISPrinterEventTemperaturesChanged
     */
    TEMPERATURES_CHANGED
  }

  /**
   * The "online" state of the printer changed. That is, the printer went
   * from being online to offline, or vice-versa.
   */

  @Value.Immutable
  @ImmutablesStyleType
  interface ISPrinterEventOnlineStateChangedType extends ISPrinterEventType
  {
    @Override
    default Kind kind()
    {
      return ONLINE_STATE_CHANGED;
    }

    @Override
    @Value.Parameter
    OffsetDateTime time();

    @Value.Parameter
    boolean isOnline();
  }

  /**
   * The temperature state of the printer changed.
   */

  @Value.Immutable
  @ImmutablesStyleType
  interface ISPrinterEventTemperaturesChangedType extends ISPrinterEventType
  {
    @Override
    default Kind kind()
    {
      return TEMPERATURES_CHANGED;
    }

    @Override
    @Value.Parameter
    OffsetDateTime time();

    @Value.Parameter
    ISPrinterTemperatures temperatures();
  }

  /**
   * A command was submitted to the queue. Execution of the command will
   * (presumably) begin at some point in the future.
   */

  @Value.Immutable
  @ImmutablesStyleType
  interface ISPrinterEventCommandSubmittedType extends ISPrinterEventType
  {
    @Override
    default Kind kind()
    {
      return COMMAND_SUBMITTED;
    }

    @Override
    @Value.Parameter
    OffsetDateTime time();

    @Value.Parameter
    ISPrinterCommandType command();
  }

  /**
   * A command was executed by the printer successfully. That is, the
   * command was submitted to the printer and the printer claimed to have
   * executed the command without publishing any observable errors.
   */

  @Value.Immutable
  @ImmutablesStyleType
  interface ISPrinterEventCommandSucceededType extends ISPrinterEventType
  {
    @Override
    default Kind kind()
    {
      return COMMAND_SUCCEEDED;
    }

    @Override
    @Value.Parameter
    OffsetDateTime time();

    @Value.Parameter
    ISPrinterCommandType command();
  }

  /**
   * A command was executed by the printer and it failed. That is, the
   * command was submitted to the printer and the printer published one
   * or more errors. It's possible that the command will be resubmitted
   * to the printer.
   */

  @Value.Immutable
  @ImmutablesStyleType
  interface ISPrinterEventCommandFailedType extends ISPrinterEventType
  {
    @Override
    default Kind kind()
    {
      return COMMAND_FAILED;
    }

    @Override
    @Value.Parameter
    OffsetDateTime time();

    @Value.Parameter
    ISPrinterCommandType command();

    @Value.Parameter
    String message();
  }

  /**
   * The printer encountered a fatal error and the connection to the printer
   * will almost certainly be broken.
   */

  @Value.Immutable
  @ImmutablesStyleType
  interface ISPrinterEventFatalErrorType extends ISPrinterEventType
  {
    @Override
    default Kind kind()
    {
      return FATAL_ERROR;
    }

    @Override
    @Value.Parameter
    OffsetDateTime time();

    @Value.Parameter
    Throwable exception();
  }
}

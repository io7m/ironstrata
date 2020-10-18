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

import io.reactivex.rxjava3.core.Observable;

/**
 * The interface exposed by printers.
 */

public interface ISPrinterType extends AutoCloseable
{
  /**
   * @return An observable stream of printer events
   */

  Observable<ISPrinterEventType> events();

  /**
   * @return {@code true} if the printer is currently online
   */

  boolean isOnline();

  /**
   * Get access to the command queue for the printer. Callers should pass in
   * the interface type that describes the commands supported by the printer.
   * Printers typically support G-Code, so callers should pass
   * {@link ISPrinterCommandQueueGCodeType} here.
   *
   * @param clazz The precise type of queue
   * @param <T>   The precise type of queue
   *
   * @return The command queue
   *
   * @throws ISPrinterExceptionUnsupported If the type of command queue isn't supported
   * @throws ISPrinterException            On other errors
   */

  <T extends ISPrinterCommandQueueType> T commandQueue(
    Class<T> clazz)
    throws ISPrinterException, ISPrinterExceptionUnsupported;

  @Override
  void close()
    throws ISPrinterException;
}

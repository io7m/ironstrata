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

import com.io7m.ironstrata.serialport.api.ISSerialPortType;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public final class ISFakeSerialPort implements ISSerialPortType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(ISFakeSerialPort.class);

  private final PublishSubject<String> writes;
  private final PublishSubject<String> reads;
  private Queue<String> lines = new LinkedList<>();

  public ISFakeSerialPort()
  {
    this.writes = PublishSubject.<String>create();
    this.reads = PublishSubject.<String>create();
  }

  public Observable<String> writes()
  {
    return this.writes;
  }

  public Observable<String> reads()
  {
    return this.reads;
  }

  public void addLine(
    final String line)
  {
    LOG.debug("addLine: {}", line);
    this.lines.add(line);
  }

  private static void pause()
  {
    try {
      Thread.sleep(30L);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public String readLine()
    throws IOException
  {
    pause();

    final String text = this.lines.poll();
    LOG.trace("<<< {}", text);

    if ("X".equals(text)) {
      throw new IOException("Fatal read error");
    }

    if (text != null) {
      this.reads.onNext(text);
    } else {
      this.reads.onNext("<<null>>");
    }

    return text;
  }

  @Override
  public void writeLine(
    final String text)
    throws IOException
  {
    pause();
    LOG.trace(">>> {}", text);

    if ("X".equals(text)) {
      throw new IOException("Fatal write error");
    }

    if (text != null) {
      this.writes.onNext(text);
    } else {
      this.writes.onNext("<<null>>");
    }
  }

  @Override
  public void close()
  {

  }
}

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

package com.io7m.ironstrata.serialport.plain.internal;

import com.io7m.ironstrata.serialport.api.ISSerialPortType;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.US_ASCII;

public final class ISSerialPort implements ISSerialPortType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(ISSerialPort.class);

  private final FileChannel channel;
  private final BufferedReader reader;
  private final PublishSubject<String> reads;
  private final PublishSubject<String> writes;

  public ISSerialPort(
    final FileChannel inChannel)
  {
    this.channel = Objects.requireNonNull(inChannel, "channel");
    this.reader = new BufferedReader(Channels.newReader(inChannel, US_ASCII));
    this.reads = PublishSubject.create();
    this.writes = PublishSubject.create();
  }

  @Override
  public Observable<String> reads()
  {
    return this.reads;
  }

  @Override
  public Observable<String> writes()
  {
    return this.writes;
  }

  @Override
  public String readLine()
    throws IOException
  {
    pause();

    final var line = this.reader.readLine();
    if ("\0".equals(line)) {
      return null;
    }
    if (line == null) {
      return null;
    }

    final var trimmed = line.stripTrailing();
    LOG.trace("<<< {}", trimmed);
    this.reads.onNext(trimmed);
    return line;
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
  public void writeLine(
    final String text)
    throws IOException
  {
    pause();

    final var trimmed = text.stripTrailing();
    LOG.trace(">>> {}", trimmed);
    final var data = (trimmed + '\n').getBytes(US_ASCII);
    final var buffer = ByteBuffer.wrap(data);
    this.channel.write(buffer);
    this.writes.onNext(trimmed);
  }

  @Override
  public void close()
    throws IOException
  {
    this.reads.onComplete();
    this.writes.onComplete();
    this.channel.close();
  }
}

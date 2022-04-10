/*
 * Copyright © 2016 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

package com.io7m.jpuddle.core;

/**
 * An exception raised by attempting to create a new object in a pool when that
 * object would exceed the pool's hard size limit.
 */

public final class JPPoolHardLimitExceededException extends JPPoolException
{
  private final long limit;
  private final long size;

  private JPPoolHardLimitExceededException(
    final String message,
    final long in_limit,
    final long in_size)
  {
    super(message);
    this.limit = in_limit;
    this.size = in_size;
  }

  /**
   * Construct a new exception.
   *
   * @param limit The hard limit
   * @param size  The size that exceeded the limit
   *
   * @return A new exception
   */

  public static JPPoolHardLimitExceededException newException(
    final long limit,
    final long size)
  {
    final String separator = System.lineSeparator();
    final StringBuilder sb = new StringBuilder(128);
    sb.append("Hard size limit exceeded.");
    sb.append(separator);
    sb.append("Hard limit: ");
    sb.append(Long.toUnsignedString(limit));
    sb.append(separator);
    sb.append("Size:       ");
    sb.append(Long.toUnsignedString(size));
    sb.append(separator);
    return new JPPoolHardLimitExceededException(sb.toString(), limit, size);
  }

  /**
   * @return The pool's hard limit
   */

  public long getLimit()
  {
    return this.limit;
  }

  /**
   * @return The size that exceeded the limit
   */

  public long getSize()
  {
    return this.size;
  }
}

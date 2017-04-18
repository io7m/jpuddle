/*
 * Copyright © 2016 <code@io7m.com> http://io7m.com
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

import com.io7m.junreachable.UnimplementedCodeException;

import java.math.BigInteger;

final class BigUnsigned
{
  private static final BigInteger UNSIGNED_LONG_MAX =
    new BigInteger("ffffffffffffffff", 16);

  private BigUnsigned()
  {
    throw new UnimplementedCodeException();
  }

  static long checkedAddLong(
    final long x,
    final long y)
  {
    final BigInteger bx = new BigInteger(Long.toUnsignedString(x));
    final BigInteger by = new BigInteger(Long.toUnsignedString(y));
    final BigInteger r = bx.add(by);

    if (r.compareTo(UNSIGNED_LONG_MAX) >= 0) {
      throw new ArithmeticException("Integer overflow: " + r);
    }

    return r.longValue();
  }

  static long checkedSubtractLong(
    final long x,
    final long y)
  {
    final BigInteger bx = BigInteger.valueOf(x);
    final BigInteger by = BigInteger.valueOf(y);
    final BigInteger r = bx.subtract(by);

    if (r.compareTo(UNSIGNED_LONG_MAX) >= 0) {
      throw new ArithmeticException("Integer overflow: " + r);
    }

    if (r.compareTo(BigInteger.ZERO) < 0) {
      throw new ArithmeticException("Integer underflow: " + r);
    }

    return r.longValue();
  }
}

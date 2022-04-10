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
 * The type of usable synchronous pools that yield values of type {@code T} for
 * keys of type {@code K}, using context values of type {@code C} to create new
 * values if necessary.
 *
 * @param <K> The type of keys
 * @param <T> The type of pooled values
 * @param <U> The type of user-visible pooled values
 * @param <C> The type of context values
 */

public interface JPPoolSynchronousUsableType<K, T extends U, U, C>
{
  /**
   * @return {@code true} iff the pool has been deleted
   */

  boolean isDeleted();

  /**
   * Trim free objects within the pool to reduce the pool size.
   *
   * @param context A context value
   *
   * @throws JPPoolException On errors
   */

  void trim(C context)
    throws JPPoolException;

  /**
   * Retrieve an object from the pool, creating it if necessary.
   *
   * @param context A context value
   * @param key     The key that will be used to construct or retrieve the
   *                object
   *
   * @return A new (or reused) object
   *
   * @throws JPPoolObjectCreationException On exceptions raised during creation
   *                                       of objects
   * @throws JPPoolException               On errors
   */

  U get(
    C context,
    K key)
    throws JPPoolException, JPPoolObjectCreationException;

  /**
   * Return an object to the pool for re-use by later calls to {@link
   * #get(Object, Object)}.
   *
   * @param context A context value
   * @param value   A value
   *
   * @throws JPPoolException             On errors
   * @throws JPPoolObjectReturnException If the given value is not in the pool,
   *                                     or has already been returned
   */

  void returnValue(
    C context,
    U value)
    throws JPPoolException, JPPoolObjectReturnException;

  /**
   * @return The current number of objects, active or free, within the pool
   */

  long size();
}

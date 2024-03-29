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
 * values if necessary. Only values of type {@code U} are exposed to the user:
 * This is to allow the pool to, for example, allocate and manipulate mutable
 * values internally, but only expose read-only interfaces to the user
 * requesting objects from the pool.
 *
 * @param <K> The type of keys
 * @param <T> The type of pooled values
 * @param <U> The type of externally visible pooled values
 * @param <C> The type of context values
 */

public interface JPPoolSynchronousType<K, T extends U, U, C> extends
  JPPoolSynchronousUsableType<K, T, U, C>
{
  /**
   * Delete all items in the pool and shut the pool down. The method will refuse
   * to delete the pool if any items are yet to be returned.
   *
   * @param context A context value
   *
   * @throws JPPoolException Iff any of the items in the pool have yet to be
   *                         returned
   */

  void deleteSafely(C context)
    throws JPPoolException;

  /**
   * Delete all items in the pool and shut the pool down. The method will delete
   * the pool even if there are items yet to be returned.
   *
   * @param context A context value
   *
   * @throws JPPoolException On errors
   */

  void deleteUnsafely(C context)
    throws JPPoolException;
}

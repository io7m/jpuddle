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

import java.util.Optional;

/**
 * The type of listeners used to manipulate objects within pools.
 *
 * @param <K> The type of keys used to fetch objects
 * @param <T> The type of objects within pools
 * @param <C> The type of contextual values used to create objects
 */

public interface JPPoolableListenerType<K, T, C>
{
  /**
   * Estimate the size of the object that will be created for {@code key}. If
   * the object size cannot be estimated, the function should return {@code 0}.
   * The purpose of estimation is to avoid having to allocate an object and then
   * finding out afterwards that the object is too large for the cache. If the
   * sizes are known ahead of time, allocations can be avoided.
   *
   * @param c   A context value
   * @param key A key
   *
   * @return The estimated size of the object
   */

  long onEstimateSize(
    C c,
    K key);

  /**
   * Called when an object is to be created.
   *
   * @param c   A context value
   * @param key A key
   *
   * @return A new object
   */

  T onCreate(
    C c,
    K key);

  /**
   * Called when the size of {@code value} is required.
   *
   * @param c     A context value
   * @param key   A key
   * @param value The object to be measured
   *
   * @return The size of {@code value}
   */

  long onGetSize(
    C c,
    K key,
    T value);

  /**
   * Called when an existing object is to be reused.
   *
   * @param c     A context value
   * @param key   A key
   * @param value The object to be reused
   */

  void onReuse(
    C c,
    K key,
    T value);

  /**
   * Called when an object is to be deleted. The method should delete any
   * resourced associated with {@code value}.
   *
   * @param c     A context value
   * @param key   A key
   * @param value The object to be deleted
   */

  void onDelete(
    C c,
    K key,
    T value);

  /**
   * Called on errors.
   *
   * @param c     A context value
   * @param key   A key
   * @param value The value associated with the error, if any
   * @param e     The exception raised
   */

  void onError(
    C c,
    K key,
    Optional<T> value,
    Throwable e);
}

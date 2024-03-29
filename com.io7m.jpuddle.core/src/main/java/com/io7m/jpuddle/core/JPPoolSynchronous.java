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

import com.io7m.jaffirm.core.Postconditions;
import com.io7m.jaffirm.core.Preconditions;
import com.io7m.junsigned.ranges.UnsignedRangeCheck;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;

/**
 * The default implementation of the {@link JPPoolSynchronousType} interface.
 *
 * @param <K> The type of keys
 * @param <T> The type of internal pooled values
 * @param <U> The type of user-visible pooled values
 * @param <C> The type of context values
 */

public final class JPPoolSynchronous<K, T extends U, U, C> implements
  JPPoolSynchronousType<K, T, U, C>
{
  private static final Logger LOG;
  private static final String SEPARATOR = System.lineSeparator();

  static {
    LOG = LoggerFactory.getLogger(JPPoolSynchronous.class);
  }

  private final JPPoolableListenerType<K, T, C> listener;
  private final Map<K, SortedSet<TimedEntry<K, T>>> entries_free;
  private final ObjectRBTreeSet<TimedEntry<K, T>> entries_free_timed;
  private final Map<T, TimedEntry<K, T>> entries_used;
  private final long size_limit_soft;
  private final long size_limit_hard;
  private long size_now;
  private long time;
  private boolean deleted;

  private JPPoolSynchronous(
    final JPPoolableListenerType<K, T, C> in_listener,
    final long in_size_limit_soft,
    final long in_size_limit_hard)
  {
    this.listener =
      new CheckedListener<>(Objects.requireNonNull(in_listener, "Listener"));
    this.entries_free =
      new Object2ReferenceOpenHashMap<>(1024);
    this.entries_used =
      new Reference2ReferenceOpenHashMap<>(1024);
    this.entries_free_timed =
      new ObjectRBTreeSet<>();

    this.size_limit_soft =
      UnsignedRangeCheck.checkGreaterEqualLong(
        in_size_limit_soft,
        "Soft limit",
        0L,
        "Smallest soft limit");
    this.size_limit_hard =
      UnsignedRangeCheck.checkGreaterEqualLong(
        in_size_limit_hard,
        "Hard limit",
        this.size_limit_soft,
        "Smallest hard limit");

    this.size_now = 0L;
    this.time = 0L;
    this.deleted = false;
  }

  private static <K, T> T mapListTake(
    final Map<K, SortedSet<T>> m,
    final K key)
  {
    final SortedSet<T> xs = m.get(key);
    if (xs != null && !xs.isEmpty()) {
      final T first = xs.first();
      xs.remove(first);
      return first;
    }
    return null;
  }

  private static <K, T> void mapListPut(
    final Map<K, SortedSet<T>> m,
    final K key,
    final T value)
  {
    final SortedSet<T> xs =
      m.getOrDefault(key, new ObjectRBTreeSet<>());

    xs.add(value);
    m.put(key, xs);
  }

  /**
   * Construct a new pool. The size of the pool will never exceed {@code
   * hard_limit}, and free (unused) objects within the pool will be frequently
   * trimmed so that the size of the pool stays at (at most) {@code
   * soft_limit}.
   *
   * @param listener   The listener that will manipulate objects within the
   *                   pool
   * @param soft_limit The soft size limit
   * @param hard_limit The hard size limit
   * @param <K>        The type of keys
   * @param <T>        The type of values
   * @param <U>        The type of user-visible pooled values
   * @param <C>        The type of contextual values
   *
   * @return A new pool
   */

  public static <K, T extends U, U, C> JPPoolSynchronous<K, T, U, C> newPool(
    final JPPoolableListenerType<K, T, C> listener,
    final long soft_limit,
    final long hard_limit)
  {
    return new JPPoolSynchronous<>(listener, soft_limit, hard_limit);
  }

  @Override
  public boolean isDeleted()
  {
    return this.deleted;
  }

  @Override
  public void trim(final C context)
    throws JPPoolException
  {
    Objects.requireNonNull(context, "Context");

    /*
     * Remove the least recently fetched values first.
     */

    while (true) {
      if (Long.compareUnsigned(this.size_now, this.size_limit_soft) <= 0) {
        return;
      }

      if (this.entries_free_timed.isEmpty()) {
        return;
      }

      final TimedEntry<K, T> oldest = this.entries_free_timed.first();
      this.evict(context, oldest);
    }
  }

  private void checkNotDeleted()
  {
    if (this.deleted) {
      throw new JPPoolDeletedException("Pool has been deleted.");
    }
  }

  @Override
  public T get(
    final C context,
    final K key)
    throws JPPoolException
  {
    Objects.requireNonNull(context, "Context");
    Objects.requireNonNull(key, "Key");

    this.checkNotDeleted();

    /*
     * Trim the pool down to the soft limit, if possible.
     */

    this.trim(context);

    /*
     * Check if there is a free element that matches the current key.
     */

    if (!this.entries_free_timed.isEmpty()) {
      final TimedEntry<K, T> r =
        mapListTake(this.entries_free, key);

      if (r != null) {
        this.entries_free_timed.remove(r);
        ++this.time;
        r.time = this.time;

        this.entries_used.put(r.value, r);
        this.listener.onReuse(context, r.key, r.value);
        return r.value;
      }
    }

    /*
     * Check the estimated size against the hard limit.
     */

    this.checkEstimatedSizeAgainstLimit(context, key);

    /*
     * Create a new value.
     */

    final T r;
    final long size;

    try {
      r = this.listener.onCreate(context, key);
    } catch (final Throwable e) {
      throw new JPPoolObjectCreationException(e);
    }

    try {
      size = this.listener.onGetSize(context, key, r);
    } catch (final Throwable e) {
      this.listener.onDelete(context, key, r);
      throw new JPPoolObjectCreationException(e);
    }

    /*
     * Check the size of the created object against the hard limit. Fail
     * and delete it if the limit is exceeded.
     */

    final long new_size = this.checkNewSize(context, key, r, size);

    /*
     * Add a new entry for the object.
     */

    this.addNewEntry(key, r, size, new_size);
    return r;
  }

  private void addNewEntry(
    final K key,
    final T value,
    final long size,
    final long new_size)
  {
    ++this.time;
    this.size_now = new_size;

    final TimedEntry<K, T> te = new TimedEntry<>();
    te.key = key;
    te.size = size;
    te.time = this.time;
    te.value = value;
    this.entries_used.put(te.value, te);
  }

  /*
   * Check the size of the created object against the hard limit. Fail
   * and delete it if the limit is exceeded.
   */

  private long checkNewSize(
    final C context,
    final K key,
    final T r,
    final long size)
  {
    final long new_size;
    try {
      new_size = BigUnsigned.checkedAddLong(this.size_now, size);
    } catch (final ArithmeticException e) {
      this.listener.onDelete(context, key, r);
      throw new JPPoolInternalOverflowException(e);
    }

    if (Long.compareUnsigned(new_size, this.size_limit_hard) > 0) {
      this.listener.onDelete(context, key, r);
      throw JPPoolHardLimitExceededException.newException(
        this.size_limit_hard,
        new_size);
    }
    return new_size;
  }

  /*
   * Check the estimated size against the hard limit.
   */

  private void checkEstimatedSizeAgainstLimit(
    final C context,
    final K key)
  {
    final long e_size;

    try {
      e_size = this.listener.onEstimateSize(context, key);
    } catch (final Throwable e) {
      throw new JPPoolObjectCreationException(e);
    }

    final long estimated_new;

    try {
      estimated_new = BigUnsigned.checkedAddLong(this.size_now, e_size);
    } catch (final ArithmeticException e) {
      throw new JPPoolInternalOverflowException(e);
    }

    if (Long.compareUnsigned(estimated_new, this.size_limit_hard) > 0) {
      throw JPPoolHardLimitExceededException.newException(
        this.size_limit_hard,
        estimated_new);
    }
  }

  private void evict(
    final C context,
    final TimedEntry<K, T> e)
  {
    Preconditions.checkPrecondition(
      e.key,
      this.entries_free.containsKey(e.key),
      k -> "Key " + k + " must be free");

    Preconditions.checkPreconditionL(
      this.size_now,
      Long.compareUnsigned(this.size_now, 0L) > 0,
      x -> "Size " + x + " must be > 0");

    final SortedSet<TimedEntry<K, T>> free = this.entries_free.get(e.key);
    free.remove(e);
    this.entries_free_timed.remove(e);
    this.size_now = BigUnsigned.checkedSubtractLong(this.size_now, e.size);

    Postconditions.checkPostconditionL(
      this.size_now,
      Long.compareUnsigned(this.size_now, 0L) >= 0,
      x -> "Size " + x + " must be >= 0");

    this.listener.onDelete(context, e.key, e.value);
  }

  @Override
  public void returnValue(
    final C context,
    final U value)
    throws JPPoolException
  {
    Objects.requireNonNull(context, "Context");
    Objects.requireNonNull(value, "Value");

    this.checkNotDeleted();

    final TimedEntry<K, T> e = this.entries_used.get(value);
    if (e != null) {
      this.entries_used.remove(value);
      mapListPut(this.entries_free, e.key, e);
      this.entries_free_timed.add(e);
      this.trim(context);
      return;
    }

    final StringBuilder sb = new StringBuilder(128);
    sb.append("Returned value not active!");
    sb.append(SEPARATOR);
    sb.append("Value: ");
    sb.append(value);
    sb.append(SEPARATOR);
    throw new JPPoolObjectReturnException(sb.toString());
  }

  @Override
  public long size()
  {
    this.checkNotDeleted();
    return this.size_now;
  }

  @Override
  public void deleteSafely(final C context)
    throws JPPoolException
  {
    Objects.requireNonNull(context, "Context");

    this.checkNotDeleted();

    if (!this.entries_used.isEmpty()) {
      throw this.errorNotEmpty();
    }

    this.deleteActual(context);
  }

  @Override
  public void deleteUnsafely(final C context)
    throws JPPoolException
  {
    Objects.requireNonNull(context, "Context");

    this.checkNotDeleted();
    this.deleteActual(context);
  }

  private void deleteActual(
    final C context)
  {
    try {

      {
        final ObjectArrayList<TimedEntry<K, T>> xs =
          new ObjectArrayList<>();
        final Iterator<Map.Entry<T, TimedEntry<K, T>>> iter =
          this.entries_used.entrySet().iterator();

        while (iter.hasNext()) {
          final Map.Entry<T, TimedEntry<K, T>> e = iter.next();
          xs.add(e.getValue());
        }

        for (int index = 0; index < xs.size(); ++index) {
          final TimedEntry<K, T> e = xs.get(index);
          this.returnValue(context, e.value);
        }
      }

      {
        final ObjectArrayList<TimedEntry<K, T>> xs =
          new ObjectArrayList<>();
        final ObjectBidirectionalIterator<TimedEntry<K, T>> iter =
          this.entries_free_timed.iterator();

        while (iter.hasNext()) {
          xs.add(iter.next());
        }

        for (int index = 0; index < xs.size(); ++index) {
          final TimedEntry<K, T> e = xs.get(index);
          this.evict(context, e);
        }
      }

    } finally {
      this.deleted = true;
    }
  }

  private JPPoolObjectsNotReturnedException errorNotEmpty()
  {
    final StringBuilder sb = new StringBuilder(256);

    int count = 0;
    final Iterator<Map.Entry<T, TimedEntry<K, T>>> iter =
      this.entries_used.entrySet().iterator();

    sb.append("Attempted to delete a pool with items not yet returned.");
    sb.append(SEPARATOR);
    sb.append("The first 10 items:");
    sb.append(SEPARATOR);

    while (iter.hasNext()) {
      if (count == 10) {
        break;
      }
      ++count;

      final Map.Entry<T, TimedEntry<K, T>> e = iter.next();
      sb.append(e.getKey());
      sb.append(" -> ");
      sb.append(e.getValue().value);
      sb.append(SEPARATOR);
    }

    return new JPPoolObjectsNotReturnedException(sb.toString());
  }

  private static final class CheckedListener<K, T, C> implements
    JPPoolableListenerType<K, T, C>
  {
    private final JPPoolableListenerType<K, T, C> listener;

    CheckedListener(
      final JPPoolableListenerType<K, T, C> in_listener)
    {
      this.listener = Objects.requireNonNull(in_listener, "Listener");
    }

    @Override
    public long onEstimateSize(
      final C c,
      final K key)
    {
      try {
        return this.listener.onEstimateSize(c, key);
      } catch (final Throwable ex) {
        try {
          this.listener.onError(c, key, Optional.empty(), ex);
        } catch (final Throwable z) {
          LOG.error("suppressed exception: ", z);
        }
        throw ex;
      }
    }

    @Override
    public T onCreate(
      final C c,
      final K key)
    {
      try {
        return this.listener.onCreate(c, key);
      } catch (final Throwable ex) {
        try {
          this.listener.onError(c, key, Optional.empty(), ex);
        } catch (final Throwable z) {
          LOG.error("suppressed exception: ", z);
        }
        throw ex;
      }
    }

    @Override
    public long onGetSize(
      final C c,
      final K key,
      final T value)
    {
      try {
        return this.listener.onGetSize(c, key, value);
      } catch (final Throwable ex) {
        try {
          this.listener.onError(c, key, Optional.empty(), ex);
        } catch (final Throwable z) {
          LOG.error("suppressed exception: ", z);
        }
        throw ex;
      }
    }

    @Override
    public void onReuse(
      final C c,
      final K key,
      final T value)
    {
      try {
        this.listener.onReuse(c, key, value);
      } catch (final Throwable ex) {
        try {
          this.listener.onError(c, key, Optional.of(value), ex);
        } catch (final Throwable z) {
          LOG.error("suppressed exception: ", z);
        }
      }
    }

    @Override
    public void onDelete(
      final C c,
      final K key,
      final T value)
    {
      try {
        this.listener.onDelete(c, key, value);
      } catch (final Throwable ex) {
        try {
          this.listener.onError(c, key, Optional.of(value), ex);
        } catch (final Throwable z) {
          LOG.error("suppressed exception: ", z);
        }
      }
    }

    @Override
    public void onError(
      final C c,
      final K key,
      final Optional<T> value,
      final Throwable e)
    {
      try {
        this.listener.onError(c, key, value, e);
      } catch (final Throwable ex) {
        try {
          this.listener.onError(c, key, value, ex);
        } catch (final Throwable z) {
          LOG.error("suppressed exception: ", z);
        }
      }
    }
  }

  private static final class TimedEntry<K, T> implements
    Comparable<TimedEntry<K, T>>
  {
    private K key;
    private T value;
    private long time;
    private long size;

    TimedEntry()
    {

    }

    @Override
    public int compareTo(final TimedEntry<K, T> o)
    {
      return Long.compare(this.time, o.time);
    }
  }
}

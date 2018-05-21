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

package com.io7m.jpuddle.tests.core;

import com.io7m.jpuddle.core.JPPoolHardLimitExceededException;
import com.io7m.jpuddle.core.JPPoolInternalOverflowException;
import com.io7m.jpuddle.core.JPPoolObjectCreationException;
import com.io7m.jpuddle.core.JPPoolObjectReturnException;
import com.io7m.jpuddle.core.JPPoolObjectsNotReturnedException;
import com.io7m.jpuddle.core.JPPoolSynchronous;
import com.io7m.jpuddle.core.JPPoolableListenerType;
import com.io7m.jranges.RangeCheckException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

public abstract class JPPoolSynchronousContract
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(JPPoolSynchronousContract.class);
  }

  @Rule public ExpectedException expected = ExpectedException.none();

  protected abstract <K, T extends U, U, C>
  JPPoolSynchronous<K, T, U, C> newPool(
    JPPoolableListenerType<K, T, C> listener,
    long soft_limit,
    long hard_limit);

  @Test
  public final void testGetEmpty()
  {
    final PooledListener listener = new PooledListener();

    final JPPoolSynchronous<Integer, Pooled, Pooled, Integer> p =
      this.newPool(listener, 10L, 20L);

    final Pooled[] values = new Pooled[15];
    for (int index = 0; index < values.length; ++index) {
      values[index] = p.get(Integer.valueOf(2), Integer.valueOf(index));
      Assert.assertEquals(new Pooled(index * 2), values[index]);
    }

    Assert.assertEquals(15L, p.size());
  }

  @Test
  public final void testGetEmptyFull()
  {
    final PooledListener listener = new PooledListener();

    final JPPoolSynchronous<Integer, Pooled, Pooled, Integer> p =
      this.newPool(listener, 10L, 20L);

    final Pooled[] values = new Pooled[20];
    for (int index = 0; index < values.length; ++index) {
      values[index] = p.get(Integer.valueOf(2), Integer.valueOf(index));
      Assert.assertEquals(new Pooled(index * 2), values[index]);
    }

    Assert.assertEquals(20L, p.size());

    this.expected.expect(JPPoolHardLimitExceededException.class);
    p.get(Integer.valueOf(2), Integer.valueOf(0));
  }

  @Test
  public final void testGetReturn()
  {
    final PooledListener listener = new PooledListener();

    final JPPoolSynchronous<Integer, Pooled, Pooled, Integer> p =
      this.newPool(listener, 10L, 20L);

    final Integer context = Integer.valueOf(2);
    final Pooled[] values = new Pooled[20];
    for (int index = 0; index < values.length; ++index) {
      values[index] = p.get(context, Integer.valueOf(index));
      Assert.assertEquals(new Pooled(index * 2), values[index]);
    }

    Assert.assertEquals(0L, (long) listener.reuses);
    Assert.assertEquals(0L, (long) listener.deletes);
    Assert.assertEquals(20L, (long) listener.creates);
    Assert.assertEquals(20L, p.size());

    for (int index = 0; index < values.length; ++index) {
      p.returnValue(context, values[index]);
    }

    Assert.assertEquals(0L, (long) listener.reuses);
    Assert.assertEquals(10L, (long) listener.deletes);
    Assert.assertEquals(20L, (long) listener.creates);
    Assert.assertEquals(10L, p.size());

    for (int index = values.length - 1; index >= 0; --index) {
      values[index] = p.get(context, Integer.valueOf(index));
      Assert.assertEquals(new Pooled(index * 2), values[index]);
    }

    Assert.assertEquals(10L, (long) listener.reuses);
    Assert.assertEquals(10L, (long) listener.deletes);
    Assert.assertEquals(30L, (long) listener.creates);
    Assert.assertEquals(20L, p.size());
  }

  @Test
  public final void testGetReturnReuseErrorSuppressed()
  {
    final PooledListenerReuseErrorSuppressed listener =
      new PooledListenerReuseErrorSuppressed();

    final JPPoolSynchronous<Integer, Pooled, Pooled, Integer> p =
      this.newPool(listener, 1L, 2L);

    final Integer context = Integer.valueOf(2);
    final Pooled v0 = p.get(context, Integer.valueOf(0));

    Assert.assertEquals(0L, (long) listener.reuses);
    Assert.assertEquals(0L, (long) listener.deletes);
    Assert.assertEquals(1L, (long) listener.creates);
    Assert.assertEquals(1L, p.size());

    p.returnValue(context, v0);

    Assert.assertEquals(0L, (long) listener.reuses);
    Assert.assertEquals(0L, (long) listener.deletes);
    Assert.assertEquals(1L, (long) listener.creates);
    Assert.assertEquals(1L, p.size());

    final Pooled v1 = p.get(context, Integer.valueOf(0));
    Assert.assertSame(v0, v1);

    Assert.assertEquals(1L, (long) listener.reuses);
    Assert.assertEquals(0L, (long) listener.deletes);
    Assert.assertEquals(1L, (long) listener.creates);
    Assert.assertEquals(1L, p.size());

    Assert.assertEquals(
      IllegalArgumentException.class,
      listener.error.getClass());
  }

  @Test
  public final void testGetReturnReuseAll()
  {
    final PooledListener listener = new PooledListener();

    final JPPoolSynchronous<Integer, Pooled, Pooled, Integer> p =
      this.newPool(listener, 10L, 20L);

    final Integer context = Integer.valueOf(2);
    final Pooled[] values = new Pooled[10];
    for (int index = 0; index < values.length; ++index) {
      values[index] = p.get(context, Integer.valueOf(index));
      Assert.assertEquals(new Pooled(index * 2), values[index]);
    }

    Assert.assertEquals(0L, (long) listener.reuses);
    Assert.assertEquals(0L, (long) listener.deletes);
    Assert.assertEquals(10L, (long) listener.creates);
    Assert.assertEquals(10L, p.size());

    for (int index = 0; index < values.length; ++index) {
      p.returnValue(context, values[index]);
    }

    Assert.assertEquals(0L, (long) listener.reuses);
    Assert.assertEquals(0L, (long) listener.deletes);
    Assert.assertEquals(10L, (long) listener.creates);
    Assert.assertEquals(10L, p.size());

    for (int index = 0; index < values.length; ++index) {
      Assert.assertFalse(values[index].deleted);
    }

    for (int index = values.length - 1; index >= 0; --index) {
      values[index] = p.get(context, Integer.valueOf(index));
      Assert.assertEquals(new Pooled(index * 2), values[index]);
    }

    Assert.assertEquals(10L, (long) listener.reuses);
    Assert.assertEquals(0L, (long) listener.deletes);
    Assert.assertEquals(10L, (long) listener.creates);
    Assert.assertEquals(10L, p.size());
  }

  @Test
  public final void testGetReturnReuseNone()
  {
    final PooledListener listener = new PooledListener();

    final JPPoolSynchronous<Integer, Pooled, Pooled, Integer> p =
      this.newPool(listener, 0L, 20L);

    final Integer context = Integer.valueOf(2);
    final Pooled[] values = new Pooled[10];
    for (int index = 0; index < values.length; ++index) {
      values[index] = p.get(context, Integer.valueOf(index));
      Assert.assertEquals(new Pooled(index * 2), values[index]);
    }

    Assert.assertEquals(0L, (long) listener.reuses);
    Assert.assertEquals(0L, (long) listener.deletes);
    Assert.assertEquals(10L, (long) listener.creates);
    Assert.assertEquals(10L, p.size());

    for (int index = 0; index < values.length; ++index) {
      p.returnValue(context, values[index]);
    }

    Assert.assertEquals(0L, (long) listener.reuses);
    Assert.assertEquals(10L, (long) listener.deletes);
    Assert.assertEquals(10L, (long) listener.creates);
    Assert.assertEquals(0L, p.size());

    for (int index = 0; index < values.length; ++index) {
      Assert.assertTrue(values[index].deleted);
    }

    for (int index = values.length - 1; index >= 0; --index) {
      values[index] = p.get(context, Integer.valueOf(index));
      Assert.assertEquals(new Pooled(index * 2), values[index]);
    }

    Assert.assertEquals(0L, (long) listener.reuses);
    Assert.assertEquals(10L, (long) listener.deletes);
    Assert.assertEquals(20L, (long) listener.creates);
    Assert.assertEquals(10L, p.size());
  }

  @Test
  public final void testCreationFailureObject()
  {
    final IntCreationFailureObjectListener listener =
      new IntCreationFailureObjectListener();

    final JPPoolSynchronous<Integer, Pooled, Pooled, Integer> p =
      this.newPool(listener, 10L, 20L);

    final Integer context = Integer.valueOf(2);

    this.expected.expect(JPPoolObjectCreationException.class);
    p.get(context, Integer.valueOf(0));
  }

  @Test
  public final void testCreationFailureSize()
  {
    final IntCreationFailureSizeListener listener =
      new IntCreationFailureSizeListener();

    final JPPoolSynchronous<Integer, Pooled, Pooled, Integer> p =
      this.newPool(listener, 10L, 20L);

    final Integer context = Integer.valueOf(2);

    this.expected.expect(JPPoolObjectCreationException.class);
    p.get(context, Integer.valueOf(0));
  }

  @Test
  public final void testCreationFailureEstimateSize()
  {
    final IntCreationFailureEstimateSizeListener listener =
      new IntCreationFailureEstimateSizeListener();

    final JPPoolSynchronous<Integer, Pooled, Pooled, Integer> p =
      this.newPool(listener, 10L, 20L);

    final Integer context = Integer.valueOf(2);

    this.expected.expect(JPPoolObjectCreationException.class);
    p.get(context, Integer.valueOf(0));
  }

  @Test
  public final void testReturnTwice()
  {
    final PooledListener listener = new PooledListener();

    final JPPoolSynchronous<Integer, Pooled, Pooled, Integer> p =
      this.newPool(listener, 10L, 20L);

    final Integer context = Integer.valueOf(2);
    final Pooled r = p.get(context, Integer.valueOf(0));

    p.returnValue(context, r);
    this.expected.expect(JPPoolObjectReturnException.class);
    p.returnValue(context, r);
  }

  @Test
  public final void testDeleteUnsafely()
  {
    final PooledListener listener = new PooledListener();

    final JPPoolSynchronous<Integer, Pooled, Pooled, Integer> p =
      this.newPool(listener, 20L, 30L);

    final Integer context = Integer.valueOf(2);

    for (int index = 0; index < 20; ++index) {
      final Pooled r = p.get(context, Integer.valueOf(index));
    }

    p.deleteUnsafely(context);
    Assert.assertTrue(p.isDeleted());
    Assert.assertEquals(20L, (long) listener.deletes);
  }

  @Test
  public final void testDeleteSafely()
  {
    final PooledListener listener = new PooledListener();

    final JPPoolSynchronous<Integer, Pooled, Pooled, Integer> p =
      this.newPool(listener, 10L, 20L);

    final Integer context = Integer.valueOf(2);

    for (int index = 0; index < 10; ++index) {
      final Pooled r = p.get(context, Integer.valueOf(index));
      p.returnValue(context, r);
    }

    p.deleteSafely(context);
    Assert.assertTrue(p.isDeleted());
    Assert.assertEquals(10L, (long) listener.deletes);
  }

  @Test
  public final void testDeleteSafelyNotReturned()
  {
    final PooledListener listener = new PooledListener();

    final JPPoolSynchronous<Integer, Pooled, Pooled, Integer> p =
      this.newPool(listener, 10L, 20L);

    final Integer context = Integer.valueOf(2);
    final Pooled r = p.get(context, Integer.valueOf(0));

    this.expected.expect(JPPoolObjectsNotReturnedException.class);
    p.deleteSafely(context);
  }

  @Test
  public final void testGetHardLimitExceeded()
  {
    final PooledListener listener = new PooledListener();
    listener.estimated_size = 21L;

    final JPPoolSynchronous<Integer, Pooled, Pooled, Integer> p =
      this.newPool(listener, 10L, 20L);

    this.expected.expect(JPPoolHardLimitExceededException.class);
    p.get(Integer.valueOf(2), Integer.valueOf(0));
  }

  @Test
  public final void testGetHardLimitExceededBigEstimated()
  {
    final PooledListener listener = new PooledListener();
    listener.estimated_size = 0x8000000000000065L;
    listener.size = 1L;

    final JPPoolSynchronous<Integer, Pooled, Pooled, Integer> p =
      this.newPool(
        listener,
        0L,
        0x8000000000000064L);

    this.expected.expect(JPPoolHardLimitExceededException.class);
    p.get(Integer.valueOf(2), Integer.valueOf(0));
  }

  @Test
  public final void testGetHardLimitExceededBig()
  {
    final PooledListener listener = new PooledListener();
    listener.estimated_size = 1L;
    listener.size = 0x8000000000000065L;

    final JPPoolSynchronous<Integer, Pooled, Pooled, Integer> p =
      this.newPool(
        listener,
        0L,
        0x8000000000000064L);

    this.expected.expect(JPPoolHardLimitExceededException.class);
    p.get(Integer.valueOf(2), Integer.valueOf(0));
  }

  @Test
  public final void testGetHardLimitExceededBigOverflow()
  {
    final PooledListener listener = new PooledListener();
    listener.estimated_size = 1L;
    listener.size =
      0b11111111_11111111_11111111_11111111_11111111_11111111_11111111_11111111L;

    final JPPoolSynchronous<Integer, Pooled, Pooled, Integer> p =
      this.newPool(
        listener,
        0L,
        0b11111111_11111111_11111111_11111111_11111111_11111111_11111111_11111111L);

    this.expected.expect(JPPoolInternalOverflowException.class);
    p.get(Integer.valueOf(2), Integer.valueOf(0));
  }

  @Test
  public final void testGetHardLimitExceededBigEstimatedOverflow()
  {
    final PooledListener listener = new PooledListener();
    listener.estimated_size =
      0b11111111_11111111_11111111_11111111_11111111_11111111_11111111_11111111L;
    listener.size =
      1L;

    final JPPoolSynchronous<Integer, Pooled, Pooled, Integer> p =
      this.newPool(
        listener,
        0L,
        0b11111111_11111111_11111111_11111111_11111111_11111111_11111111_11111111L);

    this.expected.expect(JPPoolInternalOverflowException.class);
    p.get(Integer.valueOf(2), Integer.valueOf(0));
  }

  @Test
  public final void testBadLimit_0()
  {
    this.expected.expect(RangeCheckException.class);
    this.newPool(new UnitListener(), 1L, 0L);
  }

  private static class IntCreationFailureObjectListener extends PooledListener
  {
    @Override
    public Pooled onCreate(
      final Integer c,
      final Integer key)
    {
      super.onCreate(c, key);
      throw new RuntimeException("Failed for " + key);
    }
  }

  private static class IntCreationFailureEstimateSizeListener extends
    PooledListener
  {
    @Override
    public long onEstimateSize(
      final Integer c,
      final Integer key)
    {
      super.onEstimateSize(c, key);
      throw new RuntimeException("Failed for " + key);
    }
  }

  private static class IntCreationFailureSizeListener extends PooledListener
  {
    @Override
    public long onGetSize(
      final Integer c,
      final Integer key,
      final Pooled value)
    {
      super.onGetSize(c, key, value);
      throw new RuntimeException("Failed for " + key);
    }
  }

  private static final class Pooled
  {
    private int value;
    private boolean deleted;

    Pooled()
    {
      this.deleted = false;
    }

    Pooled(final int x)
    {
      this.value = x;
      this.deleted = false;
    }

    @Override
    public boolean equals(final Object o)
    {
      if (this == o) {
        return true;
      }
      if (o == null || this.getClass() != o.getClass()) {
        return false;
      }

      final Pooled pooled = (Pooled) o;
      return this.value == pooled.value;
    }

    @Override
    public int hashCode()
    {
      return this.value;
    }
  }

  private static class PooledListenerReuseErrorSuppressed extends PooledListener
  {
    @Override
    public void onReuse(
      final Integer c,
      final Integer key,
      final Pooled value)
    {
      super.onReuse(c, key, value);
      throw new IllegalArgumentException("x");
    }

    @Override
    public void onError(
      final Integer c,
      final Integer key,
      final Optional<Pooled> value,
      final Throwable e)
    {
      super.onError(c, key, value, e);
      throw new RuntimeException(e);
    }
  }

  private static class PooledListener implements
    JPPoolableListenerType<Integer, Pooled, Integer>
  {
    Throwable error;
    long estimated_size;
    int creates;
    int deletes;
    int reuses;
    long size = 1L;

    PooledListener()
    {

    }

    @Override
    public long onEstimateSize(
      final Integer c,
      final Integer key)
    {
      LOG.debug("onEstimateSize: {} {}", c, key);
      return this.estimated_size;
    }

    @Override
    public Pooled onCreate(
      final Integer c,
      final Integer key)
    {
      LOG.debug("onCreate: {} {}", c, key);
      ++this.creates;

      final Pooled p = new Pooled();
      p.value = c.intValue() * key.intValue();
      return p;
    }

    @Override
    public long onGetSize(
      final Integer c,
      final Integer key,
      final Pooled value)
    {
      LOG.debug("onGetSize: {} {} {}", c, key, value);
      return this.size;
    }

    @Override
    public void onReuse(
      final Integer c,
      final Integer key,
      final Pooled value)
    {
      LOG.debug("onReuse: {} {} {}", c, key, value);
      ++this.reuses;
    }

    @Override
    public void onDelete(
      final Integer c,
      final Integer key,
      final Pooled value)
    {
      LOG.debug("onDelete: {} {} {}", c, key, value);
      ++this.deletes;
      value.deleted = true;
    }

    @Override
    public void onError(
      final Integer c,
      final Integer key,
      final Optional<Pooled> value,
      final Throwable e)
    {
      LOG.error("error: ", e);
      this.error = e;
    }
  }


  private static final class Unit
  {
    private Unit()
    {

    }

    public static Unit unit()
    {
      return new Unit();
    }

    @Override
    public boolean equals(final Object o)
    {
      if (this == o) {
        return true;
      }
      return o != null && Objects.equals(this.getClass(), o.getClass());
    }
  }

  private static class UnitListener
    implements JPPoolableListenerType<Unit, Unit, Unit>
  {
    @Override
    public long onEstimateSize(
      final Unit unit,
      final Unit key)
    {
      return 0L;
    }

    @Override
    public Unit onCreate(
      final Unit unit,
      final Unit key)
    {
      return Unit.unit();
    }

    @Override
    public long onGetSize(
      final Unit unit,
      final Unit key,
      final Unit value)
    {
      return 1L;
    }

    @Override
    public void onReuse(
      final Unit unit,
      final Unit key,
      final Unit value)
    {

    }

    @Override
    public void onDelete(
      final Unit unit,
      final Unit key,
      final Unit value)
    {

    }

    @Override
    public void onError(
      final Unit unit,
      final Unit key,
      final Optional<Unit> value,
      final Throwable e)
    {
      LOG.error("error: ", e);
    }
  }
}

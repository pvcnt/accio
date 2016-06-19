/*
 * Copyright LIRIS-CNRS (2016)
 * Contributors: Vincent Primault <vincent.primault@liris.cnrs.fr>
 *
 * This software is a computer program whose purpose is to study location privacy.
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */

package fr.cnrs.liris.common.util.cache

import java.util.concurrent.TimeUnit

import com.google.common.base.Ticker
import com.google.common.cache.{CacheBuilder => GuavaCacheBuilder, CacheLoader, RemovalListener, RemovalNotification, Weigher}
import com.twitter.util.Duration

/**
 * A Scala adapter around Guava's CacheBuilder.
 *
 * @param builder A Guava CacheBuilder
 * @tparam K The base key type for all caches created by this builder
 * @tparam V The base value type for all caches created by this builder
 */
class CacheBuilder[K <: AnyRef, V <: AnyRef] private(builder: GuavaCacheBuilder[K, V]) {
  def initialCapacity(initialCapacity: Int): CacheBuilder[K, V] =
    new CacheBuilder(builder.initialCapacity(initialCapacity))

  def concurrencyLevel(concurrencyLevel: Int): CacheBuilder[K, V] =
    new CacheBuilder(builder.concurrencyLevel(concurrencyLevel))

  def maximumSize(maximumSize: Long): CacheBuilder[K, V] =
    new CacheBuilder(builder.maximumSize(maximumSize))

  def maximumWeight(maximumWeight: Long): CacheBuilder[K, V] =
    new CacheBuilder(builder.maximumWeight(maximumWeight))

  def weigher[K1 <: K, V1 <: V](weigher: Weigher[K1, V1]): CacheBuilder[K1, V1] =
    new CacheBuilder(builder.weigher(weigher))

  def weigher[K1 <: K, V1 <: V](weigher: (K1, V1) => Int): CacheBuilder[K1, V1] =
    new CacheBuilder(builder.weigher(new Weigher[K1, V1] {
      override def weigh(key: K1, value: V1): Int = weigher(key, value)
    }))

  def weakKeys: CacheBuilder[K, V] = new CacheBuilder(builder.weakKeys)

  def weakValues: CacheBuilder[K, V] = new CacheBuilder(builder.weakValues)

  def softValues: CacheBuilder[K, V] = new CacheBuilder(builder.softValues)

  def expireAfterWrite(duration: Duration): CacheBuilder[K, V] =
    new CacheBuilder(builder.expireAfterWrite(duration.inNanoseconds, TimeUnit.NANOSECONDS))

  def expireAfterAccess(duration: Duration): CacheBuilder[K, V] =
    new CacheBuilder(builder.expireAfterAccess(duration.inNanoseconds, TimeUnit.NANOSECONDS))

  def refreshAfterWrite(duration: Duration): CacheBuilder[K, V] =
    new CacheBuilder(builder.refreshAfterWrite(duration.inNanoseconds, TimeUnit.NANOSECONDS))

  def ticker(ticker: Ticker): CacheBuilder[K, V] = new CacheBuilder(builder.ticker(ticker))

  def removalListener(fn: RemovalNotification[K, V] => Unit): CacheBuilder[K, V] =
    new CacheBuilder(builder.removalListener(new RemovalListener[K, V] {
      override def onRemoval(notif: RemovalNotification[K, V]): Unit = fn(notif)
    }))

  def recordStats: CacheBuilder[K, V] = new CacheBuilder(builder.recordStats)

  def build[K1 <: K, V1 <: V](fn: () => V1): LoadingCache[K1, V1] = {
    val loader = CacheLoader.from(new com.google.common.base.Supplier[V1] {
      override def get: V1 = fn()
    })
    new LoadingCache[K1, V1](builder.build[K1, V1](loader))
  }

  def build[K1 <: K, V1 <: V](fn: K1 => V1): LoadingCache[K1, V1] = {
    val loader = CacheLoader.from(new com.google.common.base.Function[K1, V1] {
      override def apply(key: K1): V1 = fn(key)
    })
    new LoadingCache(builder.build[K1, V1](loader))
  }

  def build[K1 <: K, V1 <: V]: Cache[K1, V1] = new Cache(builder.build[K1, V1])

  override def toString: String = builder.toString
}

object CacheBuilder {
  def apply(): CacheBuilder[AnyRef, AnyRef] = new CacheBuilder(GuavaCacheBuilder.newBuilder)
}
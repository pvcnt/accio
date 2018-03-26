/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
 *
 * Accio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Accio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Accio.  If not, see <http://www.gnu.org/licenses/>.
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
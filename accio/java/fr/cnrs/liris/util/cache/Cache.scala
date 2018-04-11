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

package fr.cnrs.liris.util.cache

import java.util.concurrent.ConcurrentMap

import com.google.common.cache.{Cache => GuavaCache, CacheStats}
import com.twitter.conversions.thread._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionException

/**
 * A Scala adapter around Guava's Cache.
 *
 * @param cache A Guava Cache
 * @tparam K The type of cached keys
 * @tparam V The type of cached values
 */
class Cache[K, V](cache: GuavaCache[K, V]) {
  def get(key: AnyRef): Option[V] = Option(cache.getIfPresent(key))

  @throws[ExecutionException]
  def apply(key: K, fn: => V): V = cache.get(key, fn)

  def getAllPresent(keys: Iterable[_]): Map[K, V] = cache.getAllPresent(keys.asJava).asScala.toMap

  def put(key: K, value: V): Unit = cache.put(key, value)

  def putAll(values: Map[_ <: K, _ <: V]): Unit = cache.putAll(values.asJava)

  def invalidate(key: AnyRef): Unit = cache.invalidate(key)

  def invalidateAll(keys: Iterable[_]): Unit = cache.invalidateAll(keys.asJava)

  def invalidateAll(): Unit = cache.invalidateAll()

  def size: Long = cache.size

  def stats: CacheStats = cache.stats

  def asMap: ConcurrentMap[K, V] = cache.asMap

  def cleanUp(): Unit = cache.cleanUp()

  override def toString: String = cache.toString
}

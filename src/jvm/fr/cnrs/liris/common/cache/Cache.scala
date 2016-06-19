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

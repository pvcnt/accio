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

import com.google.common.cache.{LoadingCache => GuavaLoadingCache}
import com.google.common.util.concurrent.{ExecutionError, UncheckedExecutionException}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionException

/**
 * A Scala adapter around Guava's LoadingCache.
 *
 * @param cache A Guava LoadingCache
 * @tparam K The type of cached keys
 * @tparam V The type of cached values
 */
class LoadingCache[K, V](cache: GuavaLoadingCache[K, V]) extends Cache[K, V](cache) {
  /**
   * Returns the value associated with `key` in this cache, first loading that value if
   * necessary. No observable state associated with this cache is modified until loading completes.
   *
   * If another call to [[get]] or [[getUnchecked]] is currently loading the value for `key`,
   * simply waits for that thread to finish and returns its loaded value. Note that multiple
   * threads can concurrently load values for distinct keys.
   *
   * Caches loaded by a cache loader will use it to load new values into the cache. Newly loaded
   * values are added to the cache using [[Cache.asMap.putIfAbsent]] after loading has completed;
   * if another value was associated with `key` while the new value was loading then a removal
   * notification will be sent for the new value.
   *
   * If the cache loader associated with this cache is known not to throw checked exceptions, then
   * prefer [[getUnchecked]] over this method.
   *
   * @throws ExecutionException          if a checked exception was thrown while loading the value.
   *                                     [[ExecutionException]]} is thrown even if
   *                                     computation was interrupted by an [[InterruptedException]])
   * @throws UncheckedExecutionException if an unchecked exception was thrown while loading the
   *                                     value
   * @throws ExecutionError              if an error was thrown while loading the value
   */
  @throws[ExecutionException]
  def apply(key: K): V = cache.get(key)

  /**
   * Returns the value associated with `key` in this cache, first loading that value if
   * necessary. No observable state associated with this cache is modified until loading
   * completes. Unlike {@link #get}, this method does not throw a checked exception, and thus should
   * only be used in situations where checked exceptions are not thrown by the cache loader.
   *
   * <p>If another call to {@link #get} or {@link #getUnchecked} is currently loading the value for
   * `key`, simply waits for that thread to finish and returns its loaded value. Note that
   * multiple threads can concurrently load values for distinct keys.
   *
   * <p>Caches loaded by a {@link CacheLoader} will call {@link CacheLoader#load} to load new values
   * into the cache. Newly loaded values are added to the cache using
   * {@code Cache.asMap().putIfAbsent} after loading has completed; if another value was associated
   * with `key` while the new value was loading then a removal notification will be sent for
   * the new value.
   *
   * <p><b>Warning:</b> this method silently converts checked exceptions to unchecked exceptions,
   * and should not be used with cache loaders which throw checked exceptions. In such cases use
   * {@link #get} instead.
   *
   * @throws UncheckedExecutionException if an exception was thrown while loading the value. (As
   *                                     explained in the last paragraph above, this should be an unchecked exception only.)
   * @throws ExecutionError              if an error was thrown while loading the value
   */
  def getUnchecked(key: K): V = cache.getUnchecked(key)

  /**
   * Returns a map of the values associated with `keys`, creating or retrieving those values
   * if necessary. The returned map contains entries that were already cached, combined with newly
   * loaded entries; it will never contain null keys or values.
   *
   * <p>Caches loaded by a {@link CacheLoader} will issue a single request to
   * {@link CacheLoader#loadAll} for all keys which are not already present in the cache. All
   * entries returned by {@link CacheLoader#loadAll} will be stored in the cache, over-writing
   * any previously cached values. This method will throw an exception if
   * {@link CacheLoader#loadAll} returns {@code null}, returns a map containing null keys or values,
   * or fails to return an entry for each requested key.
   *
   * <p>Note that duplicate elements in `keys`, as determined by {@link Object#equals}, will
   * be ignored.
   *
   * @throws ExecutionException          if a checked exception was thrown while loading the value. ({ @code
   *                                     ExecutionException} is thrown <a
   *                                     href="https://github.com/google/guava/wiki/CachesExplained#interruption">even if
   *                                     computation was interrupted by an { @code InterruptedException}</a>.)
   * @throws UncheckedExecutionException if an unchecked exception was thrown while loading the
   *                                     values
   * @throws ExecutionError              if an error was thrown while loading the values
   * @since 11.0
   */
  @throws[ExecutionException]
  def getAll(keys: Iterable[_ <: K]): Map[K, V] = cache.getAllPresent(keys.asJava).asScala.toMap

  /**
   * Loads a new value for key `key`, possibly asynchronously. While the new value is loading
   * the previous value (if any) will continue to be returned by {@code get(key)} unless it is
   * evicted. If the new value is loaded successfully it will replace the previous value in the
   * cache; if an exception is thrown while refreshing the previous value will remain, <i>and the
   * exception will be logged (using {@link java.util.logging.Logger}) and swallowed</i>.
   *
   * <p>Caches loaded by a {@link CacheLoader} will call {@link CacheLoader#reload} if the
   * cache currently contains a value for `key`, and {@link CacheLoader#load} otherwise.
   * Loading is asynchronous only if {@link CacheLoader#reload} was overridden with an
   * asynchronous implementation.
   *
   * <p>Returns without doing anything if another thread is currently loading the value for
   * `key`. If the cache loader associated with this cache performs refresh asynchronously
   * then this method may return before refresh completes.
   */
  def refresh(key: K): Unit = cache.refresh(key)

  /**
   * {@inheritDoc }
   *
   * <p><b>Note that although the view <i>is</i> modifiable, no method on the returned map will ever
   * cause entries to be automatically loaded.</b>
   */
  override def asMap: ConcurrentMap[K, V] = cache.asMap
}

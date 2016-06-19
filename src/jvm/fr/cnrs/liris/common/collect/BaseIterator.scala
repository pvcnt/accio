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

package fr.cnrs.liris.common.collect

/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.NoSuchElementException

import com.google.common.base.Preconditions.checkState

import scala.collection.AbstractIterator

/**
 * This class provides a skeletal implementation of the [[Iterator]] interface, to make this
 * interface easier to implement for certain types of data sources.
 *
 * [[Iterator]] requires its implementations to support querying the end-of-data status without
 * changing the iterator's state, using the [[Iterator.hasNext]] method. But many data sources, such as
 * [[java.io.Reader.read]], do not expose this information; the only way to discover whether there
 * is any data left is by trying to retrieve it. These types of data sources are ordinarily
 * difficult to write iterators for. But using this class, one must implement only the
 * [[BaseIterator.computeNext]] method.
 *
 * This class supports iterators that include null elements.
 */
trait BaseIterator[T] extends AbstractIterator[T] {
  private[this] var state: State = State.NotReady
  private[this] var nextElement: Option[T] = None

  /**
   * Returns the next element.
   *
   * The initial invocation of [[hasNext]] or [[next]] calls this method, as does the first
   * invocation of [[hasNext]] or [[next]] following each successful call to [[next]]. Once the
   * implementation either returns [[None]] or throws an exception, [[computeNext]] is guaranteed
   * to never be called again.
   *
   * If this method throws an exception, it will propagate outward to the [[hasNext]] or [[next]]
   * invocation that invoked this method. Any further attempts to use the iterator will result in
   * an [[IllegalStateException]].
   *
   * The implementation of this method may not invoke the [[hasNext]], [[next]], or [[peek]]
   * methods on this instance; if it does, an [[IllegalStateException]] will result.
   *
   * @return the next element if there was one
   * @throws RuntimeException if any unrecoverable error happens
   */
  protected def computeNext(): Option[T]

  override final def hasNext: Boolean = {
    checkState(state != State.Failed)
    state match {
      case State.Done => false
      case State.Ready => true
      case _ => tryToComputeNext()
    }
  }

  private def tryToComputeNext(): Boolean = {
    state = State.Failed //Temporary pessimism
    nextElement = computeNext()
    nextElement match {
      case Some(_) =>
        state = State.Ready
        true
      case None =>
        state = State.Done
        false
    }
  }

  override final def next(): T = {
    if (!hasNext) {
      throw new NoSuchElementException()
    }
    state = State.NotReady
    val res = nextElement.get
    nextElement = None
    res
  }

  /**
   * Return the next element in the iteration without advancing the iteration, according to the
   * contract of [[PeekingIterator.peek]]
   *
   * Implementations of [[BaseIterator]] that wish to expose this functionality should implement
   * [[PeekingIterator]].
   */
  final def peek(): T = {
    if (!hasNext) {
      throw new NoSuchElementException()
    }
    nextElement.get
  }
}

private sealed trait State

private object State {

  /** We have computed the next element and haven't returned it yet. */
  case object Ready extends State

  /** We haven't yet computed or have already returned the element. */
  case object NotReady extends State

  /** We have reached the end of the data and are finished. */
  case object Done extends State

  /** We've suffered an exception and are kaput. */
  case object Failed extends State

}
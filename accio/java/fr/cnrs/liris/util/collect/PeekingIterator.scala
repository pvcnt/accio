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

package fr.cnrs.liris.util.collect

/**
 * An iterator that supports a one-element lookahead while iterating.
 *
 * Calls to the [[peek]] method with no intervening calls to [[next]] do not affect the
 * iteration, and hence return the same object each time. A subsequent call to [[next]] is
 * guaranteed to return the same object again. For example:
 *
 * {{{
 * val peekingIterator = new PeekingIterator(Iterators.forArray("a", "b"))
 * val a1 = peekingIterator.peek() // returns "a"
 * val String a2 = peekingIterator.peek() // also returns "a"
 * val String a3 = peekingIterator.next() // also returns "a"
 * }}}
 *
 * @param it the backing iterator. The { @link PeekingIterator} assumes ownership of this
 *           iterator, so users should cease making direct calls to it after calling this method.
 * @return a peeking iterator backed by that iterator. Apart from the additional { @link
 *         PeekingIterator#peek()} method, this iterator behaves exactly the same as { @code iterator}.
 */
final class PeekingIterator[T](it: Iterator[T]) extends Iterator[T] {
  private[this] var peekedElement: Option[T] = None

  override def hasNext: Boolean = peekedElement.isDefined || it.hasNext

  override def next(): T = peekedElement match {
    case None => it.next()
    case Some(element) =>
      peekedElement = None
      element
  }

  def peek(): T = peekedElement match {
    case Some(element) => element
    case None =>
      val element = it.next()
      peekedElement = Some(element)
      element
  }
}

object PeekingIterator {
  /**
   * <p>Note: If the given iterator is already a {@code PeekingIterator}, it <i>might</i> be
   * returned to the caller, although this is neither guaranteed to occur nor required to be
   * consistent. For example, this method <i>might</i> choose to pass through recognized
   * implementations of {@code PeekingIterator} when the behavior of the implementation is known to
   * meet the contract guaranteed by this method.
   *
   * @param it
   * @tparam T
   * @return
   */
  def apply[T](it: Iterator[T]): PeekingIterator[T] = new PeekingIterator(it)
}
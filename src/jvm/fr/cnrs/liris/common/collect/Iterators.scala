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

import scala.collection.mutable

object Iterators {
  def singleton[T](value: => T): Iterator[T] = new SingletonIterator[T](value)

  def distinct[T](it: Iterator[T]): Iterator[T] = new DistinctIterator[T](it)

  def peeking[T](it: Iterator[T]): PeekingIterator[T] = new PeekingImpl[T](it)
}

private class SingletonIterator[T](value: => T) extends Iterator[T] {
  private[this] var exhausted = false

  override def hasNext: Boolean = !exhausted

  override def next(): T = {
    if (exhausted) {
      throw new IllegalStateException("Iterator is already exhausted")
    }
    exhausted = true
    value
  }
}

private class DistinctIterator[T](it: Iterator[T]) extends BaseIterator[T] {
  private[this] val previous = mutable.Set.empty[Int]

  override protected def computeNext(): Option[T] = {
    var res: Option[T] = None
    while (it.hasNext && res.isEmpty) {
      res = Some(it.next())
      val hash = res.get.hashCode
      if (previous.contains(hash)) {
        res = None
      } else {
        previous += hash
      }
    }
    res
  }
}

private class PeekingImpl[T](it: Iterator[T]) extends PeekingIterator[T] {
  private var peekedElement: Option[T] = None

  override def hasNext: Boolean = peekedElement.isDefined || it.hasNext

  override def next(): T = {
    if (peekedElement.isEmpty) {
      return it.next()
    }
    val res = peekedElement.get
    peekedElement = None
    res
  }

  override def peek(): T = {
    if (peekedElement.isEmpty) {
      peekedElement = Some(it.next())
    }
    peekedElement.get
  }
}
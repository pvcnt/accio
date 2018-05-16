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

package fr.cnrs.liris.locapriv.ops

import fr.cnrs.liris.locapriv.domain.Event

import scala.collection.mutable

/**
 * Base trait for transformers using previous event and current one to take a decision about
 * whether to keep it or not.
 */
private[ops] trait SlidingSamplingOp extends TransformOp[Event] {
  this: Product =>

  protected def sample(prev: Event, curr: Event): Boolean

  override protected def transform(key: String, trace: Iterable[Event]): Iterable[Event] = {
    if (trace.isEmpty) {
      trace
    } else {
      val result = mutable.ListBuffer.empty[Event]
      var maybePrev: Option[Event] = None
      for (event <- trace) {
        val keep = maybePrev match {
          case Some(prev) => sample(prev, event)
          case None => true
        }
        if (keep) {
          result += event
          maybePrev = Some(event)
        }
      }
      result
    }
  }
}
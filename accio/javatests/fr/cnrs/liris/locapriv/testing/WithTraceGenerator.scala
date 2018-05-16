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

package fr.cnrs.liris.locapriv.testing

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.locapriv.domain.Event
import fr.cnrs.liris.util.geo.{LatLng, Point}
import org.joda.time.Instant

import scala.collection.mutable
import scala.util.Random

/**
 * Trait allowing to generate random traces.
 */
trait WithTraceGenerator {
  protected val Here: Point = LatLng.degrees(48.858222, 2.2945).toPoint
  protected val Now: Instant = Instant.now
  protected val Me = "me"
  protected val Him = "him"

  protected def randomLocation(): LatLng = LatLng.degrees(Random.nextDouble() * 180 - 90, Random.nextDouble() * 360 - 180)

  protected def randomTrace(user: String, size: Int, rate: => Duration = Duration.standardSeconds(1)): Seq[Event] = {
    if (size <= 0) {
      Seq.empty
    } else {
      val events = mutable.ListBuffer.empty[Event]
      var now = Now
      (0 until size).foreach { _ =>
        events += Event(user, randomLocation(), now)
        now += rate
      }
      events.toList
    }
  }

  protected def randomFixedTrace(user: String, size: Int): Seq[Event] = {
    if (size <= 0) {
      Seq.empty
    } else {
      Seq.tabulate(size)(i => Event(user, Here, Now + Duration.standardSeconds(i)))
    }
  }
}
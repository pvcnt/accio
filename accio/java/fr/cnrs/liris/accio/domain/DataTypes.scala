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

package fr.cnrs.liris.accio.domain

import fr.cnrs.liris.lumos.domain.{DataType, Value}
import fr.cnrs.liris.util.geo
import com.twitter.util.{Duration => TwitterDuration}
import org.joda.time.{Instant, Duration => JodaDuration}

import scala.reflect.{ClassTag, classTag}
import scala.util.Try

object DataTypes {
  def register(): Unit = {
    Seq(Distance, Timestamp, Duration).foreach(DataType.register)
  }

  object Distance extends DataType.UserDefined {
    override type JvmType = geo.Distance

    override def name: String = "Distance"

    override def help = "a distance"

    override def encode(v: this.JvmType): Value = Value.Double(v.meters)

    override def decode(value: Value): Option[this.JvmType] =
      value match {
        case Value.Double(v) => Some(geo.Distance.meters(v))
        case Value.Float(v) => Some(geo.Distance.meters(v))
        case Value.Int(v) => Some(geo.Distance.meters(v))
        case Value.String(v) => Try(geo.Distance.parse(v)).toOption
        case Value.UserDefined(v, Distance) => Some(v.asInstanceOf[JvmType])
        case _ => None
      }

    override def cls: ClassTag[this.JvmType] = classTag[geo.Distance]
  }

  object Timestamp extends DataType.UserDefined {
    override type JvmType = Instant

    override def name: String = "Timestamp"

    override def help = "an instant"

    override def encode(v: this.JvmType): Value = Value.Long(v.getMillis)

    override def decode(value: Value): Option[this.JvmType] =
      value match {
        case Value.Long(v) => Some(new Instant(v))
        case Value.UserDefined(v, Timestamp) => Some(v.asInstanceOf[JvmType])
        case _ => None
      }

    override def cls: ClassTag[this.JvmType] = classTag[Instant]
  }

  object Duration extends DataType.UserDefined {
    override type JvmType = JodaDuration

    override def name: String = "Duration"

    override def help = "a duration"

    override def encode(v: this.JvmType): Value = Value.Long(v.getMillis)

    override def decode(value: Value): Option[this.JvmType] =
      value match {
        case Value.Int(v) => Some(new org.joda.time.Duration(v))
        case Value.Long(v) => Some(new org.joda.time.Duration(v))
        case Value.String(v) =>
          // We use Twitter's format for durations (e.g., "3.minutes").
          Try(new JodaDuration(TwitterDuration.parse(v).inMillis)).toOption
        case Value.UserDefined(v, Duration) => Some(v.asInstanceOf[JvmType])
        case _ => None
      }

    override def cls: ClassTag[this.JvmType] = classTag[JodaDuration]
  }

}

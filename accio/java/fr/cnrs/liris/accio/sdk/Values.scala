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

package fr.cnrs.liris.accio.sdk

import fr.cnrs.liris.lumos.domain.{DataType, Value}
import fr.cnrs.liris.util.geo.Distance
import org.joda.time.{Duration, Instant, ReadableDuration, ReadableInstant}

import scala.reflect.runtime.universe.{Type, typeOf}

object Values {
  def dataTypeOf(tpe: Type): (DataType, Set[String]) =
    tpe.dealias match {
      case t if t <:< typeOf[java.lang.Integer] => (DataType.Int, Set.empty)
      case t if t <:< typeOf[java.lang.Long] => (DataType.Long, Set.empty)
      case t if t <:< typeOf[java.lang.Float] => (DataType.Float, Set.empty)
      case t if t <:< typeOf[java.lang.Double] => (DataType.Double, Set.empty)
      case t if t <:< typeOf[java.lang.Boolean] => (DataType.Bool, Set.empty)
      case t if t <:< typeOf[String] => (DataType.String, Set.empty)
      case t if t <:< typeOf[Int] => (DataType.Int, Set.empty)
      case t if t <:< typeOf[Long] => (DataType.Long, Set.empty)
      case t if t <:< typeOf[Float] => (DataType.Float, Set.empty)
      case t if t <:< typeOf[Double] => (DataType.Double, Set.empty)
      case t if t <:< typeOf[Boolean] => (DataType.Bool, Set.empty)
      case t if t <:< typeOf[org.joda.time.ReadableInstant] => (DataType.Long, Set("time"))
      case t if t <:< typeOf[org.joda.time.ReadableDuration] => (DataType.Long, Set("duration"))
      case t if t <:< typeOf[fr.cnrs.liris.util.geo.Distance] => (DataType.Double, Set("distance"))
      case _ => throw new IllegalArgumentException(s"Unsupported Scala type: $tpe")
    }

  def encode(v: Any, dataType: DataType, aspects: Set[String] = Set.empty): Option[Value] = {
    if (aspects.contains("time")) {
      Value.Long(v.asInstanceOf[ReadableInstant].getMillis).cast(dataType)
    } else if (aspects.contains("duration")) {
      Value.Long(v.asInstanceOf[ReadableDuration].getMillis).cast(dataType)
    } else if (aspects.contains("distance")) {
      Value.Double(v.asInstanceOf[Distance].meters).cast(dataType)
    } else {
      Some(Value(v, dataType))
    }
  }

  def decode(value: Value, dataType: DataType, aspects: Set[String] = Set.empty): Option[Any] = {
    value.cast(dataType).map { normalizedValue =>
      if (aspects.contains("time")) {
        normalizedValue match {
          case Value.Long(v) => Some(new Instant(v))
          case _ => None
        }
      } else if (aspects.contains("duration")) {
        normalizedValue match {
          case Value.Long(v) => Some(new Duration(v))
          case _ => None
        }
      } else if (aspects.contains("distance")) {
        normalizedValue match {
          case Value.Double(v) => Some(Distance.meters(v))
          case _ => None
        }
      } else {
        Some(normalizedValue.v)
      }
    }
  }
}

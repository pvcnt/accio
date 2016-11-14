/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.core.framework

import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonSubTypes}
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.common.geo.Distance
import org.joda.time.{Instant, Duration => JodaDuration}

/**
 * An exploration is a way to explore a space of parameters.
 */
@JsonSubTypes(Array(
  new JsonSubTypes.Type(value = classOf[SingletonExploration], name = "value"),
  new JsonSubTypes.Type(value = classOf[ListExploration], name = "values"),
  new JsonSubTypes.Type(value = classOf[RangeExploration], name = "from")
))
@JsonIgnoreProperties(ignoreUnknown = true)
sealed trait Exploration {
  /**
   * Expand an exploration into a set of values, w.r.t. a given data type.
   *
   * @param kind Data type.
   * @throws IllegalArgumentException If it cannot be expanded w.r.t. the given data type.
   */
  @throws[IllegalArgumentException]
  def expand(kind: DataType): Set[Any]
}

/**
 * An exploration which only explores only a single value. It is mainly useful to override the
 * value of a parameter.
 *
 * @param value New parameter's value.
 */
case class SingletonExploration(value: Any) extends Exploration {
  override def expand(kind: DataType): Set[Any] = Set(Values.as(value, kind))
}

/**
 * An exploration which explores a fixed set of values.
 *
 * @param values List of values taken by the parameter.
 */
case class ListExploration(values: Set[Any]) extends Exploration {
  override def expand(kind: DataType): Set[Any] = values.map(Values.as(_, kind))
}

/**
 * An exploration which explores a range of values.
 *
 * @param from  First value taken by the parameter.
 * @param to    Last value taken by the parameter.
 * @param step  Step between two values of parameter.
 * @param log   Whether to follow a logarithmic progression (base e).
 * @param log10 Whether to follow a logarithmic progression (base 10).
 * @param log2  Whether to follow a logarithmic progression (base 2).
 */
case class RangeExploration(from: Any, to: Any, step: Any, log: Boolean = false, log10: Boolean = false, log2: Boolean = false) extends Exploration with LazyLogging {
  override def expand(kind: DataType): Set[Any] = kind match {
    case DataType.Byte =>
      checkNotLogarithmic(kind)
      (Values.asByte(from) to Values.asByte(to) by Values.asByte(step)).toSet
    case DataType.Short =>
      checkNotLogarithmic(kind)
      (Values.asShort(from) to Values.asShort(to) by Values.asShort(step)).toSet
    case DataType.Integer =>
      checkNotLogarithmic(kind)
      (Values.asInteger(from) to Values.asInteger(to) by Values.asInteger(step)).toSet
    case DataType.Long =>
      checkNotLogarithmic(kind)
      (Values.asLong(from) to Values.asLong(to) by Values.asLong(step)).toSet
    case DataType.Double =>
      checkLogarithmic()
      val dFrom = applyLog(Values.asDouble(from))
      val dTo = applyLog(Values.asDouble(to))
      val dStep = applyLog(Values.asDouble(step))
      (dFrom to dTo by dStep).map(reverseLog).toSet
    case DataType.Distance =>
      checkLogarithmic()
      val dFrom = applyLog(Values.asDistance(from).meters)
      val dTo = applyLog(Values.asDistance(to).meters)
      val dStep = applyLog(Values.asDistance(step).meters)
      (dFrom to dTo by dStep).map(v => Distance.meters(reverseLog(v))).toSet
    case DataType.Duration =>
      checkLogarithmic()
      val dFrom = applyLog(Values.asDuration(from).getMillis)
      val dTo = applyLog(Values.asDuration(to).getMillis)
      val dStep = applyLog(Values.asDuration(step).getMillis)
      (dFrom to dTo by dStep).map(v => new JodaDuration(reverseLog(v).round)).toSet
    case DataType.Timestamp =>
      checkNotLogarithmic(kind)
      val tFrom = Values.asTimestamp(from).getMillis
      val tTo = Values.asTimestamp(to).getMillis
      val dStep = Values.asDuration(step).getMillis
      (tFrom to tTo by dStep).map(new Instant(_)).toSet
    case _ => throw new IllegalArgumentException(s"Cannot generate a range of $kind")
  }

  private def checkLogarithmic() = {
    if (log && (log10 || log2)) {
      logger.error("Cannot apply multiple logarithmic progressions in different bases, will use base e")
    } else if (log10 && log2) {
      logger.error("Cannot apply multiple logarithmic progressions in different bases, will use base 10")
    }
  }

  private def checkNotLogarithmic(kind: DataType) = {
    if (log || log2 || log10) {
      logger.error(s"Cannot use logarithmic progression with ${kind.typeDescription}s")
    }
  }

  private def applyLog(d: Double) =
    if (log) math.log(d) else if (log10) math.log10(d) else if (log2) math.log(d) / math.log(2)  else d

  private def reverseLog(d: Double) =
    if (log) math.pow(math.E, d) else if (log10) math.pow(10, d) else if (log2) math.pow(2, d) else d
}
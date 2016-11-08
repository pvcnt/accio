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

import com.twitter.util.{Duration => TwitterDuration}
import fr.cnrs.liris.accio.core.api.{Dataset, Image}
import fr.cnrs.liris.common.geo.{LatLng, Location}
import fr.cnrs.liris.common.geo.Distance
import org.joda.time.{Instant, Duration => JodaDuration}

/**
 * Helper functions to deal with values of various data types.
 */
object Values {
  private[this] val instantOrdering = new Ordering[Instant] {
    override def compare(x: Instant, y: Instant): Int = x.compareTo(y)
  }
  private[this] val durationOrdering = new Ordering[JodaDuration] {
    override def compare(x: JodaDuration, y: JodaDuration): Int = x.compareTo(y)
  }

  /**
   * Correct a raw value to match a data type.
   *
   * Note: This method produce [[Any]] from [[Any]], so in appearance we do nothing. In reality the output
   * value is of the correct Scala type and can be used later safely.
   *
   * @param rawValue Raw value.
   * @param kind     Data type.
   * @throws IllegalArgumentException If the raw value is incorrect w.r.t. the data type.
   * @return Corrected value.
   */
  @throws[IllegalArgumentException]
  def as(rawValue: Any, kind: DataType): Any = rawValue match {
    case None => None
    case Some(obj) => Some(as(obj, kind))
    case _ =>
      kind match {
        case DataType.Boolean => asBoolean(rawValue)
        case DataType.Byte => asByte(rawValue)
        case DataType.Short => asShort(rawValue)
        case DataType.Integer => asInteger(rawValue)
        case DataType.Long => asLong(rawValue)
        case DataType.Double => asDouble(rawValue)
        case DataType.String => asString(rawValue)
        case DataType.Location => asLocation(rawValue)
        case DataType.Timestamp => asTimestamp(rawValue)
        case DataType.Duration => asDuration(rawValue)
        case DataType.Distance => asDistance(rawValue)
        case DataType.Image => asImage(rawValue)
        case DataType.Dataset => asDataset(rawValue)
        case DataType.List(of) => asList(rawValue, of)
        case DataType.Set(of) => asSet(rawValue, of)
        case DataType.Map(ofKeys, ofValues) => asMap(rawValue, ofKeys, ofValues)
      }
  }

  def asByte(rawValue: Any): Byte = rawValue match {
    case s: String => parseByte(s)
    case n: java.lang.Number => n.byteValue
    case b: Byte => b
    case _ => throwInvalidTypeException(DataType.Byte, rawValue)
  }

  def asShort(rawValue: Any): Short = rawValue match {
    case s: String => parseShort(s)
    case n: java.lang.Number => n.shortValue
    case b: Byte => b
    case s: Short => s
    case _ => throwInvalidTypeException(DataType.Short, rawValue)
  }

  def asInteger(rawValue: Any): Int = rawValue match {
    case s: String => parseInteger(s)
    case n: java.lang.Number => n.intValue
    case b: Byte => b
    case s: Short => s
    case i: Int => i
    case _ => throwInvalidTypeException(DataType.Integer, rawValue)
  }

  def asLong(rawValue: Any): Long = rawValue match {
    case s: String => parseLong(s)
    case n: java.lang.Number => n.longValue
    case b: Byte => b
    case s: Short => s
    case i: Int => i
    case l: Long => l
    case _ => throwInvalidTypeException(DataType.Long, rawValue)
  }

  def asDouble(rawValue: Any): Double = rawValue match {
    case s: String => parseDouble(s)
    case n: java.lang.Number => n.doubleValue
    case b: Byte => b
    case s: Short => s
    case i: Int => i
    case l: Long => l
    case d: Double => d
    case _ => throwInvalidTypeException(DataType.Double, rawValue)
  }

  def asBoolean(rawValue: Any): Boolean = rawValue match {
    case s: String => parseBoolean(s)
    case b: java.lang.Boolean => b.asInstanceOf[Boolean]
    case b: Boolean => b
    case _ => throwInvalidTypeException(DataType.Boolean, rawValue)
  }

  def asString(rawValue: Any): String = rawValue match {
    case s: String => s
    case _ => throwInvalidTypeException(DataType.String, rawValue)
  }

  def asLocation(rawValue: Any): Location = rawValue match {
    case s: String => parseLocation(s)
    case l: Location => l
    case _ => throwInvalidTypeException(DataType.Location, rawValue)
  }

  def asTimestamp(rawValue: Any): Instant = rawValue match {
    case s: String => parseTimestamp(s)
    case i: Instant => i
    case n: Number => new Instant(n.longValue)
    case l: Long => new Instant(l)
    case _ => throwInvalidTypeException(DataType.Timestamp, rawValue)
  }

  def asDistance(rawValue: Any): Distance = rawValue match {
    case s: String => parseDistance(s)
    case d: Distance => d
    case _ => throwInvalidTypeException(DataType.Distance, rawValue)
  }

  def asDuration(rawValue: Any): JodaDuration = rawValue match {
    case s: String => parseDuration(s)
    case d: JodaDuration => d
    case n: Number => new JodaDuration(n.longValue)
    case l: Long => new JodaDuration(l)
    case _ => throwInvalidTypeException(DataType.Duration, rawValue)
  }

  def asImage(rawValue: Any): Image = rawValue match {
    case i: Image => i
    case _ => throwInvalidTypeException(DataType.Image, rawValue)
  }

  def asDataset(rawValue: Any): Dataset = rawValue match {
    case s: String => parseDataset(s)
    case d: Dataset => d
    case _ => throwInvalidTypeException(DataType.Dataset, rawValue)
  }

  def asList(rawValue: Any, of: DataType): Seq[Any] = rawValue match {
    case arr: Array[_] => arr.map(as(_, of)).toSeq
    case seq: Seq[_] => seq.map(as(_, of))
    case _ => throwInvalidTypeException(DataType.List(of), rawValue)
  }

  def asSet(rawValue: Any, of: DataType): Set[Any] = rawValue match {
    case arr: Array[_] => arr.map(as(_, of)).toSet
    case set: Set[_] => set.map(as(_, of))
    case _ => throwInvalidTypeException(DataType.Set(of), rawValue)
  }

  def asMap(rawValue: Any, ofKeys: DataType, ofValues: DataType): Map[Any, Any] = rawValue match {
    case map: Map[_, _] => map.map { case (k, v) => as(k, ofKeys) -> as(v, ofValues) }
    case _ => throwInvalidTypeException(DataType.Map(ofKeys, ofValues), rawValue)
  }

  /**
   * Parse a string into a given data type.
   *
   * @param kind Data type.
   * @param str  String to parse.
   */
  def parse(str: String, kind: DataType): Any = kind match {
    case DataType.Boolean => parseBoolean(str)
    case DataType.Byte => parseByte(str)
    case DataType.Short => parseShort(str)
    case DataType.Integer => parseInteger(str)
    case DataType.Long => parseLong(str)
    case DataType.Double => parseDouble(str)
    case DataType.String => parseString(str)
    case DataType.Location => parseLocation(str)
    case DataType.Timestamp => parseTimestamp(str)
    case DataType.Duration => parseDuration(str)
    case DataType.Distance => parseDistance(str)
    case DataType.Dataset => parseDataset(str)
    case _ => throw new IllegalArgumentException(s"Cannot parse a $kind param")
  }

  /**
   * Parse a string into a byte.
   *
   * @param str String to parse.
   */
  def parseByte(str: String): Byte = str.toByte

  /**
   * Parse a string into a short.
   *
   * @param str String to parse.
   */
  def parseShort(str: String): Short = str.toShort

  /**
   * Parse a string into an integer.
   *
   * @param str String to parse.
   */
  def parseInteger(str: String): Int = str.toInt

  /**
   * Parse a string into a long.
   *
   * @param str String to parse.
   */
  def parseLong(str: String): Long = str.toLong

  /**
   * Parse a string into a double.
   *
   * @param str String to parse.
   */
  def parseDouble(str: String): Double = str.toDouble

  /**
   * Parse a string into a boolean.
   *
   * @param str String to parse.
   */
  def parseBoolean(str: String): Boolean = str match {
    case "true" => true
    case "t" => true
    case "yes" => true
    case "y" => true
    case "false" => false
    case "f" => false
    case "no" => false
    case "n" => false
    case _ => throw new IllegalArgumentException(s"Invalid boolean: $str")
  }

  def parseString(str: String): String = str

  def parseLocation(str: String): Location = LatLng.parse(str)

  def parseTimestamp(str: String): Instant = Instant.parse(str)

  def parseDuration(str: String): JodaDuration = new JodaDuration(TwitterDuration.parse(str).inMillis)

  def parseDistance(str: String): Distance = Distance.parse(str)

  def parseDataset(str: String): Dataset = Dataset.parse(str)

  def ordering(kind: DataType): Option[Ordering[_]] = kind match {
    case DataType.Boolean => Some(Ordering[Boolean])
    case DataType.Byte => Some(Ordering[Byte])
    case DataType.Short => Some(Ordering[Short])
    case DataType.Integer => Some(Ordering[Int])
    case DataType.Long => Some(Ordering[Long])
    case DataType.Double => Some(Ordering[Double])
    case DataType.String => Some(Ordering[String])
    case DataType.Timestamp => Some(instantOrdering)
    case DataType.Duration => Some(durationOrdering)
    case DataType.Distance => Some(Ordering[Distance])
    case _ => None
  }

  /**
   * Expand an exploration into a list of values to explore, w.r.t. a given parameter type.
   *
   * @param explo Exploration definition
   * @param kind  Parameter type
   * @throws IllegalArgumentException If it cannot be expanded w.r.t. the given parameter type
   * @return List of values to explore
   */
  @throws[IllegalArgumentException]
  def expand(explo: Exploration, kind: DataType): Seq[Any] = explo match {
    case SingletonExploration(value) => Seq(as(value, kind))
    case ListExploration(values) => values.map(as(_, kind)).toSeq
    case RangeExploration(from, to, step, log, log2, log10) =>
      kind match {
        case DataType.Byte => asByte(from) to asByte(to) by asByte(step)
        case DataType.Short => asShort(from) to asShort(to) by asShort(step)
        case DataType.Integer => asInteger(from) to asInteger(to) by asInteger(step)
        case DataType.Long => asLong(from) to asLong(to) by asLong(step, kind)
        case DataType.Double => asDouble(from) to asDouble(to) by asDouble(step)
        case DataType.Distance =>
          val dFrom = asDistance(from).meters
          val dTo = asDistance(to).meters
          val dStep = asDistance(step).meters
          (dFrom to dTo by dStep).map(Distance.meters)
        case DataType.Duration =>
          val dFrom = asDuration(from).getMillis
          val dTo = asDuration(to).getMillis
          val dStep = asDuration(step).getMillis
          (dFrom to dTo by dStep).map(new JodaDuration(_))
        case DataType.Timestamp =>
          val tFrom = asTimestamp(from).getMillis
          val tTo = asTimestamp(to).getMillis
          val dStep = asDuration(step).getMillis
          (tFrom to tTo by dStep).map(new Instant(_))
        case _ => throw new IllegalArgumentException(s"Cannot generate a range of $kind")
      }
  }

  private def throwInvalidTypeException(kind: DataType, rawValue: Any) =
    throw new IllegalArgumentException(s"Invalid $kind value: ${rawValue.getClass.getName}")
}
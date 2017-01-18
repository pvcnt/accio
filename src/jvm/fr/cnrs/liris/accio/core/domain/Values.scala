/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.core.domain

import com.twitter.util.{Duration => TwitterDuration}
import fr.cnrs.liris.accio.core.api.Dataset
import fr.cnrs.liris.common.geo.{Distance, LatLng, Location}
import org.joda.time.{Instant, Duration => JodaDuration}

import scala.collection.JavaConverters._

/**
 * Factory and converters for [[Value]].
 */
object Values {
  val instantOrdering = new Ordering[Instant] {
    override def compare(x: Instant, y: Instant): Int = x.compareTo(y)
  }

  val durationOrdering = new Ordering[JodaDuration] {
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
  def as(rawValue: Any, kind: DataType): Any = kind.base match {
    case AtomicType.Boolean => asBoolean(rawValue)
    case AtomicType.Byte => asByte(rawValue)
    case AtomicType.Integer => asInteger(rawValue)
    case AtomicType.Long => asLong(rawValue)
    case AtomicType.Double => asDouble(rawValue)
    case AtomicType.String => asString(rawValue)
    case AtomicType.Location => asLocation(rawValue)
    case AtomicType.Timestamp => asTimestamp(rawValue)
    case AtomicType.Duration => asDuration(rawValue)
    case AtomicType.Distance => asDistance(rawValue)
    case AtomicType.Dataset => asDataset(rawValue)
    case AtomicType.List => asList(rawValue, kind.args.head)
    case AtomicType.Set => asSet(rawValue, kind.args.head)
    case AtomicType.Map => asMap(rawValue, kind.args.head, kind.args.last)
    case AtomicType.EnumUnknownAtomicType(_) => throw new IllegalArgumentException(s"Unknown type: $kind")
  }

  def asByte(rawValue: Any): Byte = rawValue match {
    case s: String => parseByte(s)
    case n: java.lang.Number => n.byteValue
    case b: Byte => b
    case _ => throwInvalidTypeException(DataType(AtomicType.Byte), rawValue)
  }

  def asInteger(rawValue: Any): Int = rawValue match {
    case s: String => parseInteger(s)
    case n: java.lang.Number => n.intValue
    case b: Byte => b
    case s: Short => s
    case i: Int => i
    case _ => throwInvalidTypeException(DataType(AtomicType.Integer), rawValue)
  }

  def asLong(rawValue: Any): Long = rawValue match {
    case s: String => parseLong(s)
    case n: java.lang.Number => n.longValue
    case b: Byte => b
    case s: Short => s
    case i: Int => i
    case l: Long => l
    case _ => throwInvalidTypeException(DataType(AtomicType.Long), rawValue)
  }

  def asDouble(rawValue: Any): Double = rawValue match {
    case s: String => parseDouble(s)
    case n: java.lang.Number => n.doubleValue
    case b: Byte => b
    case s: Short => s
    case i: Int => i
    case l: Long => l
    case d: Double => d
    case _ => throwInvalidTypeException(DataType(AtomicType.Double), rawValue)
  }

  def asBoolean(rawValue: Any): Boolean = rawValue match {
    case s: String => parseBoolean(s)
    case b: java.lang.Boolean => b.asInstanceOf[Boolean]
    case b: Boolean => b
    case _ => throwInvalidTypeException(DataType(AtomicType.Boolean), rawValue)
  }

  def asString(rawValue: Any): String = rawValue match {
    case s: String => s
    case _ => throwInvalidTypeException(DataType(AtomicType.String), rawValue)
  }

  def asLocation(rawValue: Any): Location = rawValue match {
    case s: String => parseLocation(s)
    case l: Location => l
    case _ => throwInvalidTypeException(DataType(AtomicType.Location), rawValue)
  }

  def asTimestamp(rawValue: Any): Instant = rawValue match {
    case s: String => parseTimestamp(s)
    case i: Instant => i
    case n: Number => new Instant(n.longValue)
    case l: Long => new Instant(l)
    case _ => throwInvalidTypeException(DataType(AtomicType.Timestamp), rawValue)
  }

  def asDistance(rawValue: Any): Distance = rawValue match {
    case s: String => parseDistance(s)
    case d: Distance => d
    case _ => throwInvalidTypeException(DataType(AtomicType.Distance), rawValue)
  }

  def asDuration(rawValue: Any): JodaDuration = rawValue match {
    case s: String => parseDuration(s)
    case d: JodaDuration => d
    case n: Number => new JodaDuration(n.longValue)
    case l: Long => new JodaDuration(l)
    case _ => throwInvalidTypeException(DataType(AtomicType.Duration), rawValue)
  }

  def asDataset(rawValue: Any): Dataset = rawValue match {
    case s: String => parseDataset(s)
    case d: Dataset => d
    case _ => throwInvalidTypeException(DataType(AtomicType.Dataset), rawValue)
  }

  def asList(rawValue: Any, of: AtomicType): Seq[Any] = rawValue match {
    case arr: Array[_] => arr.map(as(_, DataType(of))).toSeq
    case seq: Seq[_] => seq.map(as(_, DataType(of)))
    case list: java.util.List[_] => list.asScala.map(as(_, DataType(of)))
    case _ => throwInvalidTypeException(DataType(AtomicType.List, Seq(of)), rawValue)
  }

  def asSet(rawValue: Any, of: AtomicType): Set[Any] = rawValue match {
    case arr: Array[_] => arr.map(as(_, DataType(of))).toSet
    case set: Set[_] => set.map(as(_, DataType(of)))
    case set: java.util.Set[_] => set.asScala.toSet[Any].map(as(_, DataType(of)))
    case _ => throwInvalidTypeException(DataType(AtomicType.Set, Seq(of)), rawValue)
  }

  def asMap(rawValue: Any, ofKeys: AtomicType, ofValues: AtomicType): Map[Any, Any] = rawValue match {
    case map: Map[_, _] =>
      map.map { case (k, v) =>
        as(k, DataType(ofKeys)) -> as(v, DataType(ofValues))
      }
    case map: java.util.Map[_, _] =>
      map.asScala.toMap.map { case (k, v) =>
        as(k, DataType(ofKeys)) -> as(v, DataType(ofValues))
      }
    case _ => throwInvalidTypeException(DataType(AtomicType.Map, Seq(ofKeys, ofValues)), rawValue)
  }

  def encode(v: Any, kind: DataType): Value =
    kind.base match {
      case AtomicType.Byte => encodeByte(asByte(v))
      case AtomicType.Integer => encodeInteger(asInteger(v))
      case AtomicType.Long => encodeLong(asLong(v))
      case AtomicType.Double => encodeDouble(asDouble(v))
      case AtomicType.Boolean => encodeBoolean(asBoolean(v))
      case AtomicType.String => encodeString(asString(v))
      case AtomicType.Location => encodeLocation(asLocation(v))
      case AtomicType.Timestamp => encodeTimestamp(asTimestamp(v))
      case AtomicType.Duration => encodeDuration(asDuration(v))
      case AtomicType.Distance => encodeDistance(asDistance(v))
      case AtomicType.Dataset => encodeDataset(asDataset(v))
      case AtomicType.Map => encodeMap(asMap(v, kind.args.head, kind.args.last), kind)
      case AtomicType.List => encodeList(asList(v, kind.args.head), kind)
      case AtomicType.Set => encodeSet(asSet(v, kind.args.head), kind)
      case AtomicType.EnumUnknownAtomicType(_) => throw new IllegalArgumentException(s"Unknown type: $kind")
    }

  def encodeByte(v: Byte): Value = Value(bytes = Seq(v))

  def encodeInteger(v: Int): Value = Value(integers = Seq(v))

  def encodeLong(v: Long): Value = Value(longs = Seq(v))

  def encodeDouble(v: Double): Value = Value(doubles = Seq(v))

  def encodeString(v: String): Value = Value(strings = Seq(v))

  def encodeBoolean(v: Boolean): Value = Value(booleans = Seq(v))

  def encodeLocation(v: Location): Value = {
    val latLng = v.asInstanceOf[Location].toLatLng
    Value(doubles = Seq(latLng.lat.degrees, latLng.lng.degrees))
  }

  def encodeTimestamp(v: Instant): Value = Value(longs = Seq(v.getMillis))

  def encodeDuration(v: JodaDuration): Value = Value(longs = Seq(v.getMillis))

  def encodeDistance(v: Distance): Value = Value(doubles = Seq(v.meters))

  def encodeDataset(v: Dataset): Value = Value(strings = Seq(v.uri))

  def encodeList(v: Seq[_], kind: DataType): Value = {
    require(kind.base == AtomicType.List, s"${kind.base} is not a List")
    encodeList(v, kind.args.head)
  }

  def encodeList(v: Seq[_], of: AtomicType): Value = {
    val values = v.map(encode(_, DataType(of)))
    merge(values.size, values)
  }

  def encodeSet(v: Set[_], kind: DataType): Value = {
    require(kind.base == AtomicType.Set, s"${kind.base} is not a Set")
    encodeSet(v, kind.args.head)
  }

  def encodeSet(v: Set[_], of: AtomicType): Value = {
    val values = v.toSeq.map(encode(_, DataType(of)))
    merge(values.size, values)
  }

  def encodeMap(v: Map[_, _], kind: DataType): Value = {
    require(kind.base == AtomicType.Map, s"${kind.base} is not a Map")
    encodeMap(v, kind.args.head, kind.args.last)
  }

  def encodeMap(v: Map[_, _], ofKeys: AtomicType, ofValues: AtomicType): Value = {
    val keys = v.keys.toSeq.map(encode(_, DataType(ofKeys)))
    val values = v.values.toSeq.map(encode(_, DataType(ofValues)))
    merge(keys.size, keys ++ values)
  }

  def toString(value: Value, kind: DataType): String = decode(value, kind).toString

  def decode(value: Value, kind: DataType): Any =
    kind.base match {
      case AtomicType.Byte => decodeByte(value)
      case AtomicType.Integer => decodeInteger(value)
      case AtomicType.Long => decodeLong(value)
      case AtomicType.Double => decodeDouble(value)
      case AtomicType.String => decodeString(value)
      case AtomicType.Boolean => decodeBoolean(value)
      case AtomicType.Location => decodeLocation(value)
      case AtomicType.Timestamp => decodeTimestamp(value)
      case AtomicType.Duration => decodeDuration(value)
      case AtomicType.Distance => decodeDistance(value)
      case AtomicType.Dataset => decodeDataset(value)
      case AtomicType.List => decodeList(value, kind.args.head)
      case AtomicType.Set => decodeSet(value, kind.args.head)
      case AtomicType.Map => decodeMap(value, kind.args.head, kind.args.last)
      case AtomicType.EnumUnknownAtomicType(_) => throw new IllegalArgumentException(s"Unknown type: $kind")
    }

  def decodeByte(value: Value): Byte = value.bytes.head

  def decodeInteger(value: Value): Int = value.integers.head

  def decodeLong(value: Value): Long = value.longs.head

  def decodeDouble(value: Value): Double = value.doubles.head

  def decodeBoolean(value: Value): Boolean = value.booleans.head

  def decodeString(value: Value): String = value.strings.head

  def decodeLocation(value: Value): LatLng = LatLng.degrees(value.doubles.head, value.doubles.last)

  def decodeTimestamp(value: Value): Instant = new Instant(value.longs.head)

  def decodeDataset(value: Value): Dataset = Dataset(value.strings.head)

  def decodeList(value: Value, of: AtomicType): Seq[Any] = split(value).map(decode(_, DataType(of)))

  def decodeSet(value: Value, of: AtomicType): Set[Any] = split(value).map(decode(_, DataType(of))).toSet

  def decodeMap(value: Value, kind: DataType): Map[Any, Any] = {
    require(kind.base == AtomicType.Map, s"${kind.base} is not a Map")
    decodeMap(value, kind.args.head, kind.args.last)
  }

  def decodeMap(value: Value, ofKeys: AtomicType, ofValues: AtomicType): Map[Any, Any] = {
    val keysValues = split(value.copy(size = 2))
    val keys = split(keysValues.head.copy(size = value.size)).map(decode(_, DataType(ofKeys)))
    val values = split(keysValues.last.copy(size = value.size)).map(decode(_, DataType(ofValues)))
    Map(keys.zip(values): _*)
  }

  def decodeDistance(value: Value): Distance = Distance.meters(value.doubles.head)

  def decodeDuration(value: Value): JodaDuration = new JodaDuration(value.longs.head)

  /**
   * Parse a string into a given data type.
   *
   * @param str  String to parse.
   * @param kind Data type.
   */
  def parse(str: String, kind: DataType): Any =
    kind.base match {
      case AtomicType.Boolean => parseBoolean(str)
      case AtomicType.Byte => parseByte(str)
      case AtomicType.Integer => parseInteger(str)
      case AtomicType.Long => parseLong(str)
      case AtomicType.Double => parseDouble(str)
      case AtomicType.String => parseString(str)
      case AtomicType.Location => parseLocation(str)
      case AtomicType.Timestamp => parseTimestamp(str)
      case AtomicType.Duration => parseDuration(str)
      case AtomicType.Distance => parseDistance(str)
      case AtomicType.Dataset => parseDataset(str)
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

  def getAtomicType(clazz: Class[_]): AtomicType = {
    clazz match {
      case c if c == classOf[Boolean] || c == classOf[java.lang.Boolean] => AtomicType.Boolean
      case c if c == classOf[Byte] || c == classOf[java.lang.Byte] => AtomicType.Byte
      case c if c == classOf[Int] || c == classOf[java.lang.Integer] => AtomicType.Integer
      case c if c == classOf[Long] || c == classOf[java.lang.Long] => AtomicType.Long
      case c if c == classOf[Double] || c == classOf[java.lang.Double] => AtomicType.Double
      case c if c == classOf[String] => AtomicType.String
      case c if classOf[Location].isAssignableFrom(c) => AtomicType.Location
      case c if c == classOf[Instant] => AtomicType.Timestamp
      case c if c == classOf[JodaDuration] => AtomicType.Duration
      case c if c == classOf[Distance] => AtomicType.Distance
      case c if c == classOf[Dataset] => AtomicType.Dataset
      case c if classOf[Seq[_]].isAssignableFrom(c) => AtomicType.List
      case c if classOf[Set[_]].isAssignableFrom(c) => AtomicType.Set
      case c if classOf[Map[_, _]].isAssignableFrom(c) => AtomicType.Map
      case _ => throw new IllegalArgumentException(s"Unsupported data type: ${clazz.getName}")
    }
  }

  private def merge(size: Int, values: Seq[Value]): Value = {
    Value(
      bytes = values.flatMap(_.bytes),
      integers = values.flatMap(_.integers),
      longs = values.flatMap(_.longs),
      doubles = values.flatMap(_.doubles),
      booleans = values.flatMap(_.booleans),
      strings = values.flatMap(_.strings),
      size = size)
  }

  private def split(value: Value): Seq[Value] = {
    def slice[T](i: Int, seq: Seq[T]): Seq[T] = {
      if (seq.isEmpty) {
        seq
      } else {
        val n = seq.size / value.size
        seq.slice(i * n, (i + 1) * n)
      }
    }

    Seq.tabulate(value.size) { i =>
      Value(
        bytes = slice(i, value.bytes),
        integers = slice(i, value.integers),
        longs = slice(i, value.longs),
        doubles = slice(i, value.doubles),
        booleans = slice(i, value.booleans),
        strings = slice(i, value.strings))
    }
  }

  private def throwInvalidTypeException(kind: DataType, rawValue: Any) =
    throw new IllegalArgumentException(s"${rawValue.getClass.getName} is not a $kind")
}

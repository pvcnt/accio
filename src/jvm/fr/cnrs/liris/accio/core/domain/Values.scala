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

package fr.cnrs.liris.accio.core.domain

import com.twitter.util.{Duration => TwitterDuration}
import fr.cnrs.liris.accio.core.api.Dataset
import fr.cnrs.liris.common.geo.{Distance, LatLng, Location}
import org.joda.time.{Instant, Duration => JodaDuration}

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

  def encode(v: Any, kind: DataType): Value = {
    kind.base match {
      case AtomicType.Byte => encodeByte(v.asInstanceOf[java.lang.Number].byteValue)
      case AtomicType.Integer => encodeInteger(v.asInstanceOf[java.lang.Number].intValue)
      case AtomicType.Long => encodeLong(v.asInstanceOf[java.lang.Number].longValue)
      case AtomicType.Double => encodeDouble(v.asInstanceOf[java.lang.Number].doubleValue)
      case AtomicType.String => encodeString(v.toString)
      case AtomicType.Boolean => encodeBoolean(v.asInstanceOf[Boolean])
      case AtomicType.Location => encodeLocation(v.asInstanceOf[Location])
      case AtomicType.Timestamp => encodeTimestamp(v.asInstanceOf[Instant])
      case AtomicType.Duration => encodeDuration(v.asInstanceOf[JodaDuration])
      case AtomicType.Distance => encodeDistance(v.asInstanceOf[Distance])
      case AtomicType.Dataset => encodeDataset(v.asInstanceOf[Dataset])
      case AtomicType.Map =>
        val keys = v.asInstanceOf[Map[Any, Any]].keys.toSeq.map(encode(_, DataType(kind.args.head)))
        val values = v.asInstanceOf[Map[Any, Any]].values.toSeq.map(encode(_, DataType(kind.args.last)))
        merge(keys.size, keys ++ values)
      case AtomicType.List =>
        val values = v.asInstanceOf[Seq[Any]].map(encode(_, DataType(kind.args.head)))
        merge(values.size, values)
      case AtomicType.Set =>
        val values = v.asInstanceOf[Set[Any]].toSeq.map(encode(_, DataType(kind.args.head)))
        merge(values.size, values)
      case AtomicType.EnumUnknownAtomicType(_) => throw new IllegalArgumentException("Unknown type")
    }
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

  def encodeMap(v: Map[Any, Any], kind: DataType): Value = {
    require(kind.base == AtomicType.Map, s"${kind.base} is not a Map")
    encodeMap(v, kind.args.head, kind.args.last)
  }

  def encodeMap(v: Map[Any, Any], ofKeys: AtomicType, ofValues: AtomicType): Value = {
    val keys = v.keys.toSeq.map(encode(_, DataType(ofKeys)))
    val values = v.values.toSeq.map(encode(_, DataType(ofValues)))
    merge(keys.size, keys ++ values)
  }

  def toString(value: Value, kind: DataType): String = decode(value, kind).toString

  def decode(value: Value, kind: DataType): Any = {
    kind.base match {
      case AtomicType.Byte => decodeByte(value)
      case AtomicType.Integer => decodeInteger(value)
      case AtomicType.Long => decodeLong(value)
      case AtomicType.Double => decodeDouble(value)
      case AtomicType.String => decodeString(value)
      case AtomicType.Boolean => value.booleans.head
      case AtomicType.Location => LatLng.degrees(value.doubles.head, value.doubles.last)
      case AtomicType.Timestamp => new Instant(value.longs.head)
      case AtomicType.Duration => decodeDuration(value)
      case AtomicType.Distance => decodeDistance(value)
      case AtomicType.Dataset => Dataset(value.strings.head)
      case AtomicType.List => decodeList(value, kind.args.head)
      case AtomicType.Set => decodeSet(value, kind.args.head)
      case AtomicType.Map => decodeMap(value, kind.args.head, kind.args.last)
      case AtomicType.EnumUnknownAtomicType(_) => throw new IllegalArgumentException("Unknown type")
    }
  }

  def decodeByte(value: Value): Byte = value.bytes.head

  def decodeInteger(value: Value): Int = value.integers.head

  def decodeLong(value: Value): Long = value.longs.head

  def decodeDouble(value: Value): Double = value.doubles.head

  def decodeString(value: Value): String = value.strings.head

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
}

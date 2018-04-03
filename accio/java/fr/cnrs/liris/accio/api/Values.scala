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

package fr.cnrs.liris.accio.api

import com.twitter.util.{Try, Duration => TwitterDuration}
import fr.cnrs.liris.accio.api.thrift.{AtomicType, DataType, Value}
import fr.cnrs.liris.accio.sdk.Dataset
import fr.cnrs.liris.common.geo.{Distance, LatLng, Location}
import org.joda.time.{Instant, Duration => JodaDuration}

import scala.collection.JavaConverters._

/**
 * Utils to deal with [[Value]]s.
 */
object Values {
  private val AbbreviateThreshold = 3

  implicit val instantOrdering: Ordering[Instant] = new Ordering[Instant] {
    override def compare(x: Instant, y: Instant): Int = x.compareTo(y)
  }

  implicit val durationOrdering: Ordering[JodaDuration] = new Ordering[JodaDuration] {
    override def compare(x: JodaDuration, y: JodaDuration): Int = x.compareTo(y)
  }

  /**
   * Correct a value into another data type.
   *
   * @param value Value.
   * @param kind  Target data type.
   * @return Corrected value, if the two data types were compatible.
   */
  def as(value: Value, kind: DataType): Option[Value] = encode(decode(value), kind)

  def encode(v: Any, kind: DataType): Option[Value] =
    kind.base match {
      case AtomicType.Byte => encodeByte(v)
      case AtomicType.Integer => encodeInteger(v)
      case AtomicType.Long => encodeLong(v)
      case AtomicType.Double => encodeDouble(v)
      case AtomicType.Boolean => encodeBoolean(v)
      case AtomicType.String => encodeString(v)
      case AtomicType.Location => encodeLocation(v)
      case AtomicType.Timestamp => encodeTimestamp(v)
      case AtomicType.Duration => encodeDuration(v)
      case AtomicType.Distance => encodeDistance(v)
      case AtomicType.Dataset => encodeDataset(v)
      case AtomicType.Map => encodeMap(v, kind.args.head, kind.args.last)
      case AtomicType.List => encodeList(v, kind.args.head)
      case AtomicType.Set => encodeSet(v, kind.args.head)
      case AtomicType.EnumUnknownAtomicType(_) => throw new IllegalArgumentException(s"Unknown type: $kind")
    }

  def encodeByte(v: Byte): Value = Value(DataType(AtomicType.Byte), bytes = Seq(v))

  def encodeByte(rawValue: Any): Option[Value] =
    rawValue match {
      case s: String => parseByte(s)
      case n: Number => Some(encodeByte(n.byteValue))
      case b: Byte => Some(encodeByte(b))
      case _ => None
    }

  def encodeInteger(v: Int): Value = Value(DataType(AtomicType.Integer), integers = Seq(v))

  def encodeInteger(rawValue: Any): Option[Value] = rawValue match {
    case s: String => parseInteger(s)
    case n: Number => Some(encodeInteger(n.intValue))
    case b: Byte => Some(encodeInteger(b))
    case s: Short => Some(encodeInteger(s))
    case i: Int => Some(encodeInteger(i))
    case _ => None
  }

  def encodeLong(v: Long): Value = Value(DataType(AtomicType.Long), longs = Seq(v))

  def encodeLong(rawValue: Any): Option[Value] =
    rawValue match {
      case s: String => parseLong(s)
      case n: Number => Some(encodeLong(n.longValue))
      case b: Byte => Some(encodeLong(b))
      case s: Short => Some(encodeLong(s))
      case i: Int => Some(encodeLong(i))
      case l: Long => Some(encodeLong(l))
      case _ => None
    }

  def encodeDouble(v: Double): Value = Value(DataType(AtomicType.Double), doubles = Seq(v))

  def encodeDouble(rawValue: Any): Option[Value] =
    rawValue match {
      case s: String => parseDouble(s)
      case n: Number => Some(encodeDouble(n.doubleValue))
      case b: Byte => Some(encodeDouble(b))
      case s: Short => Some(encodeDouble(s))
      case i: Int => Some(encodeDouble(i))
      case l: Long => Some(encodeDouble(l))
      case d: Double => Some(encodeDouble(d))
      case _ => None
    }

  def encodeString(v: String): Value = Value(DataType(AtomicType.String), strings = Seq(v))

  def encodeString(rawValue: Any): Option[Value] =
    rawValue match {
      case s: String => Some(encodeString(s))
      case o => Some(encodeString(o.toString))
    }

  def encodeBoolean(v: Boolean): Value = Value(DataType(AtomicType.Boolean), booleans = Seq(v))

  def encodeBoolean(rawValue: Any): Option[Value] =
    rawValue match {
      case s: String => parseBoolean(s)
      case b: java.lang.Boolean => Some(encodeBoolean(b))
      case b: Boolean => Some(encodeBoolean(b))
      case _ => None
    }

  def encodeLocation(v: Location): Value = {
    val latLng = v.asInstanceOf[Location].toLatLng
    Value(DataType(AtomicType.Location), doubles = Seq(latLng.lat.radians, latLng.lng.radians))
  }

  def encodeLocation(rawValue: Any): Option[Value] =
    rawValue match {
      case s: String => parseLocation(s)
      case l: Location => Some(encodeLocation(l))
      case Seq(x, y) => (x, y) match {
        case (lng: Double, lat: Double) => Some(encodeLocation(LatLng.degrees(lat, lng)))
        case _ => None
      }
      case Array(x, y) => (x, y) match {
        case (lng: Double, lat: Double) => Some(encodeLocation(LatLng.degrees(lat, lng)))
        case _ => None
      }
      case _ => None
    }

  def encodeTimestamp(v: Instant): Value = Value(DataType(AtomicType.Timestamp), longs = Seq(v.getMillis))

  def encodeTimestamp(rawValue: Any): Option[Value] =
    rawValue match {
      case s: String => parseTimestamp(s)
      case i: Instant => Some(encodeTimestamp(i))
      case n: Number => Some(encodeTimestamp(new Instant(n.longValue)))
      case b: Byte => Some(encodeTimestamp(new Instant(b)))
      case s: Short => Some(encodeTimestamp(new Instant(s)))
      case i: Int => Some(encodeTimestamp(new Instant(i)))
      case l: Long => Some(encodeTimestamp(new Instant(l)))
      case _ => None
    }

  def encodeDuration(v: JodaDuration): Value = Value(DataType(AtomicType.Duration), longs = Seq(v.getMillis))

  def encodeDuration(rawValue: Any): Option[Value] = rawValue match {
    case s: String => parseDuration(s)
    case d: JodaDuration => Some(encodeDuration(d))
    case n: Number => Some(encodeDuration(new JodaDuration(n.longValue)))
    case b: Byte => Some(encodeDuration(new JodaDuration(b)))
    case s: Short => Some(encodeDuration(new JodaDuration(s)))
    case i: Int => Some(encodeDuration(new JodaDuration(i)))
    case l: Long => Some(encodeDuration(new JodaDuration(l)))
    case _ => None
  }

  def encodeDistance(v: Distance): Value = Value(DataType(AtomicType.Distance), doubles = Seq(v.meters))

  def encodeDistance(rawValue: Any): Option[Value] =
    rawValue match {
      case s: String => parseDistance(s)
      case d: Distance => Some(encodeDistance(d))
      case _ => None
    }

  def encodeDataset(v: Dataset): Value = Value(DataType(AtomicType.Dataset), strings = Seq(v.uri))

  def encodeDataset(rawValue: Any): Option[Value] =
    rawValue match {
      case s: String => parseDataset(s)
      case d: Dataset => Some(encodeDataset(d))
      case _ => None
    }

  def encodeList(v: Seq[_], kind: DataType): Option[Value] = {
    require(kind.base == AtomicType.List, s"${kind.base} is not a List")
    encodeList(v, kind.args.head)
  }

  def emptyList: Value = encodeList(Seq.empty, AtomicType.String).get

  def encodeList(v: Seq[_], of: AtomicType): Option[Value] = {
    val values = v.flatMap(encode(_, DataType(of)))
    if (values.size == v.size) {
      Some(merge(values.size, values, DataType(AtomicType.List, Seq(of))))
    } else {
      None
    }
  }

  def encodeList(rawValue: Any, of: AtomicType): Option[Value] = rawValue match {
    case arr: Array[_] => encodeList(arr.toSeq, of)
    case seq: Seq[_] => encodeList(seq, of)
    case list: java.util.List[_] => encodeList(list.asScala, of)
    case set: Set[_] => encodeList(set.toSeq, of)
    case set: java.util.Set[_] => encodeList(set.asScala.toSeq, of)
    case _ => None
  }

  def encodeSet(v: Set[_], kind: DataType): Option[Value] = {
    require(kind.base == AtomicType.Set, s"${kind.base} is not a Set")
    encodeSet(v, kind.args.head)
  }

  def encodeSet(v: Set[_], of: AtomicType): Option[Value] = {
    val values = v.toSeq.flatMap(encode(_, DataType(of)))
    if (values.size == v.size) {
      Some(merge(values.size, values, DataType(AtomicType.Set, Seq(of))))
    } else {
      None
    }
  }

  def encodeSet(rawValue: Any, of: AtomicType): Option[Value] =
    rawValue match {
      case arr: Array[_] => encodeSet(arr.toSet, of)
      case set: Set[_] => encodeSet(set, of)
      case set: java.util.Set[_] => encodeSet(set.asScala.toSet, of)
      case seq: Seq[_] => encodeSet(seq.toSet, of)
      case list: java.util.List[_] => encodeSet(list.asScala.toSet, of)
      case _ => None
    }

  def emptyMap: Value = encodeMap(Map.empty, AtomicType.String, AtomicType.String).get

  def encodeMap(v: Map[_, _], kind: DataType): Option[Value] = {
    require(kind.base == AtomicType.Map, s"${kind.base} is not a Map")
    encodeMap(v, kind.args.head, kind.args.last)
  }

  def encodeMap(v: Map[_, _], ofKeys: AtomicType, ofValues: AtomicType): Option[Value] = {
    val keys = v.keys.toSeq.flatMap(encode(_, DataType(ofKeys)))
    val values = v.values.toSeq.flatMap(encode(_, DataType(ofValues)))
    if (keys.size == v.size && values.size == v.size) {
      Some(merge(keys.size, keys ++ values, DataType(AtomicType.Map, Seq(ofKeys, ofValues))))
    } else {
      None
    }
  }

  def encodeMap(rawValue: Any, ofKeys: AtomicType, ofValues: AtomicType): Option[Value] =
    rawValue match {
      case map: Map[_, _] => encodeMap(map, ofKeys, ofValues)
      case map: java.util.Map[_, _] => encodeMap(map.asScala.toMap, ofKeys, ofValues)
      case _ => None
    }

  def stringify(value: Value, abbreviate: Boolean = true): String =
    value.kind.base match {
      case AtomicType.List => stringify(decodeList(value), abbreviate)
      case AtomicType.Set => stringify(decodeSet(value).toSeq, abbreviate)
      case AtomicType.Map =>
        val seq = decodeMap(value).toSeq
          .map {
            case (k, v) => s"${stringify(k)}=${stringify(v)}"
            case s => s // For the last <xx more>
          }
        stringify(seq, abbreviate)
      case AtomicType.Dataset => decodeDataset(value).uri
      case AtomicType.Duration => TwitterDuration.fromMilliseconds(decodeDuration(value).getMillis).toString
      case _ => stringify(decode(value))
    }

  private def stringify(seq: Seq[_], abbreviate: Boolean): String = {
    val abbreviated = if (seq.size > AbbreviateThreshold) {
      seq.take(AbbreviateThreshold - 1) ++ Seq(s"<${seq.size - AbbreviateThreshold + 1} more>")
    } else seq
    abbreviated.map(stringify).mkString(", ")
  }

  private def stringify(obj: Any): String =
    obj match {
      case d: Double => roundAt6(d).toString
      case d: Distance => Distance.meters(roundAt2(d.meters)).toString
      case _ => obj.toString
    }

  private def roundAt2(d: Double) = (d * 100).round / 100d

  private def roundAt6(d: Double) = (d * 1000000).round / 1000000d

  def decode(value: Value, kind: DataType): Any = decode(checkType(kind, value))

  def decode(value: Value): Any =
    value.kind.base match {
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
      case AtomicType.List => decodeList(value)
      case AtomicType.Set => decodeSet(value)
      case AtomicType.Map => decodeMap(value)
      case AtomicType.EnumUnknownAtomicType(_) => throw new IllegalArgumentException(s"Unknown type: ${stringify(value.kind)}")
    }

  def decodeByte(value: Value): Byte = checkType(AtomicType.Byte, value).bytes.head

  def decodeInteger(value: Value): Int = checkType(AtomicType.Integer, value).integers.head

  def decodeLong(value: Value): Long = checkType(AtomicType.Long, value).longs.head

  def decodeDouble(value: Value): Double = checkType(AtomicType.Double, value).doubles.head

  def decodeBoolean(value: Value): Boolean = checkType(AtomicType.Boolean, value).booleans.head

  def decodeString(value: Value): String = checkType(AtomicType.String, value).strings.head

  def decodeLocation(value: Value): LatLng = {
    checkType(AtomicType.Location, value)
    LatLng.radians(value.doubles.head, value.doubles.last)
  }

  def decodeTimestamp(value: Value): Instant = new Instant(checkType(AtomicType.Timestamp, value).longs.head)

  def decodeDataset(value: Value): Dataset = Dataset(checkType(AtomicType.Dataset, value).strings.head)

  def decodeList(value: Value): Seq[Any] = {
    checkType(AtomicType.List, value)
    split(value, value.kind.args.head).map(decode)
  }

  def decodeSet(value: Value): Set[Any] = {
    checkType(AtomicType.Set, value)
    split(value, value.kind.args.head).map(decode).toSet
  }

  def decodeMap(value: Value): Map[Any, Any] = {
    checkType(AtomicType.Map, value)
    val (keysValue, valuesValue) = splitKeysValues(value)
    val keys = split(keysValue, value.kind.args.head).map(decode)
    val values = split(valuesValue, value.kind.args.last).map(decode)
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
  def parse(str: String, kind: DataType): Option[Value] =
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
   * Parse a string into a byte value.
   *
   * @param str String to parse.
   */
  def parseByte(str: String): Option[Value] = Try(str.toByte).toOption.map(encodeByte)

  /**
   * Parse a string into an integer value.
   *
   * @param str String to parse.
   */
  def parseInteger(str: String): Option[Value] = Try(str.toInt).toOption.map(encodeInteger)

  /**
   * Parse a string into a long value.
   *
   * @param str String to parse.
   */
  def parseLong(str: String): Option[Value] = Try(str.toLong).toOption.map(encodeLong)

  /**
   * Parse a string into a double value.
   *
   * @param str String to parse.
   */
  def parseDouble(str: String): Option[Value] = Try(str.toDouble).toOption.map(encodeDouble)

  /**
   * Parse a string into a boolean value.
   *
   * @param str String to parse.
   * @return A value representing a boolean, if the string was a valid boolean, [[None]] otherwise.
   */
  def parseBoolean(str: String): Option[Value] =
    str match {
      case "true" | "t" | "yes" | "y" => Some(encodeBoolean(true))
      case "false" | "f" | "no" | "n" => Some(encodeBoolean(false))
      case _ => None
    }

  def parseString(str: String): Option[Value] = Some(encodeString(str))

  def parseLocation(str: String): Option[Value] = Try(LatLng.parse(str)).toOption.map(encodeLocation)

  def parseTimestamp(str: String): Option[Value] = Try(Instant.parse(str)).toOption.map(encodeTimestamp)

  def parseDuration(str: String): Option[Value] = Try(new JodaDuration(TwitterDuration.parse(str).inMillis)).toOption.map(encodeDuration)

  def parseDistance(str: String): Option[Value] = Try(Distance.parse(str)).toOption.map(encodeDistance)

  def parseDataset(str: String): Option[Value] = Some(encodeDataset(Dataset(str)))

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

  private def merge(size: Int, values: Seq[Value], kind: DataType): Value = {
    Value(
      kind,
      bytes = values.flatMap(_.bytes),
      integers = values.flatMap(_.integers),
      longs = values.flatMap(_.longs),
      doubles = values.flatMap(_.doubles),
      booleans = values.flatMap(_.booleans),
      strings = values.flatMap(_.strings),
      size = size)
  }

  private def splitKeysValues(value: Value): (Value, Value) = {
    val (keySlotKind, keySlotSize) = slotUsage(value.kind.args.head)
    val (valueSlotKind, _) = slotUsage(value.kind.args.last)
    if (keySlotKind == valueSlotKind) {
      keySlotKind match {
        case AtomicType.Byte =>
          (value.copy(bytes = value.bytes.take(value.size * keySlotSize)), value.copy(bytes = value.bytes.drop(value.size * keySlotSize)))
        case AtomicType.Integer =>
          (value.copy(integers = value.integers.take(value.size * keySlotSize)), value.copy(integers = value.integers.drop(value.size * keySlotSize)))
        case AtomicType.Long =>
          (value.copy(longs = value.longs.take(value.size * keySlotSize)), value.copy(longs = value.longs.drop(value.size * keySlotSize)))
        case AtomicType.Double =>
          (value.copy(doubles = value.doubles.take(value.size * keySlotSize)), value.copy(doubles = value.doubles.drop(value.size * keySlotSize)))
        case AtomicType.String =>
          (value.copy(strings = value.strings.take(value.size * keySlotSize)), value.copy(strings = value.strings.drop(value.size * keySlotSize)))
        case AtomicType.Boolean =>
          (value.copy(booleans = value.booleans.take(value.size * keySlotSize)), value.copy(booleans = value.booleans.drop(value.size * keySlotSize)))
        case _ => throw new IllegalArgumentException(s"Invalid slot kind: $keySlotKind")
      }
    } else {
      (value, value)
    }
  }

  private def slotUsage(kind: AtomicType): (AtomicType, Int) =
    kind match {
      case AtomicType.Byte => (AtomicType.Byte, 1)
      case AtomicType.Integer => (AtomicType.Integer, 1)
      case AtomicType.Long => (AtomicType.Long, 1)
      case AtomicType.Double => (AtomicType.Double, 1)
      case AtomicType.String => (AtomicType.String, 1)
      case AtomicType.Boolean => (AtomicType.Boolean, 1)
      case AtomicType.Location => (AtomicType.Double, 2)
      case AtomicType.Timestamp => (AtomicType.Long, 1)
      case AtomicType.Duration => (AtomicType.Long, 1)
      case AtomicType.Distance => (AtomicType.Double, 1)
      case AtomicType.Dataset => (AtomicType.String, 1)
      case _ => throw new IllegalArgumentException(s"Invalid slot kind: $kind")
    }

  private def split(value: Value, kind: AtomicType): Seq[Value] = {
    val (slotKind, slotSize) = slotUsage(kind)

    def slice[T](i: Int, seq: Seq[T]): Seq[T] = {
      if (seq.isEmpty) {
        seq
      } else {
        seq.slice(i * slotSize, (i + 1) * slotSize)
      }
    }

    Seq.tabulate(value.size) { i =>
      slotKind match {
        case AtomicType.Byte => Value(DataType(kind), bytes = slice(i, value.bytes))
        case AtomicType.Integer => Value(DataType(kind), integers = slice(i, value.integers))
        case AtomicType.Long => Value(DataType(kind), longs = slice(i, value.longs))
        case AtomicType.Double => Value(DataType(kind), doubles = slice(i, value.doubles))
        case AtomicType.String => Value(DataType(kind), strings = slice(i, value.strings))
        case AtomicType.Boolean => Value(DataType(kind), booleans = slice(i, value.booleans))
        case _ => throw new IllegalArgumentException(s"Invalid kind: $kind")
      }
    }
  }

  private def checkType(kind: AtomicType, value: Value) = {
    if (value.kind.base != kind) {
      throw new IllegalArgumentException(s"${stringify(value.kind.base)} is not a ${stringify(kind)}")
    }
    value
  }

  private def checkType(kind: DataType, value: Value) = {
    if (value.kind != kind) {
      throw new IllegalArgumentException(s"${stringify(value.kind)} is not a ${stringify(kind)}")
    }
    value
  }
}

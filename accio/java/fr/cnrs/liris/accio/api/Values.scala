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
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.lumos.domain.RemoteFile
import fr.cnrs.liris.util.geo.{Distance, LatLng, Location}
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
   * @param value    Value.
   * @param dataType Target data type.
   * @return Corrected value, if the two data types were compatible.
   */
  def as(value: Value, dataType: DataType): Option[Value] = encode(decode(value), dataType)

  def encode(v: Any, dataType: DataType): Option[Value] =
    dataType match {
      case DataType.Atomic(AtomicType.Integer) => encodeInteger(v)
      case DataType.Atomic(AtomicType.Long) => encodeLong(v)
      case DataType.Atomic(AtomicType.Float) => encodeFloat(v)
      case DataType.Atomic(AtomicType.Double) => encodeDouble(v)
      case DataType.Atomic(AtomicType.Boolean) => encodeBoolean(v)
      case DataType.Atomic(AtomicType.String) => encodeString(v)
      case DataType.Atomic(AtomicType.Location) => encodeLocation(v)
      case DataType.Atomic(AtomicType.Timestamp) => encodeTimestamp(v)
      case DataType.Atomic(AtomicType.Duration) => encodeDuration(v)
      case DataType.Atomic(AtomicType.Distance) => encodeDistance(v)
      case DataType.Dataset(_) => encodeDataset(v)
      case DataType.MapType(MapType(keys, values)) => encodeMap(v, keys, values)
      case DataType.ListType(ListType(values)) => encodeList(v, values)
      case DataType.UnknownUnionField(_) => throw new IllegalArgumentException(s"Unknown type: $dataType")
    }

  def encodeInteger(v: Int): Value = Value(DataType.Atomic(AtomicType.Integer), integers = Seq(v))

  def encodeInteger(rawValue: Any): Option[Value] = rawValue match {
    case s: String => parseInteger(s)
    case n: Number => Some(encodeInteger(n.intValue))
    case b: Byte => Some(encodeInteger(b))
    case s: Short => Some(encodeInteger(s))
    case i: Int => Some(encodeInteger(i))
    case _ => None
  }

  def encodeLong(v: Long): Value = Value(DataType.Atomic(AtomicType.Long), longs = Seq(v))

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

  def encodeFloat(v: Float): Value = Value(DataType.Atomic(AtomicType.Float), doubles = Seq(v))

  def encodeFloat(rawValue: Any): Option[Value] =
    rawValue match {
      case s: String => parseFloat(s)
      case n: Number => Some(encodeFloat(n.floatValue))
      case f: Float => Some(encodeFloat(f))
      case _ => None
    }

  def encodeDouble(v: Double): Value = Value(DataType.Atomic(AtomicType.Double), doubles = Seq(v))

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

  def encodeString(v: String): Value = Value(DataType.Atomic(AtomicType.String), strings = Seq(v))

  def encodeString(rawValue: Any): Option[Value] =
    rawValue match {
      case s: String => Some(encodeString(s))
      case o => Some(encodeString(o.toString))
    }

  def encodeBoolean(v: Boolean): Value = {
    Value(DataType.Atomic(AtomicType.Boolean), booleans = Seq(v))
  }

  def encodeBoolean(rawValue: Any): Option[Value] =
    rawValue match {
      case s: String => parseBoolean(s)
      case b: java.lang.Boolean => Some(encodeBoolean(b))
      case b: Boolean => Some(encodeBoolean(b))
      case _ => None
    }

  def encodeLocation(v: Location): Value = {
    val latLng = v.asInstanceOf[Location].toLatLng
    Value(DataType.Atomic(AtomicType.Location), doubles = Seq(latLng.lat.radians, latLng.lng.radians))
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

  def encodeTimestamp(v: Instant): Value = {
    Value(DataType.Atomic(AtomicType.Timestamp), longs = Seq(v.getMillis))
  }

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

  def encodeDuration(v: JodaDuration): Value = {
    Value(DataType.Atomic(AtomicType.Duration), longs = Seq(v.getMillis))
  }

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

  def encodeDistance(v: Distance): Value = {
    Value(DataType.Atomic(AtomicType.Distance), doubles = Seq(v.meters))
  }

  def encodeDistance(rawValue: Any): Option[Value] =
    rawValue match {
      case s: String => parseDistance(s)
      case d: Distance => Some(encodeDistance(d))
      case _ => None
    }

  def encodeDataset(v: RemoteFile): Value = {
    Value(DataType.Dataset(DatasetType()), strings = Seq(v.uri))
  }

  def encodeDataset(rawValue: Any): Option[Value] =
    rawValue match {
      case s: String => parseDataset(s)
      case d: RemoteFile => Some(encodeDataset(d))
      case _ => None
    }

  def encodeList(v: Seq[_], listType: ListType): Option[Value] = encodeList(v, listType.values)

  def emptyList: Value = encodeList(Seq.empty, AtomicType.String).get

  def encodeList(v: Seq[_], of: AtomicType): Option[Value] = {
    val values = v.flatMap(encode(_, DataType.Atomic(of)))
    if (values.size == v.size) {
      Some(merge(values.size, values, DataType.ListType(ListType(of))))
    } else {
      None
    }
  }

  def encodeList(rawValue: Any, of: AtomicType): Option[Value] =
    rawValue match {
      case arr: Array[_] => encodeList(arr.toSeq, of)
      case seq: Seq[_] => encodeList(seq, of)
      case list: java.util.List[_] => encodeList(list.asScala, of)
      case set: Set[_] => encodeList(set.toSeq, of)
      case set: java.util.Set[_] => encodeList(set.asScala.toSeq, of)
      case _ => None
    }

  def emptyMap: Value = encodeMap(Map.empty, AtomicType.String, AtomicType.String).get

  def encodeMap(v: Map[_, _], mapType: MapType): Option[Value] = {
    encodeMap(v, mapType.keys, mapType.values)
  }

  def encodeMap(v: Map[_, _], ofKeys: AtomicType, ofValues: AtomicType): Option[Value] = {
    val keys = v.keys.toSeq.flatMap(encode(_, DataType.Atomic(ofKeys)))
    val values = v.values.toSeq.flatMap(encode(_, DataType.Atomic(ofValues)))
    if (keys.size == v.size && values.size == v.size) {
      Some(merge(keys.size, keys ++ values, DataType.MapType(MapType(ofKeys, ofValues))))
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
    value.dataType match {
      case DataType.Atomic(AtomicType.Duration) =>
        TwitterDuration.fromMilliseconds(decodeDuration(value).getMillis).toString
      case DataType.ListType(tpe) => stringify(decodeList(value, tpe), abbreviate)
      case DataType.MapType(tpe) =>
        val seq = decodeMap(value, tpe).toSeq
          .map {
            case (k, v) => s"${stringify(k)}=${stringify(v)}"
            case s => s // For the last <xx more>
          }
        stringify(seq, abbreviate)
      case DataType.Dataset(tpe) => decodeDataset(value, tpe).uri
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

  def decode(value: Value): Any = decode(value, value.dataType)

  def decode(value: Value, dataType: DataType): Any =
    dataType match {
      case DataType.Atomic(AtomicType.Integer) => decodeInteger(value)
      case DataType.Atomic(AtomicType.Long) => decodeLong(value)
      case DataType.Atomic(AtomicType.Float) => decodeFloat(value)
      case DataType.Atomic(AtomicType.Double) => decodeDouble(value)
      case DataType.Atomic(AtomicType.String) => decodeString(value)
      case DataType.Atomic(AtomicType.Boolean) => decodeBoolean(value)
      case DataType.Atomic(AtomicType.Location) => decodeLocation(value)
      case DataType.Atomic(AtomicType.Timestamp) => decodeTimestamp(value)
      case DataType.Atomic(AtomicType.Duration) => decodeDuration(value)
      case DataType.Atomic(AtomicType.Distance) => decodeDistance(value)
      case DataType.Dataset(tpe) => decodeDataset(value, tpe)
      case DataType.ListType(tpe) => decodeList(value, tpe)
      case DataType.MapType(tpe) => decodeMap(value, tpe)
      case DataType.Atomic(AtomicType.EnumUnknownAtomicType(_)) => throw new IllegalArgumentException("Unknown data type")
      case DataType.UnknownUnionField(_) => throw new IllegalArgumentException("Unknown data type")
    }

  def decodeInteger(value: Value): Int = value.integers.head

  def decodeLong(value: Value): Long = value.longs.head

  def decodeFloat(value: Value): Float = value.doubles.head.toFloat

  def decodeDouble(value: Value): Double = value.doubles.head

  def decodeBoolean(value: Value): Boolean = value.booleans.head

  def decodeString(value: Value): String = value.strings.head

  def decodeLocation(value: Value): LatLng = {
    LatLng.radians(value.doubles.head, value.doubles.last)
  }

  def decodeTimestamp(value: Value): Instant = new Instant(value.longs.head)

  def decodeDataset(value: Value, tpe: DatasetType): RemoteFile = RemoteFile(value.strings.head)

  def decodeList(value: Value, tpe: ListType): Seq[Any] = {
    split(value, tpe.values).map(decode)
  }

  def decodeMap(value: Value, tpe: MapType): Map[Any, Any] = {
    val (keysValue, valuesValue) = splitKeysValues(value, tpe)
    val keys = split(keysValue, tpe.keys).map(decode)
    val values = split(valuesValue, tpe.values).map(decode)
    Map(keys.zip(values): _*)
  }

  def decodeDistance(value: Value): Distance = Distance.meters(value.doubles.head)

  def decodeDuration(value: Value): JodaDuration = new JodaDuration(value.longs.head)

  /**
   * Parse a string into a given data type.
   *
   * @param str      String to parse.
   * @param dataType Data type.
   */
  def parse(str: String, dataType: AtomicType): Option[Value] =
    dataType match {
      case AtomicType.Boolean => parseBoolean(str)
      case AtomicType.Integer => parseInteger(str)
      case AtomicType.Long => parseLong(str)
      case AtomicType.Float => parseFloat(str)
      case AtomicType.Double => parseDouble(str)
      case AtomicType.String => parseString(str)
      case AtomicType.Location => parseLocation(str)
      case AtomicType.Timestamp => parseTimestamp(str)
      case AtomicType.Duration => parseDuration(str)
      case AtomicType.Distance => parseDistance(str)
      case AtomicType.EnumUnknownAtomicType(_) => throw new IllegalArgumentException("Unknown data type")
    }

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
   * Parse a string into a float value.
   *
   * @param str String to parse.
   */
  def parseFloat(str: String): Option[Value] = Try(str.toFloat).toOption.map(encodeFloat)

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

  def parseDataset(str: String): Option[Value] = Some(encodeDataset(RemoteFile(str)))

  private def merge(size: Int, values: Seq[Value], dataType: DataType): Value = {
    Value(
      dataType,
      bytes = values.flatMap(_.bytes),
      integers = values.flatMap(_.integers),
      longs = values.flatMap(_.longs),
      doubles = values.flatMap(_.doubles),
      booleans = values.flatMap(_.booleans),
      strings = values.flatMap(_.strings))
  }

  private def splitKeysValues(value: Value, tpe: MapType): (Value, Value) = {
    val (keySlotKind, keySlotSize) = slotUsage(tpe.keys)
    val (valueSlotKind, valueSlotSize) = slotUsage(tpe.values)
    if (keySlotKind == valueSlotKind) {
      keySlotKind match {
        case AtomicType.Integer =>
          val (k, v) = value.integers.splitAt(value.integers.size * keySlotSize / (valueSlotSize + keySlotSize))
          (value.copy(integers = k), value.copy(integers = v))
        case AtomicType.Long =>
          val (k, v) = value.longs.splitAt(value.longs.size * keySlotSize / (valueSlotSize + keySlotSize))
          (value.copy(longs = k), value.copy(longs = v))
        case AtomicType.Double | AtomicType.Float =>
          val (k, v) = value.doubles.splitAt(value.doubles.size * keySlotSize / (valueSlotSize + keySlotSize))
          (value.copy(doubles = k), value.copy(doubles = v))
        case AtomicType.String =>
          val (k, v) = value.strings.splitAt(value.strings.size * keySlotSize / (valueSlotSize + keySlotSize))
          (value.copy(strings = k), value.copy(strings = v))
        case AtomicType.Boolean =>
          val (k, v) = value.booleans.splitAt(value.booleans.size * keySlotSize / (valueSlotSize + keySlotSize))
          (value.copy(booleans = k), value.copy(booleans = v))
        case _ => throw new AssertionError
      }
    } else {
      (value, value)
    }
  }

  private def slotUsage(dataType: AtomicType): (AtomicType, Int) =
    dataType match {
      case AtomicType.Integer => (AtomicType.Integer, 1)
      case AtomicType.Long => (AtomicType.Long, 1)
      case AtomicType.Float => (AtomicType.Float, 1)
      case AtomicType.Double => (AtomicType.Double, 1)
      case AtomicType.String => (AtomicType.String, 1)
      case AtomicType.Boolean => (AtomicType.Boolean, 1)
      case AtomicType.Location => (AtomicType.Double, 2)
      case AtomicType.Timestamp => (AtomicType.Long, 1)
      case AtomicType.Duration => (AtomicType.Long, 1)
      case AtomicType.Distance => (AtomicType.Double, 1)
      case _ => throw new AssertionError
    }

  private def split(value: Value, dataType: AtomicType): Seq[Value] = {
    val (slotKind, slotSize) = slotUsage(dataType)

    def slice[T](i: Int, seq: Seq[T]): Seq[T] = {
      if (seq.isEmpty) {
        seq
      } else {
        seq.slice(i * slotSize, (i + 1) * slotSize)
      }
    }

    slotKind match {
      case AtomicType.Integer =>
        val size = value.integers.size / slotSize
        Seq.tabulate(size)(i => Value(DataType.Atomic(dataType), integers = slice(i, value.integers)))
      case AtomicType.Long =>
        val size = value.longs.size / slotSize
        Seq.tabulate(size)(i => Value(DataType.Atomic(dataType), longs = slice(i, value.longs)))
      case AtomicType.Double | AtomicType.Float =>
        val size = value.doubles.size / slotSize
        Seq.tabulate(size)(i => Value(DataType.Atomic(dataType), doubles = slice(i, value.doubles)))
      case AtomicType.String =>
        val size = value.strings.size / slotSize
        Seq.tabulate(size)(i => Value(DataType.Atomic(dataType), strings = slice(i, value.strings)))
      case AtomicType.Boolean =>
        val size = value.booleans.size / slotSize
        Seq.tabulate(size)(i => Value(DataType.Atomic(dataType), booleans = slice(i, value.booleans)))
      case _ => throw new AssertionError
    }
  }

  private def checkType(dataType: DataType, value: Value) = {
    if (value.dataType != dataType) {
      throw new IllegalArgumentException(s"${stringify(value.dataType)} is not a ${stringify(dataType)}")
    }
    value
  }
}

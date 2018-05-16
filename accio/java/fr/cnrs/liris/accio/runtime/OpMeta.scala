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

package fr.cnrs.liris.accio.runtime

import java.io.IOException

import com.twitter.util.StorageUnit
import com.twitter.util.logging.Logging
import fr.cnrs.liris.accio.api.Values
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.accio.domain.RemoteFile
import fr.cnrs.liris.accio.sdk.{Arg, Op, ScalaOperator}
import fr.cnrs.liris.util.ResourceFileLoader
import fr.cnrs.liris.util.StringUtils.maybe
import fr.cnrs.liris.util.geo.{Distance, Location}
import fr.cnrs.liris.util.reflect.{CaseClass, ScalaType}
import org.joda.time.{Duration, Instant}

import scala.reflect.runtime.{universe => ru}
import scala.util.matching.Regex

/**
 * Metadata about an operator, i.e., its definition and runtime information.
 *
 * @param defn    Operator definition.
 * @param opClass Operator class.
 */
final class OpMeta(val defn: Operator, val opClass: CaseClass)

object OpMeta extends Logging {
  /**
   * Pattern for valid operator names.
   */
  private[this] val OpPattern = "[A-Z][a-zA-Z0-9_]+"

  /**
   * Regex for valid operator names.
   */
  private[this] val OpRegex: Regex = ("^" + OpPattern + "$").r

  def apply[T <: ScalaOperator[_] : ru.TypeTag]: OpMeta = apply(ru.typeOf[T])

  def apply(tpe: ru.Type): OpMeta = {
    val opRefl = CaseClass.apply(tpe)
    opRefl.annotations.get[Op] match {
      case None => throw new IllegalArgumentException(s"Operator in $tpe must be annotated with @Op")
      case Some(op) =>
        val clazz = opRefl.runtimeClass
        val outRefl = getOutRefl(opRefl)
        val name = if (op.name.nonEmpty) op.name else clazz.getSimpleName.stripSuffix("Op")
        if (OpRegex.findFirstIn(name).isEmpty) {
          throw new IllegalArgumentException(s"Invalid name for operator in $tpe: $name")
        }
        val resources = ComputeResources(
          cpus = op.cpus,
          ramMb = parseStorageUnit(op.ram()).inMegabytes,
          diskGb = parseStorageUnit(op.disk()).inGigabytes)
        val defn = Operator(
          name = name,
          category = op.category,
          inputs = getInputs(opRefl),
          outputs = getOutputs(outRefl),
          help = maybe(op.help),
          description = maybe(op.description).flatMap(loadDescription(_, clazz)),
          deprecation = maybe(op.deprecation),
          unstable = op.unstable,
          resource = resources,
          executable = ".")
        new OpMeta(defn, opRefl)
    }
  }

  private def loadDescription(description: String, clazz: Class[_]) = {
    if (description.startsWith("resource:")) {
      val resourceName = description.substring("resource:".length)
      try {
        Some(ResourceFileLoader.loadResource(clazz, resourceName))
      } catch {
        case e: IOException =>
          logger.warn(s"Failed to load help resource '$resourceName': ${e.getMessage}", e)
          None
      }
    } else {
      Some(description)
    }
  }

  private def getOutRefl(opRefl: CaseClass): CaseClass = {
    val typ = opRefl.scalaType.baseType[ScalaOperator[_]].args.head
    CaseClass.apply(typ.tpe)
  }

  private def getInputs(inRefl: CaseClass) =
    inRefl.fields.map { field =>
      field.annotations.get[Arg] match {
        case None => throw new IllegalArgumentException(
          s"Input ${inRefl.runtimeClass.getName}.${field.name} must be annotated with @Arg")
        case Some(in) =>
          // To simplify some already too much complicated cases, we forbid to have optional inputs (i.e., of type
          // Option[_]) with a default value (i.e., Some(...)). It unnecessarily complicate things and later checks.
          // Either an input is optional and does not come with a default value, either an input is mandatory and
          // comes possibly with a default value.
          //
          // Impl. note: Option[_] fields do have a default value, which is None, hence the check that this default
          // value is defined and equals None.
          if (field.scalaType.isOption && !field.defaultValue.contains(None)) {
            throw new IllegalArgumentException(
              s"Input ${inRefl.runtimeClass.getName}.${field.name} cannot be optional with a default value")
          }

          val isOptional = field.scalaType.isOption
          val dataType = getDataType(if (isOptional) field.scalaType.args.head else field.scalaType)
          val defaultValue = if (isOptional) {
            None
          } else {
            field.defaultValue.flatMap(Values.encode(_, dataType))
          }
          Attribute(
            name = field.name,
            dataType = dataType,
            help = maybe(in.help),
            isOptional = isOptional,
            defaultValue = defaultValue)
      }
    }

  private def getOutputs(outRefl: CaseClass) = {
    outRefl.fields.flatMap { field =>
      field
        .annotations
        .get[Arg]
        .map(out => Attribute(field.name, getDataType(field.scalaType), maybe(out.help)))
        .toSeq
    }
  }

  private def getDataType(scalaType: ScalaType): DataType =
    scalaType.tpe match {
      case c if c <:< ru.typeOf[Seq[_]] =>
        val of = getAtomicType(scalaType.args.head)
        DataType.ListType(ListType(of))
      case c if c <:< ru.typeOf[Map[_, _]] =>
        val keys = getAtomicType(scalaType.args.head)
        val values = getAtomicType(scalaType.args.last)
        DataType.MapType(MapType(keys, values))
      case c if c <:< ru.typeOf[RemoteFile] => DataType.Dataset(DatasetType())
      case _ => DataType.Atomic(getAtomicType(scalaType))
    }

  private def getAtomicType(scalaType: ScalaType): AtomicType = getAtomicType(scalaType.tpe)

  private def getAtomicType(tpe: ru.Type): AtomicType = {
    tpe match {
      case c if c == ru.typeOf[Boolean] || c == ru.typeOf[java.lang.Boolean] => AtomicType.Boolean
      case c if c == ru.typeOf[Int] || c == ru.typeOf[java.lang.Integer] => AtomicType.Integer
      case c if c == ru.typeOf[Long] || c == ru.typeOf[java.lang.Long] => AtomicType.Long
      case c if c == ru.typeOf[Float] || c == ru.typeOf[java.lang.Float] => AtomicType.Float
      case c if c == ru.typeOf[Double] || c == ru.typeOf[java.lang.Double] => AtomicType.Double
      case c if c == ru.typeOf[String] => AtomicType.String
      case c if c <:< ru.typeOf[Location] => AtomicType.Location
      case c if c <:< ru.typeOf[Instant] => AtomicType.Timestamp
      case c if c <:< ru.typeOf[Duration] => AtomicType.Duration
      case c if c <:< ru.typeOf[Distance] => AtomicType.Distance
      case _ => throw new IllegalArgumentException(s"Unsupported data type: $tpe")
    }
  }

  private[this] val StorageUnitRegex = "^(\\d+(?:\\.\\d*)?)\\s?([a-zA-Z]?)$".r

  private def parseStorageUnit(str: String) = str match {
    case StorageUnitRegex(v, u) =>
      val vv = v.toLong
      val uu = u.toLowerCase match {
        case "" | "b" => 1L
        case "k" => 1L << 10
        case "m" => 1L << 20
        case "g" => 1L << 30
        case "t" => 1L << 40
        case "p" => 1L << 50
        case "e" => 1L << 60
        case badUnit => throw new NumberFormatException(s"Unrecognized unit $badUnit")
      }
      new StorageUnit(vv * uu)
    case _ => throw new NumberFormatException(s"invalid storage unit string: $str")
  }
}

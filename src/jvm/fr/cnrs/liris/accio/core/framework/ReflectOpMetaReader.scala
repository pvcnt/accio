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

import java.io.IOException
import java.util.NoSuchElementException

import com.twitter.util.StorageUnit
import fr.cnrs.liris.accio.core.api._
import fr.cnrs.liris.common.geo.{Distance, Location}
import fr.cnrs.liris.common.reflect.{CaseClass, PlainClass, ScalaType}
import fr.cnrs.liris.common.util.ResourceFileLoader
import fr.cnrs.liris.common.util.StringUtils.maybe
import org.joda.time.{Duration, Instant}

/**
 * Reads operator metadata from information gathered by reflection and annotations.
 */
class ReflectOpMetaReader extends OpMetaReader {
  def read[T <: Operator[_, _]](clazz: Class[T]): OpMeta =
    try {
      val opRefl = PlainClass(clazz)
      opRefl.getAnnotation[Op] match {
        case None => throw new IllegalArgumentException(s"Operator must be annotated with @Op")
        case Some(op) =>
          val inRefl = getInRefl(opRefl)
          val outRefl = getOutRefl(opRefl)
          val name = if (op.name.nonEmpty) op.name else clazz.getSimpleName.stripSuffix("Op")
          val resource = Resource(op.cpu, parseStorageUnit(op.memory()), parseStorageUnit(op.disk()))
          val defn = OpDef(
            name = name,
            inputs = inRefl.map(getInputs).getOrElse(Seq.empty),
            outputs = outRefl.map(getOutputs).getOrElse(Seq.empty),
            help = maybe(op.help),
            description = maybe(op.description).map(loadDescription(_, clazz)),
            category = op.category,
            deprecation = maybe(op.deprecation),
            resource = resource)
          OpMeta(defn, clazz, inRefl.map(_.runtimeClass), outRefl.map(_.runtimeClass))
      }
    } catch {
      case e: NoSuchElementException => throw new IllegalOpException(clazz, e)
      case e: IllegalArgumentException => throw new IllegalOpException(clazz, e)
    }

  private def loadDescription(description: String, clazz: Class[_]) = {
    if (description.startsWith("resource:")) {
      val resourceName = description.substring("resource:".length)
      try {
        ResourceFileLoader.loadResource(clazz, resourceName)
      } catch {
        case e: IOException =>
          throw new IllegalStateException(s"Failed to load help resource '$resourceName': ${e.getMessage}", e)
      }
    } else {
      description
    }
  }

  private def getInRefl(opRefl: PlainClass): Option[CaseClass] = {
    val typ = opRefl.scalaType.baseClass(classOf[Operator[_, _]]).typeArgs.head
    if (typ.isUnit) None else Some(CaseClass(typ.runtimeClass))
  }

  private def getOutRefl(opRefl: PlainClass): Option[CaseClass] = {
    val typ = opRefl.scalaType.baseClass(classOf[Operator[_, _]]).typeArgs.last
    if (typ.isUnit) None else Some(CaseClass(typ.runtimeClass))
  }

  private def getInputs(inRefl: CaseClass) =
    inRefl.fields.map { field =>
      field.getAnnotation[Arg] match {
        case None => throw new IllegalArgumentException(s"Input ${field.name} must be annotated with @Arg")
        case Some(in) =>
          // To simplify some already too much complicated cases, we forbid to have optional inputs (i.e., of type
          // Option[_]) with a default value (i.e., Some(...)). It unnecessarily complicate things and later checks.
          // Either an input is optional and does not come with a default value, either an input is mandatory and
          // comes possibly with a default value.
          //
          // Impl. note: Option[_] fields do have a default value, which is none, hence the check that this default
          // value is defined and equals None.
          require(!field.isOption || field.defaultValue.contains(None), s"Input ${field.name} cannot be optional with a default value")

          val tpe = if (field.isOption) field.scalaType.typeArguments.head else field.scalaType
          InputArgDef(
            name = field.name,
            kind = getDataType(tpe),
            help = maybe(in.help),
            isOptional = field.isOption,
            defaultValue = field.defaultValue)
      }
    }

  private def getOutputs(outRefl: CaseClass) =
    outRefl.fields.flatMap { field =>
      field.getAnnotation[Arg].map { out =>
        OutputArgDef(field.name, maybe(out.help), getDataType(field.scalaType))
      }
    }

  private def getDataType(scalaType: ScalaType): DataType = scalaType.runtimeClass match {
    case c if c == classOf[Boolean] || c == classOf[java.lang.Boolean] => DataType.Boolean
    case c if c == classOf[Byte] || c == classOf[java.lang.Byte] => DataType.Byte
    case c if c == classOf[Short] || c == classOf[java.lang.Short] => DataType.Short
    case c if c == classOf[Int] || c == classOf[java.lang.Integer] => DataType.Integer
    case c if c == classOf[Long] || c == classOf[java.lang.Long] => DataType.Long
    case c if c == classOf[Double] || c == classOf[java.lang.Double] => DataType.Double
    case c if c == classOf[String] => DataType.String
    case c if classOf[Location].isAssignableFrom(c) => DataType.Location
    case c if c == classOf[Instant] => DataType.Timestamp
    case c if c == classOf[Duration] => DataType.Duration
    case c if c == classOf[Distance] => DataType.Distance
    case c if c == classOf[Dataset] => DataType.Dataset
    case c if c == classOf[Image] => DataType.Image
    case c if classOf[Seq[_]].isAssignableFrom(c) =>
      val of = getDataType(scalaType.typeArguments.head)
      DataType.List(of)
    case c if classOf[Set[_]].isAssignableFrom(c) =>
      val of = getDataType(scalaType.typeArguments.head)
      DataType.Set(of)
    case c if classOf[Map[_, _]].isAssignableFrom(c) =>
      val ofKeys = getDataType(scalaType.typeArguments.head)
      val ofValues = getDataType(scalaType.typeArguments.last)
      DataType.Map(ofKeys, ofValues)
    case _ => throw new IllegalArgumentException(s"Unsupported data type: $scalaType")
  }

  private[this] val StorageUnitRegex = "^(\\d+(?:\\.\\d*)\\s?([a-zA-Z]?)".r

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
    case _ =>
      throw new NumberFormatException(s"invalid storage unit string: $str")
  }
}
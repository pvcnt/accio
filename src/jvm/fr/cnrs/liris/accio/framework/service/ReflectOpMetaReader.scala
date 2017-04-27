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

package fr.cnrs.liris.accio.framework.service

import java.io.IOException

import com.google.inject.Inject
import com.twitter.util.StorageUnit
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.framework.sdk._
import fr.cnrs.liris.accio.framework.api.Utils
import fr.cnrs.liris.accio.framework.api.thrift._
import fr.cnrs.liris.common.reflect.{CaseClass, CaseClassField, PlainClass, ScalaType}
import fr.cnrs.liris.common.util.ResourceFileLoader
import fr.cnrs.liris.common.util.StringUtils.maybe
import fr.cnrs.liris.dal.core.api.{AtomicType, DataType, DataTypes, Values}

/**
 * Read operator metadata from information gathered by Scala reflection and annotations.
 *
 * @param valueValidator Value validator.
 */
final class ReflectOpMetaReader @Inject()(valueValidator: ValueValidator) extends OpMetaReader with LazyLogging {
  def read[T <: Operator[_, _]](clazz: Class[T]): OpMeta =
    try {
      val opRefl = PlainClass(clazz)
      opRefl.getAnnotation[Op] match {
        case None => throw new IllegalArgumentException(s"Operator must be annotated with @Op")
        case Some(op) =>
          val inRefl = getInRefl(opRefl)
          val outRefl = getOutRefl(opRefl)
          val name = if (op.name.nonEmpty) op.name else clazz.getSimpleName.stripSuffix("Op")
          if (Utils.OpRegex.findFirstIn(name).isEmpty) {
            throw new InvalidOpException(clazz, s"Illegal operator name: $name")
          }
          val resource = Resource(op.cpu, parseStorageUnit(op.ram()).inMegabytes, parseStorageUnit(op.disk()).inMegabytes)
          val defn = OpDef(
            name = name,
            inputs = inRefl.map(getInputs).getOrElse(Seq.empty),
            outputs = outRefl.map(getOutputs).getOrElse(Seq.empty),
            help = maybe(op.help),
            description = maybe(op.description).flatMap(loadDescription(_, clazz)),
            category = op.category,
            deprecation = maybe(op.deprecation),
            unstable = op.unstable,
            resource = resource)
          OpMeta(defn, clazz, inRefl.map(_.runtimeClass), outRefl.map(_.runtimeClass))
      }
    } catch {
      case e: IllegalArgumentException =>
        throw new InvalidOpException(clazz, e.getMessage.stripPrefix("requirement failed: "))
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
          // Impl. note: Option[_] fields do have a default value, which is None, hence the check that this default
          // value is defined and equals None.
          require(!field.isOption || field.defaultValue.contains(None), s"Input ${field.name} cannot be optional with a default value")

          val kind = getDataType(if (field.isOption) field.scalaType.typeArguments.head else field.scalaType)
          val defaultValue = if (field.isOption) None else field.defaultValue.map(Values.encode(_, kind))
          val argDef = ArgDef(
            name = field.name,
            kind = kind,
            help = maybe(in.help),
            isOptional = field.isOption,
            defaultValue = defaultValue,
            constraint = getConstraint(field, kind))

          // Check the default value is valid w.r.t. argument definition.
          argDef.defaultValue.foreach { defaultValue =>
            val errors = valueValidator.validate(defaultValue, argDef)
            if (errors.size == 1) {
              throw new IllegalArgumentException(s"Invalid default value: ${errors.head.message}")
            } else if (errors.size > 1) {
              throw new IllegalArgumentException(s"Invalid default value:\n - ${errors.map(_.message).mkString("\n - ")}")
            }
          }
          argDef
      }
    }

  private def getOutputs(outRefl: CaseClass) = {
    outRefl.fields.flatMap { field =>
      field.getAnnotation[Arg].map { out =>
        ArgDef(
          name = field.name,
          help = maybe(out.help),
          kind = getDataType(field.scalaType))
      }.toSeq
    }
  }

  private def getConstraint(field: CaseClassField, kind: DataType) = {
    val (minValue, minInclusive) = field.getAnnotation[Min] match {
      case Some(min) =>
        kind.base match {
          case AtomicType.Byte | AtomicType.Integer | AtomicType.Long | AtomicType.Double =>
            (Some(min.value), Some(min.inclusive))
          case AtomicType.Set | AtomicType.List | AtomicType.Map =>
            (Some(min.value), Some(min.inclusive))
          case _ => throw new IllegalArgumentException(s"Input ${field.name} of type ${DataTypes.toString(kind)} cannot have a @Min constraint")
        }
      case None => (None, None)
    }
    val (maxValue, maxInclusive) = field.getAnnotation[Max] match {
      case Some(max) =>
        require(minValue.isEmpty || max.value > minValue.get, s"Input ${field.name} @Max is less than @Min")
        kind.base match {
          case AtomicType.Byte | AtomicType.Integer | AtomicType.Long | AtomicType.Double => (Some(max.value), Some(max.inclusive))
          case AtomicType.Set | AtomicType.List | AtomicType.Map => (Some(max.value), Some(max.inclusive))
          case _ => throw new IllegalArgumentException(s"Input ${field.name} of type ${DataTypes.toString(kind)} cannot have a @Max constraint")
        }
      case None => (None, None)
    }
    val allowedValues = field.getAnnotation[OneOf] match {
      case Some(oneOf) =>
        require(oneOf.value.nonEmpty, s"Input ${field.name} @OneOf does not specify any value")
        kind match {
          case DataType(AtomicType.String, _) => oneOf.value.toSet
          case DataType(AtomicType.Set, Seq(AtomicType.String)) => oneOf.value.toSet
          case DataType(AtomicType.List, Seq(AtomicType.String)) => oneOf.value.toSet
          case _ => throw new IllegalArgumentException(s"Input ${field.name} of type ${DataTypes.toString(kind)} cannot have a @OneOf constraint")
        }
      case None => Set.empty[String]
    }
    if (minValue.isDefined || maxValue.isDefined || allowedValues.nonEmpty) {
      Some(ArgConstraint(minValue, minInclusive, maxValue, maxInclusive, allowedValues))
    } else {
      None
    }
  }

  private def getDataType(scalaType: ScalaType): DataType = {
    // TODO: prevent nested types.
    Values.getAtomicType(scalaType.runtimeClass) match {
      case AtomicType.List =>
        val of = Values.getAtomicType(scalaType.typeArguments.head.runtimeClass)
        DataType(AtomicType.List, Seq(of))
      case AtomicType.Set =>
        val of = Values.getAtomicType(scalaType.typeArguments.head.runtimeClass)
        DataType(AtomicType.Set, Seq(of))
      case AtomicType.Map =>
        val ofKeys = Values.getAtomicType(scalaType.typeArguments.head.runtimeClass)
        val ofValues = Values.getAtomicType(scalaType.typeArguments.last.runtimeClass)
        DataType(AtomicType.Map, Seq(ofKeys, ofValues))
      case atomicType => DataType(atomicType)
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
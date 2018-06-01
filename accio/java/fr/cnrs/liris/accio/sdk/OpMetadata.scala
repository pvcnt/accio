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

import java.io.IOException

import com.twitter.util.logging.Logging
import fr.cnrs.liris.accio.domain.{Attribute, Operator}
import fr.cnrs.liris.lumos.domain.{DataType, RemoteFile, Value}
import fr.cnrs.liris.util.ResourceFileLoader
import fr.cnrs.liris.util.StringUtils.maybe
import fr.cnrs.liris.util.reflect.CaseClass

import scala.reflect.runtime.{universe => ru}
import scala.util.matching.Regex

/**
 * Metadata about an operator, i.e., its definition and its JVM class. The class is needed
 * internally to instantiate it later on.
 *
 * @param defn  Operator definition.
 * @param clazz Operator class.
 */
final class OpMetadata(val defn: Operator, val clazz: Class[_])

object OpMetadata extends Logging {
  /**
   * Pattern for valid operator names.
   */
  private[this] val OpPattern = "[A-Z][a-zA-Z0-9_]+"

  /**
   * Regex for valid operator names.
   */
  private[this] val OpRegex: Regex = ("^" + OpPattern + "$").r

  /**
   * Read operator metadata from its type.
   *
   * @tparam T Operator type.
   * @throws IllegalArgumentException If the operator is invalid.
   */
  def apply[T <: ScalaOperator[_] : ru.TypeTag]: OpMetadata = apply(ru.typeOf[T])

  /**
   * Read operator metadata from its type.
   *
   * @param tpe Operator type.
   * @throws IllegalArgumentException If the operator is invalid.
   */
  def apply(tpe: ru.Type): OpMetadata = {
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
        val defn = Operator(
          name = name,
          category = op.category,
          executable = RemoteFile("."),
          inputs = getInputs(opRefl),
          outputs = getOutputs(outRefl),
          help = maybe(op.help),
          description = maybe(op.description).flatMap(loadDescription(_, clazz)),
          deprecation = maybe(op.deprecation),
          unstable = op.unstable)
        new OpMetadata(defn, clazz)
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

  private def getInputs(inRefl: CaseClass): Seq[Attribute] =
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
          val cls = if (isOptional) field.scalaType.args.head.runtimeClass else field.scalaType.runtimeClass
          val dataType = DataType.find(cls).getOrElse {
            throw new IllegalArgumentException(s"Unsupported Scala type: ${cls.getName}")
          }
          val defaultValue = if (isOptional) {
            None
          } else {
            field.defaultValue.map(Value.apply(_, dataType))
          }
          Attribute(
            name = field.name,
            dataType = dataType,
            help = maybe(in.help),
            optional = isOptional,
            defaultValue = defaultValue)
      }
    }

  private def getOutputs(outRefl: CaseClass) = {
    outRefl.fields.flatMap { field =>
      field.annotations.get[Arg].map { out =>
        val dataType = DataType.find(field.scalaType.runtimeClass).getOrElse {
          throw new IllegalArgumentException(s"Unsupported Scala type: ${field.scalaType.runtimeClass.getName}")
        }
        Attribute(field.name, dataType = dataType, help = maybe(out.help))
      }.toSeq
    }
  }
}

package fr.cnrs.liris.common.flags

import fr.cnrs.liris.common.reflect.{ReflectCaseClass, ReflectCaseField}

import scala.collection.mutable
import scala.reflect.runtime.universe._

/**
 * An immutable selection of flags data corresponding to a set of flags classes. The data is
 * collected using reflection, which can be expensive. Therefore this class can be used
 * internally to cache the results.
 *
 * @param classes    These are the flags-declaring classes which are annotated with
 *                   [[Flag]] annotations
 * @param fields     Maps flag name to Flag-annotated field
 * @param converters Mapping from each Flag-annotated field to the proper converter
 */
final class FlagsData private(
    val classes: Map[Class[_], ReflectCaseClass],
    val fields: Map[String, ReflectCaseField],
    val converters: Map[ReflectCaseField, Converter[_]])

object FlagsData {
  /**
   * A cache for the parsed options data. Both keys and values are immutable, so
   * this is always safe. Only access this field through the [[of()]]
   * method for thread-safety! The cache is very unlikely to grow to a significant amount of memory,
   * because there's only a fixed set of options classes on the classpath.
   */
  private val cache = mutable.Map.empty[Seq[String], FlagsData]

  def of[T: TypeTag]: FlagsData = of(typeOf[T])

  def of(tpe: Type): FlagsData = {
    val types =
      if (tpe =:= typeOf[Unit]) {
        Seq.empty[Type]
      } else if (tpe <:< typeOf[Product] && tpe.typeArgs.nonEmpty) {
        tpe.typeArgs
      } else {
        Seq(tpe)
      }
    of(types)
  }

  def of(types: Seq[Type]): FlagsData = synchronized {
    cache.getOrElseUpdate(types.map(_.toString).sorted, create(types))
  }

  private def create(types: Seq[Type]): FlagsData = {
    val classes = mutable.Map.empty[Class[_], ReflectCaseClass]
    val fields = mutable.Map.empty[String, ReflectCaseField]
    val converters = mutable.Map.empty[ReflectCaseField, Converter[_]]
    types.foreach { tpe =>
      val refl = ReflectCaseClass.of(tpe)
      classes += refl.runtimeClass -> refl

      refl.fields.foreach { field =>
        require(field.isAnnotated[Flag], "All fields must be annotated with @Flag")
        val annotation = field.annotation[Flag]
        require(annotation.name.nonEmpty, "Flag cannot have an empty name")
        require(!fields.contains(annotation.name), s"Duplicate flag name: -${annotation.name}")
        fields(annotation.name) = field
        converters(field) = Converter.of(field.tpe)
      }
    }
    new FlagsData(classes.toMap, fields.toMap, converters.toMap)
  }
}
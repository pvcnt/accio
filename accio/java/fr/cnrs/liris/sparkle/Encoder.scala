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

package fr.cnrs.liris.sparkle

import java.lang.reflect.Modifier

import fr.cnrs.liris.sparkle.format.{DataType, InternalRow, StructType}
import org.joda.time.Instant

import scala.reflect.ClassTag
import scala.reflect.runtime.universe.{TypeTag, typeTag}

trait Encoder[T] {
  def structType: StructType

  def classTag: ClassTag[T]

  def serialize(obj: T): Iterable[InternalRow]

  def deserialize(row: InternalRow): T
}

object Encoder {
  val universe: scala.reflect.runtime.universe.type = scala.reflect.runtime.universe

  implicit val Int32Encoder: Encoder[Int] = apply[Int]
  implicit val Int64Encoder: Encoder[Long] = apply[Long]
  implicit val Float32Encoder: Encoder[Float] = apply[Float]
  implicit val Float64Encoder: Encoder[Double] = apply[Double]
  implicit val StringEncoder: Encoder[String] = apply[String]
  implicit val BoolEncoder: Encoder[Boolean] = apply[Boolean]
  implicit val TimeEncoder: Encoder[Instant] = apply[Instant]

  implicit def structEncoder[T <: Product : TypeTag]: Encoder[T] = apply[T]

  def apply[T: TypeTag]: Encoder[T] = cleanUpReflectionObjects {
    val tpe = typeTag[T].in(mirror).tpe
    val cls = getClassFromType(tpe).asInstanceOf[Class[T]]
    tpe.dealias match {
      case t if t <:< localTypeOf[Product] => new ProductEncoder(structTypeFor(tpe), cls)
      case t => new PrimitiveEncoder(dataTypeFor("<root>", t), cls)
    }
  }

  // Since we are creating a runtime mirror using the class loader of current thread,
  // we need to use def at here. So, every time we call mirror, it is using the
  // class loader of the current thread.
  def mirror: universe.Mirror = {
    universe.runtimeMirror(Thread.currentThread().getContextClassLoader)
  }

  import universe._

  private def structTypeFor(tpe: Type): StructType = cleanUpReflectionObjects {
    val cls = getClassFromType(tpe)
    if (cls.isMemberClass && !Modifier.isStatic(cls.getModifiers)) {
      throw new RuntimeException(s"Non-static inner class is not supported: ${getClassNameFromType(tpe)}")
    }
    val params = getConstructorParameters(tpe)
    val fields = params.map { case (fieldName, fieldType) =>
      fieldName -> dataTypeFor(s"${getClassNameFromType(tpe)}.$fieldName", fieldType)
    }
    StructType(fields)
  }

  private def dataTypeFor(path: => String, tpe: Type): DataType = cleanUpReflectionObjects {
    tpe.dealias match {
      case t if t <:< localTypeOf[java.lang.Integer] => DataType.Int32
      case t if t <:< localTypeOf[java.lang.Long] => DataType.Int64
      case t if t <:< localTypeOf[java.lang.Float] => DataType.Float32
      case t if t <:< localTypeOf[java.lang.Double] => DataType.Float64
      case t if t <:< localTypeOf[java.lang.Boolean] => DataType.Bool
      case t if t <:< localTypeOf[String] => DataType.String
      case t if t <:< localTypeOf[Int] => DataType.Int32
      case t if t <:< localTypeOf[Long] => DataType.Int64
      case t if t <:< localTypeOf[Float] => DataType.Float32
      case t if t <:< localTypeOf[Double] => DataType.Float64
      case t if t <:< localTypeOf[Boolean] => DataType.Bool
      case t if t <:< localTypeOf[org.joda.time.Instant] => DataType.Time
      case _ => throw new IllegalArgumentException(s"Unsupported Scala type at $path: $tpe")
    }
  }

  /**
   * Returns the full class name for a type. The returned name is the canonical
   * Scala name, where each component is separated by a period. It is NOT the
   * Java-equivalent runtime name (no dollar signs).
   *
   * In simple cases, both the Scala and Java names are the same, however when Scala
   * generates constructs that do not map to a Java equivalent, such as singleton objects
   * or nested classes in package objects, it uses the dollar sign ($) to create
   * synthetic classes, emulating behaviour in Java bytecode.
   */
  private def getClassNameFromType(tpe: `Type`): String = {
    tpe.dealias.erasure.typeSymbol.asClass.fullName
  }

  /**
   * Any codes calling `scala.reflect.api.Types.TypeApi.<:<` should be wrapped by this method to
   * clean up the Scala reflection garbage automatically. Otherwise, it will leak some objects to
   * `scala.reflect.runtime.JavaUniverse.undoLog`.
   *
   * @see https://github.com/scala/bug/issues/8302
   */
  private def cleanUpReflectionObjects[T](func: => T): T = {
    universe.asInstanceOf[scala.reflect.runtime.JavaUniverse].undoLog.undo(func)
  }

  /**
   * Return the Scala Type for `T` in the current classloader mirror.
   *
   * Use this method instead of the convenience method `universe.typeOf`, which
   * assumes that all types can be found in the classloader that loaded scala-reflect classes.
   * That's not necessarily the case when running using Eclipse launchers or even
   * Sbt console or test (without `fork := true`).
   *
   * @see SPARK-5281
   */
  private def localTypeOf[T: TypeTag]: `Type` = {
    val tag = implicitly[TypeTag[T]]
    tag.in(mirror).tpe.dealias
  }

  /**
   * Retrieves the runtime class corresponding to the provided type.
   */
  private def getClassFromType(tpe: Type): Class[_] = mirror.runtimeClass(tpe.dealias.typeSymbol.asClass)

  /**
   * Returns the parameter names and types for the primary constructor of this type.
   *
   * Note that it only works for scala classes with primary constructor, and currently doesn't
   * support inner class.
   */
  private def getConstructorParameters(tpe: Type): Seq[(String, Type)] = {
    val dealiasedTpe = tpe.dealias
    val formalTypeArgs = dealiasedTpe.typeSymbol.asClass.typeParams
    val TypeRef(_, _, actualTypeArgs) = dealiasedTpe
    val params = constructParams(dealiasedTpe)
    // if there are type variables to fill in, do the substitution (SomeClass[T] -> SomeClass[Int])
    if (actualTypeArgs.nonEmpty) {
      params.map { p =>
        p.name.decodedName.toString -> p.typeSignature.substituteTypes(formalTypeArgs, actualTypeArgs)
      }
    } else {
      params.map(p => p.name.decodedName.toString -> p.typeSignature)
    }
  }

  protected def constructParams(tpe: Type): Seq[Symbol] = {
    val constructorSymbol = tpe.dealias.member(termNames.CONSTRUCTOR)
    val params = if (constructorSymbol.isMethod) {
      constructorSymbol.asMethod.paramLists
    } else {
      // Find the primary constructor, and use its parameter ordering.
      val primaryConstructorSymbol = constructorSymbol
        .asTerm
        .alternatives
        .find(s => s.isMethod && s.asMethod.isPrimaryConstructor)
      if (primaryConstructorSymbol.isEmpty) {
        throw new RuntimeException("Product object did not have a primary constructor")
      } else {
        primaryConstructorSymbol.get.asMethod.paramLists
      }
    }
    params.flatten
  }
}
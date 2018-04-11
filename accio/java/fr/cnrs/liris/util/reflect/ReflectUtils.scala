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

package fr.cnrs.liris.util.reflect

import scala.reflect.ClassTag
import scala.reflect.api.{Mirror, TypeCreator, Universe}
import scala.reflect.runtime.universe._

/**
 * Helpers dealing with Scala reflection.
 */
object ReflectUtils {
  private[this] val classLoaderMirror = runtimeMirror(getClass.getClassLoader)

  def classForType(tpe: Type): Class[_] = classLoaderMirror.runtimeClass(tpe)

  /**
   * Returns a TypeTag in the current runtime universe for the supplied type.
   */
  def tagForType(tpe: Type): TypeTag[_] =
    TypeTag(
      classLoaderMirror,
      new TypeCreator {
        def apply[U <: Universe with Singleton](m: Mirror[U]): U#Type = tpe.asInstanceOf[U#Type]
      })

  def isSealed(tpe: Type): Boolean = isSealed(tpe.typeSymbol)

  def isSealed(symbol: Symbol): Boolean = {
    symbol.asInstanceOf[scala.reflect.internal.Symbols#Symbol].isSealed
  }

  def sealedDescendants(tpe: Type): Set[Symbol] = sealedDescendants(tpe.typeSymbol)

  def sealedDescendants(symbol: Symbol): Set[Symbol] = {
    val internal = symbol.asInstanceOf[scala.reflect.internal.Symbols#Symbol]
    if (internal.isSealed) {
      (internal.sealedDescendants.map(_.asInstanceOf[Symbol]) - symbol).flatMap(sealedDescendants)
    } else if (internal.isTrait || internal.isInterface) {
      throw new RuntimeException(s"Non-sealed interface $symbol")
    } else {
      Set(symbol)
    }
  }

  def reflectInstance[T: ClassTag](obj: T): InstanceMirror = classLoaderMirror.reflect(obj)

  def reflectClass(symbol: ClassSymbol): ClassMirror = classLoaderMirror.reflectClass(symbol)

  def reflectModule(symbol: ModuleSymbol): ModuleMirror = classLoaderMirror.reflectModule(symbol)

  def reflectCompanion(symbol: ClassSymbol): Any = {
    // The following line is a (partial) workaround around Scala bug 7567, unresolved at the time
    // of writing this code. It makes impossible to reflect an inner module if the outer object
    // has not been fully reflected before. The goal of this line is to force the reflection of
    // this outer object (if any). It might be inefficient, as this line will be evaluated each
    // time this method is called, though only needed once. But we prefer that than getting the
    // infamous "scala.ScalaReflectionException: <none> is not a module" exception. This is a
    // partial workaround as it only support a single level of nested objects.
    //
    // See https://github.com/scala/bug/issues/7567
    symbol.owner.typeSignature
    reflectModule(symbol.companion.asModule).instance
  }
}

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

package fr.cnrs.liris.common.flags

import com.google.inject.TypeLiteral
import net.codingwell.scalaguice.{ScalaModule, ScalaMultibinder}

object FlagsModule extends ScalaModule {
  override def configure(): Unit = {
    val converters = ScalaMultibinder.newSetBinder(binder, new TypeLiteral[Converter[_]] {})
    converters.addBinding.to[ByteConverter]
    converters.addBinding.to[ShortConverter]
    converters.addBinding.to[IntConverter]
    converters.addBinding.to[LongConverter]
    converters.addBinding.to[DoubleConverter]
    converters.addBinding.to[StringConverter]
    converters.addBinding.to[PathConverter]
    converters.addBinding.to[DurationConverter]
    converters.addBinding.to[BooleanConverter]
    converters.addBinding.to[TriStateConverter]
  }
}

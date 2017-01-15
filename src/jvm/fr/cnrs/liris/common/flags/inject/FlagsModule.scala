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

package fr.cnrs.liris.common.flags.inject

import com.google.inject.{AbstractModule, Key}
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.common.flags.FlagsProvider

/**
 * Guice module provisioning values of flags for parameters annotated with [[InjectFlag]].
 *
 * @param flagsProvider Flags provider.
 */
class FlagsModule(flagsProvider: FlagsProvider) extends AbstractModule with StrictLogging {
  override def configure(): Unit = {
    flagsProvider.asListOfEffectiveFlags.foreach { flag =>
      bind(flag.name, flag.value)
    }
  }

  private def bind(name: String, value: Any): Unit = {
    var classes = Set(value.getClass.asInstanceOf[Class[Any]])
    while (classes.nonEmpty) {
      classes.foreach(bind(name, _, value))
      classes = classes.flatMap(clazz => Set(clazz.getSuperclass) ++ clazz.getInterfaces)
        .map(_.asInstanceOf[Class[Any]])
        .filter(clazz => null != clazz && !FlagsModule.ignoredInterfaces.contains(clazz.getName))
    }
  }

  private def bind[T](name: String, clazz: Class[T], value: Any): Unit = {
    val key = Key.get(clazz, new InjectFlagImpl(name))
    logger.debug(s"Binding flag: $name (${clazz.getName}) = $value")
    binder.bind(key).toInstance(value.asInstanceOf[T])
  }
}

object FlagsModule {
  def apply(flagsProvider: FlagsProvider): FlagsModule = new FlagsModule(flagsProvider)

  private val ignoredInterfaces = Set(
    classOf[java.lang.Object].getName,
    classOf[java.lang.Comparable[_]].getName,
    classOf[java.lang.CharSequence].getName,
    classOf[java.io.Serializable].getName,
    classOf[scala.Serializable].getName,
    classOf[scala.Product].getName,
    classOf[scala.Equals].getName)
}
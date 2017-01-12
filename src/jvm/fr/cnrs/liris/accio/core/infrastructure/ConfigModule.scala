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

package fr.cnrs.liris.accio.core.infrastructure

import com.google.inject.TypeLiteral
import com.google.inject.matcher.Matchers
import com.google.inject.spi.{InjectionListener, TypeEncounter, TypeListener}
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.application.Configurable
import net.codingwell.scalaguice.ScalaModule

case class Config[T](clazz: Class[_ <: Configurable[T]], config: T)

object ConfigModule {
  def apply(configs: Config[_]*): ConfigModule = {
    new ConfigModule(configs.map(config => config.clazz.getName -> config.config).toMap)
  }
}

final class ConfigModule private(configs: Map[String, Any]) extends ScalaModule with StrictLogging {
  override def configure(): Unit = {
    bindListener(Matchers.any(), new ConfigurableTypeListener)
  }

  private class ConfigurableTypeListener extends TypeListener {
    override def hear[I](typeLiteral: TypeLiteral[I], encounter: TypeEncounter[I]): Unit = {
      val interfaces = typeLiteral.getRawType.getInterfaces.map(_.asInstanceOf[Class[Any]]) ++
        Option(typeLiteral.getRawType.getSuperclass).toArray.map(_.asInstanceOf[Class[Any]])
      if (interfaces.contains(classOf[Configurable[_]])) {
        encounter.register(new InitializeConfigurableListener[I])
      }
    }
  }

  private class InitializeConfigurableListener[I] extends InjectionListener[I] {
    override def afterInjection(injectee: I): Unit = {
      val configurable = injectee.asInstanceOf[Configurable[_]]
      initialize(configurable)
    }

    private def initialize[T](obj: Configurable[T]) = {
      configs.get(obj.getClass.getName) match {
        case None => throw new RuntimeException(s"No configuration for ${obj.getClass.getName}")
        case Some(config) =>
          logger.debug(s"Initializing ${obj.getClass.getSimpleName} with $config")
          obj.initialize(config.asInstanceOf[T])
      }
    }
  }

}
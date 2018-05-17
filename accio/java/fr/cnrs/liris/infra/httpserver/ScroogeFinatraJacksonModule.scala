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

package fr.cnrs.liris.infra.httpserver

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.twitter.finatra.json.modules.FinatraJacksonModule

/**
 * Guice module providing Jackson integration for Scrooge structures.
 *
 * There is a distinction here between [[ScroogeJacksonModule]], which is a Jackson module
 * (providing serializers and deserializers) and [[ScroogeFinatraJacksonModule]], which is a Guice
 * module providing bindings for an appropriately configured Jackson object mapper.
 */
object ScroogeFinatraJacksonModule extends FinatraJacksonModule {
  override val serializationInclusion = JsonInclude.Include.NON_ABSENT

  override val propertyNamingStrategy: PropertyNamingStrategy = PropertyNamingStrategy.LOWER_CAMEL_CASE

  override def additionalJacksonModules = Seq(ScroogeJacksonModule)
}

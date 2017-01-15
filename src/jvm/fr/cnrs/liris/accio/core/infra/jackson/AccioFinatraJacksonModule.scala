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

package fr.cnrs.liris.accio.core.infra.jackson

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.Module
import com.twitter.finatra.json.modules.FinatraJacksonModule

/**
 * Guice module providing Jackson integration.
 *
 * Be careful to distinguish between [[AccioJacksonModule]], which is a Jackson module (providing serializers and
 * deserializers) and [[AccioFinatraJacksonModule]], which is a Guice module (with additional Finatra integration)
 * providing bindings to the Jackson object mapper.
 */
object AccioFinatraJacksonModule extends FinatraJacksonModule {
  // We do not want to include None's inside JSON.
  override protected val serializationInclusion = JsonInclude.Include.NON_ABSENT

  override protected def additionalJacksonModules: Seq[Module] = Seq(AccioJacksonModule)
}
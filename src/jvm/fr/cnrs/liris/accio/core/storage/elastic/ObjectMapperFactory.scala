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

package fr.cnrs.liris.accio.core.storage.elastic

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, PropertyNamingStrategy}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.twitter.finatra.json.FinatraObjectMapper

/**
 * Factory to create a Finatra object mapper suitable to be used in Elasticsearch repositories.
 */
class ObjectMapperFactory {
  def create(): FinatraObjectMapper = {
    val mapper = new ObjectMapper with ScalaObjectMapper
    mapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
    mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    mapper.configure(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY, true)
    mapper.configure(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, true)

    mapper.setPropertyNamingStrategy(new PropertyNamingStrategy.SnakeCaseStrategy)
    mapper.registerModules(ElasticJacksonModule, DefaultScalaModule)

    new FinatraObjectMapper(mapper)
  }
}

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

import com.twitter.util.{Duration => TwitterDuration}

import scala.concurrent.duration.{Duration => ScalaDuration}

/**
 * Elastic storage module configuration, made available to Guice module.
 *
 * @param addr         Address(es) to Elasticsearch cluster members ("hostname:port[,hostname:port...]").
 * @param prefix       Prefix of indices managed by Accio.
 * @param queryTimeout Timeout of queries sent to Elasticsearch.
 */
case class ElasticStorageConfig(addr: String, prefix: String, queryTimeout: TwitterDuration) {
  def toConfig: StorageConfig = StorageConfig(prefix, ScalaDuration.fromNanos(queryTimeout.inNanoseconds))
}

/**
 * Elastic storage configuration, made available to repositories.
 *
 * @param prefix       Prefix of indices managed by Accio.
 * @param queryTimeout Timeout of queries sent to Elasticsearch.
 */
private[elastic] case class StorageConfig(prefix: String, queryTimeout: ScalaDuration)
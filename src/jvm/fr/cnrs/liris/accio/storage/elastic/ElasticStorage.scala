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

package fr.cnrs.liris.accio.storage.elastic

import com.google.inject.{Inject, Singleton}
import fr.cnrs.liris.accio.storage._

/**
 * Storage giving access Elasticsearch-based repositories.
 *
 * @param runs      Elasticsearch-based run repository.
 * @param workflows Elasticsearch-based workflow repository.
 * @param logs      Elasticsearch-based log repository.
 */
@Singleton
private[elastic] final class ElasticStorage @Inject()(
  override val runs: ElasticRunRepository,
  override val workflows: ElasticWorkflowRepository,
  override val logs: ElasticLogRepository)
  extends AbstractStorage
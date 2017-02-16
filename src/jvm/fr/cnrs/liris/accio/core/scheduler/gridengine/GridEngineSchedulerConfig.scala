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

package fr.cnrs.liris.accio.core.scheduler.gridengine

/**
 * Grid Engine scheduler module configuration, made available to Guice module.
 *
 * @param agentAddr    Agent address.
 * @param host         Host where to uploader files.
 * @param user         Username. Authentication is done by public key.
 * @param prefix       Prefix on remote host where to store files (under home directory).
 * @param executorUri  URI where to fetch the executor.
 * @param javaHome     Java home to be used remotely.
 * @param executorArgs Arguments to pass to the executors.
 */
case class GridEngineSchedulerConfig(
  agentAddr: String,
  host: String,
  user: String,
  prefix: String,
  executorUri: String,
  javaHome: Option[String],
  executorArgs: Seq[String]) {
  def toConfig: SchedulerConfig = SchedulerConfig(agentAddr, prefix, executorUri, javaHome, executorArgs)
}

/**
 * Grid Engine scheduler configuration, made available to scheduler.
 *
 * @param agentAddr    Agent address.
 * @param prefix       Prefix on remote host where to store files (under home directory).
 * @param executorUri  URI where to fetch the executor.
 * @param javaHome     Java home to be used remotely.
 * @param executorArgs Arguments to pass to the executors.
 */
private[gridengine] case class SchedulerConfig(
  agentAddr: String,
  prefix: String,
  executorUri: String,
  javaHome: Option[String],
  executorArgs: Seq[String])
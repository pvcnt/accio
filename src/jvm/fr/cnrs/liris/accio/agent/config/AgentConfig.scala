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

package fr.cnrs.liris.accio.agent.config

import java.nio.file.Path

import com.twitter.util.Duration
import fr.cnrs.liris.accio.core.domain.Resource

/**
 *
 * @param name    Worker name, unique among all workers (the hostname by default).
 * @param bind    Address on which to bind network services.
 * @param workDir Working directory where files will be stored.
 */
case class AgentConfig(
  name: String,
  bind: String,
  workDir: Path,
  client: Option[ClientConfig],
  master: Option[MasterConfig],
  worker: Option[WorkerConfig])

case class MasterConfig(masterPath: Option[String], rpcPort: Int, clusterName: String)

/**
 * Worker configuration.
 *
 * @param rpcPort      Port used by internal Thrift RPC.
 * @param reserved     Amount of resources that are reserved by the host and hence not available for scheduling.
 * @param executorUri  URI where to fetch the executor JAR.
 * @param javaHome     Java home to use when launching the executor JAR.
 * @param executorArgs Arguments to pass to the executor.
 */
case class WorkerConfig(
  rpcPort: Int,
  reserved: Resource,
  executorUri: String,
  javaHome: Option[String],
  executorArgs: Seq[String])

/**
 * Client configuration.
 *
 * @param masterAddr Master server address (as a Finagle name).
 */
case class ClientConfig(masterAddr: String)

object AgentConfig {
  val ExecutorTimeout: Duration = Duration.fromSeconds(60)
  val WorkerTimeout: Duration = Duration.fromSeconds(60)
}
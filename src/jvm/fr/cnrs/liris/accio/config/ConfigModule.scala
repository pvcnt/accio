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

package fr.cnrs.liris.accio.config

import java.nio.file.{Path, Paths}

import com.google.inject.TypeLiteral
import com.twitter.inject.TwitterModule
import fr.cnrs.liris.accio.api.thrift.Resource

object ConfigModule extends TwitterModule {
  private[this] val clusterNameFlag = flag("cluster_name", "default", "Cluster name")
  private[this] val datadirFlag = flag("datadir", "/var/lib/accio-agent", "Directory where to store local files")
  private[this] val executorUriFlag = flag[String]("executor_uri", "URI to the executor")
  private[this] val executorArgsFlag = flag("executor_args", "", "Additional arguments to the executor")

  override def configure(): Unit = {
    bind[String].annotatedWith[ClusterName].toInstance(clusterNameFlag())
    bind(new TypeLiteral[Seq[String]] {}).annotatedWith(classOf[ExecutorArgs]).toInstance {
      executorArgsFlag.get
        .map(_.split(" ").map(_.trim).filter(_.nonEmpty).toSeq)
        .getOrElse(Seq.empty)
    }
    bind[Path].annotatedWith[DataDir].toInstance(Paths.get(datadirFlag()))
    bind[String].annotatedWith[ExecutorUri].toInstance(executorUriFlag())
    bind[Resource].annotatedWith[ReservedResource].toInstance(Resource(0, 0, 0))
  }
}
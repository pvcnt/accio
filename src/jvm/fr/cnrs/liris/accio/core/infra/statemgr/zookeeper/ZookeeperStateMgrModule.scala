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

package fr.cnrs.liris.accio.core.infra.statemgr.zookeeper

import com.google.inject.{Provides, Singleton}
import com.twitter.util.Duration
import fr.cnrs.liris.accio.core.application.{Configurable, StateManager}
import net.codingwell.scalaguice.ScalaModule
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry

/**
 * Zookeeper state manager configuration.
 *
 * @param zkAddr            Address to Zookeeper cluster.
 * @param rootPath          Root path under which to store data.
 * @param sessionTimeout    Session timeout.
 * @param connectionTimeout Connection timeout.
 */
case class ZookeeperStateMgrConfig(
  zkAddr: String,
  rootPath: String,
  sessionTimeout: Duration = Duration.fromSeconds(60),
  connectionTimeout: Duration = Duration.fromSeconds(15))

/**
 * Guice module provisioning a state manager using Zookeeper.
 */
final class ZookeeperStateMgrModule extends ScalaModule with Configurable[ZookeeperStateMgrConfig] {
  override def configClass: Class[ZookeeperStateMgrConfig] = classOf[ZookeeperStateMgrConfig]

  override def configure(): Unit = {}

  @Singleton
  @Provides
  def providesStateManager(): StateManager = {
    val retryPolicy = new ExponentialBackoffRetry(1000, 3)
    val client = CuratorFrameworkFactory.newClient(
      config.zkAddr,
      config.sessionTimeout.inMillis.toInt,
      config.connectionTimeout.inMillis.toInt,
      retryPolicy)
    client.start()

    sys.addShutdownHook(client.close())

    new ZookeeperStateMgr(client, config.rootPath)
  }
}
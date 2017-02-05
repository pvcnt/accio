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

package fr.cnrs.liris.accio.core.statemgr.inject

import java.nio.file.Paths

import com.twitter.inject.TwitterModule
import com.twitter.util.Duration

/**
 * Guice module provisioning the [[fr.cnrs.liris.accio.core.statemgr.StateManager]] service.
 */
object StateManagerModule extends TwitterModule {
  private[this] val stateMgrFlag = flag("statemgr.type", "local", "State manager type")

  // Local state manager configuration.
  private[this] val localStateMgrPathFlag = flag[String]("statemgr.local.path", "Path where to store state")

  // Zookeeper state manager configuration.
  private[this] val zkStateMgrAddrFlag = flag("statemgr.zk.addr", "127.0.0.1:2181", "Address to Zookeeper cluster")
  private[this] val zkStateMgrPathFlag = flag("statemgr.zk.path", "/accio", "Root path in Zookeeper")
  private[this] val zkStateMgrSessionTimeoutFlag = flag("statemgr.zk.session_timeout", Duration.fromSeconds(60), "Zookeeper session timeout")
  private[this] val zkStateMgrConnTimeoutFlag = flag("statemgr.zk.conn_timeout", Duration.fromSeconds(15), "Zookeeper connection timeout")

  protected override def configure(): Unit = {
    val module = stateMgrFlag() match {
      case "local" =>
        new LocalStateMgrModule(LocalStateMgrConfig(Paths.get(localStateMgrPathFlag())))
      case "zk" =>
        val config = ZookeeperStateMgrConfig(zkStateMgrAddrFlag(), zkStateMgrPathFlag(), zkStateMgrSessionTimeoutFlag(), zkStateMgrConnTimeoutFlag())
        new ZookeeperStateMgrModule(config)
      case unknown => throw new IllegalArgumentException(s"Unknown state manager type: $unknown")
    }
    install(module)
  }
}

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

package fr.cnrs.liris.accio.logging

import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler

/**
 * Install the slf4j bridge, enabling to redirect calls to the [[java.util.logging]] classes to slf4j. It should be
 * included only once per application, typically in the class implementing the `main()` method.
 */
trait Slf4jBridgeInstaller {
  if (!SLF4JBridgeHandler.isInstalled && canInstallBridgeHandler) {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()
    LoggerFactory.getLogger(classOf[Slf4jBridgeInstaller]).info("org.slf4j.bridge.SLF4JBridgeHandler installed")
  }

  private def canInstallBridgeHandler: Boolean = {
    // We do not want to attempt to install the bridge handler if the JDK14LoggerFactory
    // exists on the classpath. See: http://www.slf4j.org/legacy.html#jul-to-slf4j
    try {
      Class.forName("org.slf4j.impl.JDK14LoggerFactory", false, this.getClass.getClassLoader)
      LoggerFactory.getLogger(classOf[Slf4jBridgeInstaller])
        .warn("Detected [org.slf4j.impl.JDK14LoggerFactory] on classpath. SLF4JBridgeHandler cannot be installed, see: http://www.slf4j.org/legacy.html#jul-to-slf4j")
      false
    } catch {
      case _: ClassNotFoundException => true
    }
  }
}
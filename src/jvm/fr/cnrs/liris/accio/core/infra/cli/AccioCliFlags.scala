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

package fr.cnrs.liris.accio.core.infra.cli

import java.nio.file.Path

import fr.cnrs.liris.common.flags.Flag

case class AccioCliFlags(
  @Flag(name = "logging", help = "Logging level")
  logLevel: String = "warn",
  @Flag(name = "color", help = "Enable or disable colored output")
  color: Boolean = true,
  @Flag(name = "rc", help = "Path to the .acciorc configuration file")
  accioRcPath: Option[Path],
  @Flag(name = "config", help = "Name of the configuration to use, conjointly with the .acciorc configuration file")
  accioRcConfig: Option[String])
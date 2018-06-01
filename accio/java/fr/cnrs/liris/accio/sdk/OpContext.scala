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

package fr.cnrs.liris.accio.sdk

import java.nio.file.Path

/**
 * Execution context of an operator.
 *
 * @param _seed    Seed used by an unstable operator, if it is the case.
 * @param workDir  Working directory where temporary data can be written. It may be removed after
 *                 the operator completes.
 */
final class OpContext(_seed: Option[Long], val workDir: Path) {
  /**
   * Return whether this context contains a seed.
   */
  def hasSeed: Boolean = _seed.isDefined

  /**
   * Return the seed to use for an unstable operator.
   *
   * @throws IllegalStateException If the operator is not declared as unstable.
   */
  def seed: Long = _seed match {
    case None => throw new IllegalStateException("No seed is available")
    case Some(s) => s
  }
}
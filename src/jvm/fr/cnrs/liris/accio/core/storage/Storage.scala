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

package fr.cnrs.liris.accio.core.storage

import com.google.common.util.concurrent.Service

/**
 * A storage service is an interface to access repositories. Repositories should never be accessed directly (e.g.,
 * they should not be available through Guice injection), but instead used through their storage. It gives a higher
 * level control over repositories, notably including support for transactions.
 */
trait Storage extends Service {
  /**
   * Read data from underlying repositories.
   *
   * @param fn Operations to execute with repositories.
   * @tparam T Type of result.
   * @return Operations result.
   */
  def read[T](fn: RepositoryProvider => T): T

  /**
   * Write (and read) data to underlying repositories.
   *
   * @param fn Operations to execute with repositories.
   * @tparam T Type of result.
   * @return Operations result.
   */
  def write[T](fn: MutableRepositoryProvider => T): T
}

/**
 * Repository provider giving access to read-only repositories.
 */
trait RepositoryProvider {
  /**
   * Return a read-only run repository.
   */
  def runs: RunRepository

  /**
   * Return a read-only workflow repository.
   */
  def workflows: WorkflowRepository

  /**
   * Return a read-only log repository.
   */
  def logs: LogRepository
}

/**
 * Repository provider giving access to read/write repositories.
 */
trait MutableRepositoryProvider {
  /**
   * Return a read/write run repository.
   */
  def runs: MutableRunRepository

  /**
   * Return a read/write workflow repository.
   */
  def workflows: MutableWorkflowRepository

  /**
   * Return a read/write log repository.
   */
  def logs: MutableLogRepository
}
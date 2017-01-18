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

package fr.cnrs.liris.accio.core.infra.storage.local

import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.infra.storage.RunRepositorySpec
import fr.cnrs.liris.testing.WithTmpDirectory

/**
 * Unit tests of [[LocalRunRepository]].
 */
class LocalRunRepositorySpec extends RunRepositorySpec with WithTmpDirectory {
  override protected def createRepository: RunRepository = new LocalRunRepository(tmpDir)

  behavior of "LocalRunRepository"
}
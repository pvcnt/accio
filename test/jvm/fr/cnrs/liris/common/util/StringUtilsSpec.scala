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

package fr.cnrs.liris.common.util

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[StringUtils]].
 */
class StringUtilsSpec extends UnitSpec {
  "TextUtils::paragraphFill" should "wrap text into lines of fixed length" in {
    val str = StringUtils.paragraphFill("In sit amet vehicula lacus, ut dictum amet.", width = 10)
    str shouldBe "In sit \namet \nvehicula \nlacus, ut \ndictum \namet."
  }

  /*it should "wrap too-long text" in {
    val str = TextUtils.paragraphFill("Insitametvehicula lacus,utdictumamet.", width = 10)
    str shouldBe "Insitametvehicula \nlacus,utdictumamet."
  }*/

  it should "wrap an empty string into itself" in {
    val str = StringUtils.paragraphFill("", width = 10)
    str shouldBe ""
  }

  it should "wrap text and preserve line breaks" in {
    val str = StringUtils.paragraphFill("In sit amet vehicula lacus,\nut dictum amet.", width = 10)
    str shouldBe "In sit \namet \nvehicula \nlacus,\nut dictum \namet."
  }
}

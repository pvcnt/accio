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

package fr.cnrs.liris.common.cli

import java.nio.file.Files

import fr.cnrs.liris.testing.UnitSpec
import scala.collection.JavaConverters._

/**
 * Unit tests for [[AccioRcParser]].
 */
class AccioRcParserSpec extends UnitSpec {
  behavior of "AccioRcParser"

  it should "parse an rcfile" in {
    val file = Files.createTempFile("accio-test-", ".txt")
    Files.write(file, Seq(
      "run a=b c=d",
      "run e=f c=c",
      "export a=c").asJava)
    val parser = new AccioRcParser
    val args = parser.parse(Some(file), None, "run")
    args shouldBe Seq("a=b", "c=d", "e=f", "c=c")
  }

  it should "parse an rcfile with named config" in {
    val file = Files.createTempFile("accio-test-", ".txt")
    Files.write(file, Seq(
      "run a=b c=d",
      "run:foo e=f1 c=c1",
      "run:bar e=f2 c=c2",
      "export a=c").asJava)
    val parser = new AccioRcParser
    val args = parser.parse(Some(file), Some("bar"), "run")
    args shouldBe Seq("a=b", "c=d", "e=f2", "c=c2")
  }
}
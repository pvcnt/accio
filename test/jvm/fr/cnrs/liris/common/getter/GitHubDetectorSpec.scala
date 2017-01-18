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

package fr.cnrs.liris.common.getter

import java.nio.file.Paths

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[GitHubDetector]].
 */
class GitHubDetectorSpec extends UnitSpec {
  private[this] val detector = new GitHubDetector

  behavior of "GitHubDetector"

  it should "detect HTTP-based URIs" in {
    assertOk("github.com/hashicorp/foo", "https://github.com/hashicorp/foo.git")
    assertOk("github.com/hashicorp/foo.git", "https://github.com/hashicorp/foo.git")
    assertOk("github.com/hashicorp/foo/bar", "https://github.com/hashicorp/foo.git", Some("bar"))
    assertOk("github.com/hashicorp/foo?foo=bar", "https://github.com/hashicorp/foo.git?foo=bar")
    assertOk("github.com/hashicorp/foo.git?foo=bar", "https://github.com/hashicorp/foo.git?foo=bar")
    assertOk("github.com/hashicorp/foo/bar?foo=bar", "https://github.com/hashicorp/foo.git?foo=bar", Some("bar"))
    assertOk("github.com/hashicorp/foo.git/bar?foo=bar", "https://github.com/hashicorp/foo.git?foo=bar", Some("bar"))
  }

  it should "detect SSH-based URIs" in {
    assertOk("git@github.com:hashicorp/foo.git", "ssh://git@github.com/hashicorp/foo.git")
    assertOk("git@github.com:hashicorp/foo.git//bar", "ssh://git@github.com/hashicorp/foo.git", Some("bar"))
    assertOk("git@github.com:hashicorp/foo.git?foo=bar", "ssh://git@github.com/hashicorp/foo.git?foo=bar")
    assertOk("git@github.com:hashicorp/foo.git//bar?foo=bar", "ssh://git@github.com/hashicorp/foo.git?foo=bar", Some("bar"))
  }

  private def assertOk(in: String, rawUri: String, subdir: Option[String] = None) = {
    var uri = detector.detect(in, Some(Paths.get("/tmp")))
    uri should not be empty
    uri.get.getter shouldBe Some("git")
    uri.get.rawUri.toString shouldBe rawUri
    uri.get.subdir shouldBe subdir

    uri = detector.detect(in, None)
    uri should not be empty
    uri.get.getter shouldBe Some("git")
    uri.get.rawUri.toString shouldBe rawUri
    uri.get.subdir shouldBe subdir
  }
}

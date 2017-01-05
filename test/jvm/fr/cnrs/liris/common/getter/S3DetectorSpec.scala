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

package fr.cnrs.liris.common.getter

import java.nio.file.Paths

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[S3Detector]].
 */
class S3DetectorSpec extends UnitSpec {
  private[this] val detector = new S3Detector

  behavior of "S3Detector"

  it should "detect virtual host style URIs" in {
    assertOk("bucket.s3.amazonaws.com/foo", "https://s3.amazonaws.com/bucket/foo")
    assertOk("bucket.s3.amazonaws.com/foo?v=1234", "https://s3.amazonaws.com/bucket/foo?v=1234")
    assertOk("bucket.s3.amazonaws.com/foo/bar", "https://s3.amazonaws.com/bucket/foo/bar")
    assertOk("bucket.s3.amazonaws.com/foo/bar?v=1234", "https://s3.amazonaws.com/bucket/foo/bar?v=1234")
    assertOk("bucket.s3.amazonaws.com/foo/bar.baz", "https://s3.amazonaws.com/bucket/foo/bar.baz")
    assertOk("bucket.s3.amazonaws.com/foo/bar.baz?v=1234", "https://s3.amazonaws.com/bucket/foo/bar.baz?v=1234")
    assertOk("bucket.s3-eu-west-1.amazonaws.com/foo", "https://s3-eu-west-1.amazonaws.com/bucket/foo")
    assertOk("bucket.s3-eu-west-1.amazonaws.com/foo?v=1234", "https://s3-eu-west-1.amazonaws.com/bucket/foo?v=1234")
    assertOk("bucket.s3-eu-west-1.amazonaws.com/foo/bar", "https://s3-eu-west-1.amazonaws.com/bucket/foo/bar")
    assertOk("bucket.s3-eu-west-1.amazonaws.com/foo/bar?v=1234", "https://s3-eu-west-1.amazonaws.com/bucket/foo/bar?v=1234")
    assertOk("bucket.s3-eu-west-1.amazonaws.com/foo/bar.baz", "https://s3-eu-west-1.amazonaws.com/bucket/foo/bar.baz")
    assertOk("bucket.s3-eu-west-1.amazonaws.com/foo/bar.baz?v=1234", "https://s3-eu-west-1.amazonaws.com/bucket/foo/bar.baz?v=1234")
  }

  it should "detect path style URIs" in {
    assertOk("s3.amazonaws.com/bucket/foo", "https://s3.amazonaws.com/bucket/foo")
    assertOk("s3.amazonaws.com/bucket/foo?v=1234", "https://s3.amazonaws.com/bucket/foo?v=1234")
    assertOk("s3.amazonaws.com/bucket/foo/bar", "https://s3.amazonaws.com/bucket/foo/bar")
    assertOk("s3.amazonaws.com/bucket/foo/bar?v=1234", "https://s3.amazonaws.com/bucket/foo/bar?v=1234")
    assertOk("s3.amazonaws.com/bucket/foo/bar.baz", "https://s3.amazonaws.com/bucket/foo/bar.baz")
    assertOk("s3.amazonaws.com/bucket/foo/bar.baz?v=1234", "https://s3.amazonaws.com/bucket/foo/bar.baz?v=1234")
    assertOk("s3-eu-west-1.amazonaws.com/bucket/foo", "https://s3-eu-west-1.amazonaws.com/bucket/foo")
    assertOk("s3-eu-west-1.amazonaws.com/bucket/foo?v=1234", "https://s3-eu-west-1.amazonaws.com/bucket/foo?v=1234")
    assertOk("s3-eu-west-1.amazonaws.com/bucket/foo/bar", "https://s3-eu-west-1.amazonaws.com/bucket/foo/bar")
    assertOk("s3-eu-west-1.amazonaws.com/bucket/foo/bar?v=1234", "https://s3-eu-west-1.amazonaws.com/bucket/foo/bar?v=1234")
    assertOk("s3-eu-west-1.amazonaws.com/bucket/foo/bar.baz", "https://s3-eu-west-1.amazonaws.com/bucket/foo/bar.baz")
    assertOk("s3-eu-west-1.amazonaws.com/bucket/foo/bar.baz?v=1234", "https://s3-eu-west-1.amazonaws.com/bucket/foo/bar.baz?v=1234")
  }

  private def assertOk(in: String, rawUri: String) = {
    var uri = detector.detect(in, Some(Paths.get("/tmp")))
    uri should not be empty
    uri.get.getter shouldBe Some("s3")
    uri.get.rawUri.toString shouldBe rawUri
    uri.get.subdir shouldBe None

    uri = detector.detect(in, None)
    uri should not be empty
    uri.get.getter shouldBe Some("s3")
    uri.get.rawUri.toString shouldBe rawUri
    uri.get.subdir shouldBe None
  }
}

/*
 * Accio is a program whose purpose is to study location privacy.
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

package fr.cnrs.liris.accio.ops.sparkle

import fr.cnrs.liris.testing.UnitSpec
import org.scalatest.BeforeAndAfter

import scala.collection.mutable
import scala.reflect._

/**
 * Unit tests for [[DataFrame]].
 */
class DataFrameSpec extends UnitSpec with BeforeAndAfter {
  private[this] var env: SparkleEnv = null

  before {
    env = new SparkleEnv(level = 1)
  }

  after {
    env.stop()
    env = null
  }

  behavior of "DataFrame"

  // Basic/meta operations.
  it should "return its elements' type" in {
    val data = env.parallelize("foo" -> Seq(1, 2, 3), "bar" -> Seq(4, 5))
    data.elementClassTag shouldBe classTag[Int]
  }

  it should "return its keys" in {
    val data = env.parallelize("foo" -> Seq(1, 2, 3), "bar" -> Seq(4, 5))
    data.keys shouldBe Seq("foo", "bar")
  }

  it should "load a single key" in {
    val data = env.parallelize("foo" -> Seq(1, 2, 3), "bar" -> Seq(4, 5))
    data.load("foo").toSeq shouldBe Seq(1, 2, 3)
    data.load("bar").toSeq shouldBe Seq(4, 5)
  }

  // Transformation operations.
  it should "map elements" in {
    val data = env.parallelize("foo" -> Seq(1, 2, 3), "bar" -> Seq(4, 5))
    data.map(_ * 2).toArray shouldBe Array(2, 4, 6, 8, 10)
  }

  it should "flatMap elements" in {
    val data = env.parallelize("foo" -> Seq(1, 2, 3), "bar" -> Seq(4, 5))
    data.flatMap(i => Set(i, i * 2)).toArray shouldBe Array(1, 2, 2, 4, 3, 6, 4, 8, 5, 10)
  }

  it should "filter elements" in {
    val data = env.parallelize("foo" -> Seq(1, 2, 3), "bar" -> Seq(4, 5))
    data.filter(i => (i % 2) == 0).toArray shouldBe Array(2, 4)
  }

  it should "restrict keys" in {
    val data = env.parallelize("foo" -> Seq(1, 2), "bar" -> Seq(3), "foobar" -> Seq(4), "barfoo" -> Seq(5, 6, 7))
    data.restrict(Set("foo", "bar")).toArray shouldBe Array(1, 2, 3)
    data.restrict(Set("barfoo")).toArray shouldBe Array(5, 6, 7)
    data.restrict(Set("barfoo", "invalid_key")).toArray shouldBe Array(5, 6, 7)
    data.restrict(Set("invalid_key")).toArray shouldBe Array.empty[Int]
    data.restrict(Set("foo", "bar")).load("invalid_key") shouldBe Iterator.empty
  }

  it should "zip with another dataset with same keys and same size" in {
    val data1 = env.parallelize("foo" -> Seq(1, 2, 3), "bar" -> Seq(4, 5))
    val data2 = env.parallelize("foo" -> Seq(2, 4, 6), "bar" -> Seq(8, 10))
    data1.zip(data2).toArray shouldBe Array((1, 2), (2, 4), (3, 6), (4, 8), (5, 10))
  }

  it should "zip with another dataset with different keys" in {
    val data1 = env.parallelize("foo" -> Seq(1, 2, 3), "foobar" -> Seq(4))
    val data2 = env.parallelize("bar" -> Seq(8, 10), "foobar" -> Seq(8))
    data1.zip(data2).toArray shouldBe Array((4, 8))
  }

  it should "zip with another dataset with different size" in {
    val data1 = env.parallelize("foo" -> Seq(1, 2, 3), "bar" -> Seq(3))
    val data2 = env.parallelize("foo" -> Seq(2, 4), "bar" -> Seq(6, 23))
    data1.zip(data2).toArray shouldBe Array((1, 2), (2, 4), (3, 6))
  }

  // Terminal operations.
  it should "count elements" in {
    val data = env.parallelize("foo" -> Seq(1, 2, 3), "bar" -> Seq(4, 5))
    data.count() shouldBe 5
  }

  it should "return first element" in {
    val data = env.parallelize("foo" -> Seq(1, 2, 3), "bar" -> Seq(4, 5))
    data.first() shouldBe 1
  }

  it should "return its elements in order" in {
    val data = env.parallelize("foo" -> Seq(1, 2, 3), "bar" -> Seq(4, 5))
    data.toArray shouldBe Array(1, 2, 3, 4, 5)
  }

  it should "return its maximum w.r.t. implicit ordering" in {
    val data = env.parallelize("foo" -> Seq(3, 1, 2), "bar" -> Seq(5, 4))
    data.max shouldBe 5
  }

  it should "return its minimum w.r.t. implicit ordering" in {
    val data = env.parallelize("foo" -> Seq(3, 1, 2), "bar" -> Seq(5, 4))
    data.min shouldBe 1
  }

  it should "apply an operation on each element" in {
    val data = env.parallelize("foo" -> Seq(1, 2, 3), "bar" -> Seq(4, 5))
    val res = mutable.Set.empty[Int]
    data.foreach { i =>
      res synchronized {
        res += i
      }
    }
    res shouldBe Set(1, 2, 3, 4, 5)
  }
}
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

package fr.cnrs.liris.sparkle

import java.util.concurrent.ConcurrentHashMap

import fr.cnrs.liris.testing.UnitSpec
import org.scalatest.BeforeAndAfterEach

import scala.collection.mutable
import scala.collection.JavaConverters._

/**
 * Unit tests for [[DataFrame]].
 */
class DataFrameSpec extends UnitSpec with BeforeAndAfterEach {
  behavior of "DataFrame"

  private[this] var env: SparkleEnv = _

  override def beforeEach(): Unit = {
    super.beforeEach()
    env = new SparkleEnv(parallelism = 1)
  }

  override def afterEach(): Unit = {
    env.stop()
    env = null
    super.afterEach()
  }

  it should "map elements" in {
    val data = env.parallelize(1, 2, 3, 4, 5)
    data.map(_ * 2).collect() shouldBe Array(2, 4, 6, 8, 10)
  }

  it should "flatMap elements" in {
    val data = env.parallelize(1, 2, 3, 4, 5)
    data.flatMap(i => Set(i, i * 2)).collect() shouldBe Array(1, 2, 2, 4, 3, 6, 4, 8, 5, 10)
  }

  it should "filter elements" in {
    val data = env.parallelize(1, 2, 3, 4, 5)
    data.filter(i => (i % 2) == 0).collect() shouldBe Array(2, 4)
  }

  it should "zip with another dataset with same keys and same size" in {
    val data1 = env.parallelize("aa", "ab", "cd", "ef", "ag").groupBy(_.substring(0, 1))
    val data2 = env.parallelize("aa", "ca", "bd", "af").groupBy(_.substring(0, 1))
    val collect = new ConcurrentHashMap[String, Seq[String]].asScala
    data1.join(data2) { case (k, v1, v2) =>
      collect.put(k, (v1 ++ v2).toSeq)
      v1 ++ v2
    }.collect() shouldBe Array("ef", "aa", "ab", "ag", "aa", "af", "cd", "ca")
    collect.toSeq should contain theSameElementsAs Seq("a" -> Seq("aa", "ab", "ag", "aa", "af"), "c" -> Seq("cd", "ca"), "e" -> Seq("ef"))
  }

  it should "count elements" in {
    val data = env.parallelize(1, 2, 3, 4, 5)
    data.count() shouldBe 5
  }

  it should "return first element" in {
    val data = env.parallelize(1, 2, 3, 4, 5)
    data.first() shouldBe 1
  }

  it should "return its elements in order" in {
    val data = env.parallelize(1, 2, 3, 4, 5)
    data.collect() shouldBe Array(1, 2, 3, 4, 5)
  }

  it should "apply an operation on each element" in {
    val data = env.parallelize(1, 2, 3, 4, 5)
    val res = mutable.Set.empty[Int]
    data.foreach { i =>
      res synchronized {
        res += i
      }
    }
    res shouldBe Set(1, 2, 3, 4, 5)
  }
}
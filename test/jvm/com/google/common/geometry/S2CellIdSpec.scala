/*
 * Copyright 2005 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.common.geometry

import com.google.common.geometry.testing.GeometryHelpers
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.testing.UnitSpec

import scala.annotation.strictfp
import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 */
@strictfp
class S2CellIdSpec extends UnitSpec with StrictLogging with GeometryHelpers {
  val eps = 1e-9

  private def getCellId(latDegrees: Double, lngDegrees: Double): S2CellId = {
    val id = S2CellId.fromLatLng(S2LatLng.fromDegrees(latDegrees, lngDegrees))
    logger.info(java.lang.Long.toString(id.id(), 16))
    id
  }

  "S2CellId" should "have a default constructor" in {
    val id = new S2CellId()
    id.id shouldBe 0
    id.isValid shouldBe false
  }

  it should "have a constructor" in {
    // Check basic accessor methods.
    val id = S2CellId.fromFacePosLevel(3, 0x12345678, S2CellId.MAX_LEVEL - 4)
    id.isValid shouldBe true
    id.face shouldBe 3
    // assertEquals(id.pos, 0x12345700);
    id.level shouldBe S2CellId.MAX_LEVEL - 4
    id.isLeaf shouldBe false

    // Check face definitions
    getCellId(0, 0).face shouldBe 0
    getCellId(0, 90).face shouldBe 1
    getCellId(90, 0).face shouldBe 2
    getCellId(0, 180).face shouldBe 3
    getCellId(0, -90).face shouldBe 4
    getCellId(-90, 0).face shouldBe 5

    // Check parent/child relationships.
    id.childBegin(id.level + 2).pos shouldBe 0x12345610
    id.childBegin().pos shouldBe 0x12345640
    id.parent().pos shouldBe 0x12345400
    id.parent(id.level - 2).pos shouldBe 0x12345000

    // Check ordering of children relative to parents.
    id.childBegin().lessThan(id) shouldBe true
    id.childEnd().greaterThan(id) shouldBe true
    id.childBegin().next().next().next().next() shouldBe id.childEnd()
    id.childBegin(S2CellId.MAX_LEVEL) shouldBe id.rangeMin()
    id.childEnd(S2CellId.MAX_LEVEL) shouldBe id.rangeMax().next()

    // Check wrapping from beginning of Hilbert curve to end and vice versa.
    S2CellId.begin(0).prevWrap() shouldBe S2CellId.end(0).prev()

    S2CellId.begin(S2CellId.MAX_LEVEL).prevWrap() shouldBe S2CellId.fromFacePosLevel(5, ~0L >>> S2CellId.FACE_BITS, S2CellId.MAX_LEVEL)

    S2CellId.end(4).prev().nextWrap() shouldBe S2CellId.begin(4)
    S2CellId.end(S2CellId.MAX_LEVEL).prev().nextWrap() shouldBe S2CellId.fromFacePosLevel(0, 0, S2CellId.MAX_LEVEL)

    // Check that cells are represented by the position of their center
    // along the Hilbert curve.
    id.rangeMin().id() + id.rangeMax().id() shouldBe 2 * id.id()
  }

  it should "have inverses" in {
    // Check the conversion of random leaf cells to S2LatLngs and back.
    for (i <- 0 until 200000) {
      val id = randomCellId(S2CellId.MAX_LEVEL)
      id.isLeaf shouldBe true
      id.level shouldBe S2CellId.MAX_LEVEL
      val center = id.toLatLng
      S2CellId.fromLatLng(center).id() shouldBe id.id()
    }
  }

  it should "be convertible to a token" in {
    new S2CellId(266).toToken shouldBe "000000000000010a"
    new S2CellId(-9185834709882503168L).toToken shouldBe "80855c"
  }

  it should "be constructed from a token" in {
    // Test random cell ids at all levels.
    for (i <- 0 until 10000) {
      val id = randomCellId
      if (id.isValid) {
        val token = id.toToken
        token.length() shouldBe <=(16)
        S2CellId.fromToken(token) shouldBe id
      }
    }
    // Check that invalid cell ids can be encoded.
    val token = S2CellId.none().toToken
    S2CellId.fromToken(token) shouldBe S2CellId.none()
  }

  private val kMaxExpandLevel = 3

  private def expandCell(parent: S2CellId, cells: scala.collection.mutable.ListBuffer[S2CellId],
    parentMap: scala.collection.mutable.Map[S2CellId, S2CellId]): Unit = {
    cells += parent
    if (parent.level == kMaxExpandLevel) {
      return
    }
    val i = new MutableInteger(0)
    val j = new MutableInteger(0)
    val orientation = new MutableInteger(0)
    val face = parent.toFaceIJOrientation(i, j, orientation)
    face shouldBe parent.face

    var pos = 0
    var child = parent.childBegin()
    while (child != parent.childEnd()) {
      // Do some basic checks on the children
      child.level shouldBe parent.level + 1
      child.isLeaf shouldBe false
      val childOrientation = new MutableInteger(0)
      child.toFaceIJOrientation(i, j, childOrientation) shouldBe face
      childOrientation.intValue() shouldBe orientation.intValue() ^ S2.posToOrientation(pos)
      parentMap(child) = parent
      expandCell(child, cells, parentMap)
      pos += 1
      child = child.next()
    }
  }

  it should "test containment" in {
    val parentMap = mutable.Map.empty[S2CellId, S2CellId]
    val cells = mutable.ListBuffer.empty[S2CellId]
    for (face <- 0 until 6) {
      expandCell(S2CellId.fromFacePosLevel(face, 0, 0), cells, parentMap)
    }
    for (i <- cells.indices) {
      for (j <- cells.indices) {
        var contained = true
        var id = cells(j)
        while (contained && id != cells(i)) {
          if (!parentMap.contains(id)) {
            contained = false
          } else {
            id = parentMap(id)
          }
        }
        cells(i).contains(cells(j)) shouldBe contained
        (cells(j).greaterOrEquals(cells(i).rangeMin()) && cells(j).lessOrEquals(cells(i).rangeMax())) shouldBe contained
        cells(i).intersects(cells(j)) shouldBe (cells(i).contains(cells(j)) || cells(j).contains(cells(i)))
      }
    }
  }

  private val MAX_WALK_LEVEL = 8

  it should "test continuity" in {
    // Make sure that sequentially increasing cell ids form a continuous
    // path over the surface of the sphere, i.e. there are no
    // discontinuous jumps from one region to another.

    val maxDist = S2Projections.MAX_EDGE.getValue(MAX_WALK_LEVEL)
    val end = S2CellId.end(MAX_WALK_LEVEL)
    var id = S2CellId.begin(MAX_WALK_LEVEL)
    while (id != end) {
      id.toPointRaw.angle(id.nextWrap().toPointRaw) shouldBe <=(maxDist)

      // Check that the ToPointRaw() returns the center of each cell
      // in (s,t) coordinates.
      val p = id.toPointRaw
      val face = S2Projections.xyzToFace(p)
      val uv = S2Projections.validFaceXyzToUv(face, p)
      math.IEEEremainder(S2Projections.uvToST(uv.x()), 1.0 / (1 << MAX_WALK_LEVEL)) shouldBe (0d +- eps)
      math.IEEEremainder(S2Projections.uvToST(uv.y()), 1.0 / (1 << MAX_WALK_LEVEL)) shouldBe (0d +- eps)
      id = id.next()
    }
  }

  it should "test coverage" in {
    // Make sure that random points on the sphere can be represented to the
    // expected level of accuracy, which in the worst case is sqrt(2/3) times
    // the maximum arc length between the points on the sphere associated with
    // adjacent values of "i" or "j". (It is sqrt(2/3) rather than 1/2 because
    // the cells at the corners of each face are stretched -- they have 60 and
    // 120 degree angles.)

    val maxDist = 0.5 * S2Projections.MAX_DIAG.getValue(S2CellId.MAX_LEVEL)
    for (i <- 0 until 1000000) {
      // randomPoint();
      val p = new S2Point(0.37861576725894824, 0.2772406863275093, 0.8830558887338725)
      val q = S2CellId.fromPoint(p).toPointRaw
      p.angle(q) shouldBe <=(maxDist)
    }
  }

  private def testAllNeighbors(id: S2CellId, level: Int) = {
    level shouldBe >=(id.level)
    level shouldBe <(S2CellId.MAX_LEVEL)

    // We compute GetAllNeighbors, and then add in all the children of "id"
    // at the given level. We then compare this against the result of finding
    // all the vertex neighbors of all the vertices of children of "id" at the
    // given level. These should give the same result.
    val all = mutable.ListBuffer.empty[S2CellId]
    val expected = mutable.ListBuffer.empty[S2CellId]
    id.getAllNeighbors(level, all.asJava)
    val end = id.childEnd(level + 1)
    var c = id.childBegin(level + 1)
    while (c != end) {
      all += c.parent()
      c.getVertexNeighbors(level, expected.asJava)
      c = c.next
    }
    // Sort the results and eliminate duplicates.
    all.sorted.distinct should contain theSameElementsInOrderAs expected.sorted.distinct
  }

  it should "test neighbors" in {
    // Check the edge neighbors of face 1.
    val outFaces = Array(5, 3, 2, 0)
    val faceNbrs = Array.ofDim[S2CellId](4)
    S2CellId.fromFacePosLevel(1, 0, 0).getEdgeNeighbors(faceNbrs)
    for (i <- 0 until 4) {
      faceNbrs(i).isFace shouldBe true
      faceNbrs(i).face shouldBe outFaces(i)
    }

    // Check the vertex neighbors of the center of face 2 at level 5.
    var nbrs = mutable.ListBuffer.empty[S2CellId]
    S2CellId.fromPoint(new S2Point(0, 0, 1)).getVertexNeighbors(5, nbrs.asJava)
    nbrs = nbrs.sorted
    for (i <- 0 until 4) {
      nbrs(i) shouldBe S2CellId.fromFaceIJ(2, (1 << 29) - (if (i < 2) 1 else 0), (1 << 29) - (if (i == 0 || i == 3) 1 else 0)).parent(5)
    }
    nbrs.clear()

    // Check the vertex neighbors of the corner of faces 0, 4, and 5.
    val id = S2CellId.fromFacePosLevel(0, 0, S2CellId.MAX_LEVEL)
    id.getVertexNeighbors(0, nbrs.asJava)
    nbrs = nbrs.sorted
    nbrs.size shouldBe 3
    nbrs(0) shouldBe S2CellId.fromFacePosLevel(0, 0, 0)
    nbrs(1) shouldBe S2CellId.fromFacePosLevel(4, 0, 0)
    nbrs(2) shouldBe S2CellId.fromFacePosLevel(5, 0, 0)

    // Check that GetAllNeighbors produces results that are consistent
    // with GetVertexNeighbors for a bunch of random cells.
    for (i <- 0 until 1000) {
      var id1 = randomCellId
      if (id1.isLeaf) {
        id1 = id1.parent()
      }
      // TestAllNeighbors computes approximately 2**(2*(diff+1)) cell id1s,
      // so it's not reasonable to use large values of "diff".
      val maxDiff = math.min(6, S2CellId.MAX_LEVEL - id1.level - 1)
      val level = id1.level + random(maxDiff)
      testAllNeighbors(id1, level)
    }
  }
}

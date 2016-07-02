package com.google.common.geometry.testing

import com.google.common.base.Splitter
import com.google.common.geometry.{S2Polygon, S2Polyline, _}

import scala.collection.JavaConverters._
import scala.util.Random

trait GeometryHelpers {
  private[this] val rand = new Random(123456)

  protected def samplePoint(cap: S2Cap): S2Point = {
    val z = cap.axis
    val x = z.ortho
    val y = S2Point.crossProd(z, x)
    val h = rand.nextDouble * cap.height
    val theta = 2 * S2.M_PI * rand.nextDouble
    val r = Math.sqrt(h * (2 - h))
    S2Point.normalize(S2Point.add(S2Point.add(S2Point.mul(x, Math.cos(theta) * r), S2Point.mul(y, Math.sin(theta) * r)), S2Point.mul(z, 1 - h)))
  }

  protected def randomFrame: Seq[S2Point] = {
    val p0 = randomPoint
    val p1 = S2Point.normalize(S2Point.crossProd(p0, randomPoint))
    val p2 = S2Point.normalize(S2Point.crossProd(p0, p1))
    Seq(p0, p1, p2)
  }

  protected def randomCellId(level: Int): S2CellId = {
    val face = random(S2CellId.NUM_FACES)
    val pos = rand.nextLong & ((1L << (2 * S2CellId.MAX_LEVEL)) - 1)
    S2CellId.fromFacePosLevel(face, pos, level)
  }

  protected def randomCellId: S2CellId = randomCellId(random(S2CellId.MAX_LEVEL + 1))

  protected def randomPoint: S2Point =
    S2Point.normalize(new S2Point(2 * rand.nextDouble - 1, 2 * rand.nextDouble - 1, 2 * rand.nextDouble - 1))

  protected def random(n: Int): Int = if (n == 0) 0 else rand.nextInt(n)

  protected def skewed(maxLog: Int): Int = {
    val base = math.abs(rand.nextInt) % (maxLog + 1)
    rand.nextInt & ((1 << base) - 1)
  }

  protected def parseVertices(str: String): Seq[S2Point] = {
    Splitter.on(',').split(str).asScala.map { token =>
      val colon = token.indexOf(':')
      require(colon > -1, s"Illegal string: $token. Should look like '35:20'")
      val lat = token.substring(0, colon).toDouble
      val lng = token.substring(colon + 1).toDouble
      S2LatLng.fromDegrees(lat, lng).toPoint
    }.toSeq
  }

  protected def makePoint(str: String): S2Point = parseVertices(str).head

  protected def makeLoop(str: String): S2Loop = new S2Loop(parseVertices(str).asJava)

  protected def makePolygon(str: String): S2Polygon = {
    val loops = Splitter.on(';').omitEmptyStrings.split(str).asScala.map { token =>
      val loop = makeLoop(token)
      loop.normalize()
      loop
    }.toSeq
    new S2Polygon(loops.asJava)
  }

  protected def makePolyline(str: String): S2Polyline = new S2Polyline(parseVertices(str).asJava)
}
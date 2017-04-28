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

package fr.cnrs.liris.privamov.ops

import fr.cnrs.liris.accio.framework.sdk._
import fr.cnrs.liris.common.geo._
import fr.cnrs.liris.dal.core.api.Dataset
import fr.cnrs.liris.dal.core.sparkle.DataFrame
import fr.cnrs.liris.privamov.core.model.Trace

@Op(
  category = "metric",
  help = "Computes the HeatMaps' distortions between two datasets")
class HeatMapDistortionOp extends Operator[HeatMapDistortionIn, HeatMapDistortionOut] with SparkleOperator {
  override def execute(in: HeatMapDistortionIn, ctx: OpContext): HeatMapDistortionOut = {
    // read the data
    val dstrain = read[Trace](in.train)
    val dstest = read[Trace](in.test)
    // rectangular point
    val (p1, p2) = initializePoint(in)
    val (rdstrain, _) = restrictArea(dstrain, p1, p2)
    val (rdstest, _) = restrictArea(dstest, p1, p2)
    // Dimension of the matrix
    val dimensions = computeMatricesSize(p1, p2, in.cellSize)
    // compute HeatMaps
    val train = formSingleMatrices(rdstrain, dimensions, in.cellSize)
    val test = formSingleMatrices(rdstest, dimensions, in.cellSize)
    // Compute distortions
    var dist = getDistortions(train, test, in.distanceType)
    // Add none found user in Train
    dstrain.keys.union(dstest.keys).toSet.foreach { u: String =>
      if (!dist.contains(u)) dist += u -> Double.NaN
    }

    val distWithoutNaN = dist.filter { case (k, p) => p != Double.NaN }
    val avgDist = distWithoutNaN.values.sum / dist.size.toDouble

    HeatMapDistortionOut(dist, avgDist)
  }

  private def formSingleMatrices(ds: DataFrame[Trace], dimensions: (Int, Int, Point), cellSize: Distance): Map[String, SparseMatrix[Int]] = {
    var outputMap = scala.collection.immutable.Map.empty[String, SparseMatrix[Int]]
    ds.foreach { t =>
      synchronized(outputMap += (t.user -> new SparseMatrix[Int](dimensions._1, dimensions._2)))
    }
    ds.foreach { t =>
      val l = t.events.length
      if (l != 0) {
        t.events.last.time
        val user = t.user
        val events = t.events
        events.foreach { e =>
          val p = e.point
          val j = math.floor((p.x - dimensions._3.x) / cellSize.meters).toInt
          val i = math.floor((p.y - dimensions._3.y) / cellSize.meters).toInt
          val mat = outputMap(user)
          mat.inc(i, j)
          synchronized(outputMap += (user -> mat))
        }
      } else {
        synchronized(outputMap -= t.user)
      }
    }
    outputMap
  }

  private def getDistortions(trainMats: Map[String, SparseMatrix[Int]], testMats: Map[String, SparseMatrix[Int]], n: String): Map[String, Double] = {
    testMats.par.map {
      case (k, mat_k) =>
        if (!trainMats.contains(k)) k -> Double.NaN
        else {
          val mat_u = trainMats(k)
          k -> DistanceUtils.d(mat_k.proportional, mat_u.proportional, n)
        }
    }.seq
  }

  private def computeMatricesSize(p1: Point, p2: Point, cellSize: Distance): (Int, Int, Point) = {
    val topCornerleft = Point(math.min(p1.x, p2.x), math.max(p1.y, p2.y))
    val bottomCornerleft = Point(math.min(p1.x, p2.x), math.min(p1.y, p2.y))
    val bottomCornerRight = Point(math.max(p1.x, p2.x), math.min(p1.y, p2.y))

    val width = bottomCornerleft.distance(bottomCornerRight)
    val height = topCornerleft.distance(bottomCornerleft)
    val nbRows = math.ceil(height / cellSize).toInt
    val nbColumn = math.ceil(width / cellSize).toInt

    (nbRows, nbColumn, bottomCornerleft)
  }


  private def initializePoint(in: HeatMapDistortionIn): (Point, Point) = in.lower.toPoint -> in.upper.toPoint

  private def restrictArea(ds: DataFrame[Trace], p1: Point, p2: Point): (DataFrame[Trace], Double) = {
    // Prepare the restrictive box
    val bounder = BoundingBox(p1, p2)
    // Restrict the tracers to the region.
    val output: DataFrame[Trace] = ds.map { t =>
      val newt = t.filter { e => bounder.contains(e.point) }
      newt
    }
    val nbTot = ds.map(_.events.size).toArray.sum
    val nbTaken = output.map(_.events.size).toArray.sum
    val ratio = nbTaken.toDouble / nbTot.toDouble
    (output, ratio)
  }
}

case class HeatMapDistortionIn(
  @Arg(help = "Input train dataset")
  train: Dataset,
  @Arg(help = "Input test dataset")
  test: Dataset,
  @Arg(help = "Type of distance metrics between matrices")
  @OneOf(Array("kl", "det", "nonzerocellsratio", "loren", "innerp", "cosine", "dice", "ndiv", "clark", "topsoe", "jensendiv"))
  distanceType: String = "topsoe",
  @Arg(help = "Cell Size in meters")
  cellSize: Distance,
  @Arg(help = "Lower point")
  lower: LatLng = LatLng.degrees(-61.0, -131.0),
  @Arg(help = "Upper point")
  upper: LatLng = LatLng.degrees(80, 171))

case class HeatMapDistortionOut(
  @Arg(help = "Distortions (\"-\" = missing user in train or test) ") distortions: Map[String, Double],
  @Arg(help = "Average distortion") avgDist: Double)
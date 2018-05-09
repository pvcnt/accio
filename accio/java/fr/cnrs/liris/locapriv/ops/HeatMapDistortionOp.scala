/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016 Vincent Primault <v.primault@ucl.ac.uk>
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

package fr.cnrs.liris.locapriv.ops

import fr.cnrs.liris.accio.sdk._
import fr.cnrs.liris.locapriv.domain.Event
import fr.cnrs.liris.sparkle.DataFrame
import fr.cnrs.liris.util.geo._

@Op(
  category = "metric",
  help = "Computes the HeatMaps' distortions between two datasets")
case class HeatMapDistortionOp(
  @Arg(help = "Input train dataset")
  train: RemoteFile,
  @Arg(help = "Input test dataset")
  test: RemoteFile,
  @Arg(help = "Type of distance metrics between matrices")
  distanceType: String = "topsoe",
  @Arg(help = "Cell Size in meters")
  cellSize: Distance,
  @Arg(help = "Lower point")
  lower: LatLng = LatLng.degrees(-61.0, -131.0),
  @Arg(help = "Upper point")
  upper: LatLng = LatLng.degrees(80, 171))
  extends ScalaOperator[HeatMapDistortionOp.Out] with SparkleOperator {

  private[this] val (nbRows, nbColumns, bottomCornerLeft) = {
    val p1 = lower.toPoint
    val p2 = upper.toPoint
    val topCornerleft = Point(math.min(p1.x, p2.x), math.max(p1.y, p2.y))
    val bottomCornerleft = Point(math.min(p1.x, p2.x), math.min(p1.y, p2.y))
    val bottomCornerRight = Point(math.max(p1.x, p2.x), math.min(p1.y, p2.y))

    val width = bottomCornerleft.distance(bottomCornerRight)
    val height = topCornerleft.distance(bottomCornerleft)
    val nbRows = math.ceil(height / cellSize).toInt
    val nbColumn = math.ceil(width / cellSize).toInt

    (nbRows, nbColumn, bottomCornerleft)
  }

  override def execute(ctx: OpContext): HeatMapDistortionOp.Out = {
    val dstrain = restrictArea(read[Event](train))
    val dstest = restrictArea(read[Event](test))

    val metrics = dstrain.zipPartitions(dstest)(compute)
    // Add none found user in Train
    /*dstrain.keys.union(dstest.keys).toSet.foreach { u: String =>
      if (!metrics.contains(u)) metrics += u -> Double.NaN
    }*/

    val distWithoutNaN = metrics.map(_.distortion).filter(n => !n.isNaN)
    val avgDist = distWithoutNaN.sum / metrics.count()

    HeatMapDistortionOp.Out(write(metrics, 0, ctx), avgDist)
  }

  private def computeMatrix(trace: Seq[Event]): SparseMatrix[Int] = {
    val matrix = new SparseMatrix[Int](nbRows, nbColumns)
    trace.foreach { e =>
      val p = e.point
      val j = math.floor((p.x - bottomCornerLeft.x) / cellSize.meters).toInt
      val i = math.floor((p.y - bottomCornerLeft.y) / cellSize.meters).toInt
      matrix.inc(i, j)
    }
    matrix
  }

  private def compute(train: Seq[Event], test: Seq[Event]): Seq[HeatMapDistortionOp.Value] = {
    val matTrain = computeMatrix(train)
    val matTest = computeMatrix(test)
    val d = DistanceUtils.d(matTest.proportional, matTrain.proportional, distanceType)
    Seq(HeatMapDistortionOp.Value(train.head.id, d))
  }

  private def restrictArea(ds: DataFrame[Event]): DataFrame[Event] = {
    val bounder = BoundingBox(lower.toPoint, upper.toPoint)
    ds.filter(e => bounder.contains(e.point))
  }
}

object HeatMapDistortionOp {

  case class Value(id: String, distortion: Double)

  case class Out(
    @Arg(help = "Metrics dataset")
    distortions: RemoteFile,
    @Arg(help = "Average distortion")
    avgDist: Double)

}
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

import fr.cnrs.liris.lumos.domain.RemoteFile
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
  @Arg(help = "Lower point latitude")
  lowerLat: Double = -61,
  @Arg(help = "Lower point longitude")
  lowerLng: Double = -131,
  @Arg(help = "Upper point latitude")
  upperLat: Double = 80,
  @Arg(help = "Upper point longitude")
  upperLng: Double = 171)
  extends ScalaOperator[HeatMapDistortionOp.Out] with SparkleOperator {

  private[this] val lower = LatLng.degrees(lowerLat, lowerLng).toPoint
  private[this] val upper = LatLng.degrees(upperLat, upperLng).toPoint

  private[this] val (nbRows, nbColumns, bottomCornerLeft) = {
    val topCornerleft = Point(math.min(lower.x, upper.x), math.max(lower.y, upper.y))
    val bottomCornerleft = Point(math.min(lower.x, upper.x), math.min(lower.y, upper.y))
    val bottomCornerRight = Point(math.max(lower.x, upper.x), math.min(lower.y, upper.y))

    val width = bottomCornerleft.distance(bottomCornerRight)
    val height = topCornerleft.distance(bottomCornerleft)
    val nbRows = math.ceil(height / cellSize).toInt
    val nbColumn = math.ceil(width / cellSize).toInt

    (nbRows, nbColumn, bottomCornerleft)
  }

  override def execute(ctx: OpContext): HeatMapDistortionOp.Out = {
    val dstrain = restrictArea(read[Event](train)).groupBy(_.id)
    val dstest = restrictArea(read[Event](test)).groupBy(_.id)

    val metrics = dstrain.join(dstest)(compute)
    // Add none found user in Train
    /*dstrain.keys.union(dstest.keys).toSet.foreach { u: String =>
      if (!metrics.contains(u)) metrics += u -> Double.NaN
    }*/

    val distWithoutNaN = metrics.map(_.distortion).filter(n => !n.isNaN)
    val avgDist = distWithoutNaN.sum / metrics.count()

    HeatMapDistortionOp.Out(write(metrics, 0, ctx), avgDist)
  }

  private def computeMatrix(trace: Iterable[Event]): SparseMatrix[Int] = {
    val matrix = new SparseMatrix[Int](nbRows, nbColumns)
    trace.foreach { e =>
      val p = e.point
      val j = math.floor((p.x - bottomCornerLeft.x) / cellSize.meters).toInt
      val i = math.floor((p.y - bottomCornerLeft.y) / cellSize.meters).toInt
      matrix.inc(i, j)
    }
    matrix
  }

  private def compute(id: String, train: Iterable[Event], test: Iterable[Event]): Seq[HeatMapDistortionOp.Value] = {
    val matTrain = computeMatrix(train)
    val matTest = computeMatrix(test)
    val d = DistanceUtils.d(matTest.proportional, matTrain.proportional, distanceType)
    Seq(HeatMapDistortionOp.Value(id, d))
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
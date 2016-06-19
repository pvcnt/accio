package fr.cnrs.liris.common.geo

import com.google.common.base.MoreObjects
import com.google.common.geometry.{S1Angle, S2LatLng, S2Point}
import fr.cnrs.liris.common.util.Distance

sealed trait Location {
  def distance(other: Location): Distance

  def translate(angle: S1Angle, distance: Distance): Location

  def toPoint: Point

  def toLatLng: LatLng

  def toSeq: Seq[Double]
}

/**
 * A geographical location.
 */
class LatLng(private val inner: S2LatLng) extends Location {
  def lat: S1Angle = inner.lat

  def lng: S1Angle = inner.lng

  /**
   * Compute the distance with another location on the surface of the Earth..
   *
   * @param other Another location
   * @return The distance in meters
   */
  override def distance(other: Location): Distance = other match {
    case latLng: LatLng => Distance.meters(inner.getEarthDistance(latLng.inner))
    case point: Point => toPoint.distance(point)
  }

  override def translate(angle: S1Angle, distance: Distance): LatLng = {
    // http://www.movable-type.co.uk/scripts/latlong.html
    val angularDistance = distance / LatLng.EarthRadius
    val lat1 = inner.latRadians
    val lon1 = inner.lngRadians
    val lat2 = math.asin(
      math.sin(lat1) * math.cos(angularDistance) +
          math.cos(lat1) * math.sin(angularDistance) * math.cos(angle.radians))
    val lon2 = lon1 + math.atan2(
      math.sin(angle.radians) * math.sin(angularDistance) * math.cos(lat1),
      math.cos(angularDistance) - math.sin(lat1) * math.sin(lat2))
    LatLng.radians(lat2, lon2)
  }

  override def toSeq: Seq[Double] = Seq(lng.degrees, lat.degrees)

  override def toPoint: Point = Point(Mercator.lng2x(lng), Mercator.lat2y(lat))

  override def toLatLng: LatLng = this

  def toS2: S2LatLng = inner

  override def toString: String =
    MoreObjects.toStringHelper(this)
        .add("lat", lat)
        .add("lng", lng)
        .toString
}

object LatLng {
  val EarthRadius = Distance.meters(S2LatLng.EARTH_RADIUS_METERS)

  def parse(str: String): LatLng = str.split(",") match {
    case Array(lat, lng) => LatLng.degrees(lat.trim.toDouble, lng.trim.toDouble)
    case _ => throw new IllegalArgumentException(s"Invalid lat/lng string: $str")
  }

  def apply(lat: S1Angle, lng: S1Angle): LatLng = new LatLng(new S2LatLng(lat, lng))

  def apply(latLng: S2LatLng): LatLng = new LatLng(latLng)

  def apply(point: S2Point): LatLng = new LatLng(new S2LatLng(point))

  /**
   * Create a location from a latitude and a longitude.
   *
   * @param lat
   * @param lng
   * @return
   */
  def degrees(lat: Double, lng: Double): LatLng = new LatLng(S2LatLng.fromDegrees(lat, lng))

  def radians(lat: Double, lng: Double): LatLng = {
    val normLat = math.atan(math.sin(lat) / math.abs(math.cos(lat)))
    val normLng = math.atan2(math.sin(lng), math.cos(lng))
    new LatLng(S2LatLng.fromRadians(normLat, normLng))
  }
}

/**
 * A planar 2D point.
 *
 * @param x X coordinate
 * @param y Y coordinate
 */
case class Point(x: Double, y: Double) extends Location {
  /**
   * Compute the euclidian distance with another point.
   *
   * @param other Another point
   * @return A distance
   */
  override def distance(other: Location): Distance = other match {
    case point: Point => Distance.meters(math.sqrt(math.pow(point.x - x, 2) + Math.pow(point.y - y, 2)))
    case latLng: LatLng => distance(latLng.toPoint)
  }

  /**
   * Move a point in a given direction from a given distance.
   *
   * @param direction A direction as an angle (e.g. 0° east, 90° north, 180° west, 270° south)
   * @param distance  A distance to move the point by
   * @return A new translated point
   */
  override def translate(direction: S1Angle, distance: Distance): Point = {
    val dx = distance.meters * math.cos(direction.radians)
    val dy = distance.meters * math.sin(direction.radians)
    new Point(x + dx, y + dy)
  }

  override def toLatLng: LatLng = LatLng(Mercator.y2lat(y), Mercator.x2lng(x))

  override def toPoint: Point = this

  override def toSeq: Seq[Double] = Seq(x, y)

  def interpolate(dest: Point, ratio: Double): Point = {
    val dx = dest.x - x
    val dy = dest.y - y
    new Point(x + ratio * dx, y + ratio * dy)
  }

  def +(other: Point): Point = Point(x + other.x, y + other.y)

  def -(other: Point): Point = Point(x - other.x, y - other.y)

  def /(factor: Double): Point = Point(x / factor, y / factor)

  def *(factor: Double): Point = Point(x * factor, y * factor)
}

object Point {
  def tuple(tuple: (Double, Double)): Point = new Point(tuple._1, tuple._2)

  def nearest(ref: Point, points: Iterable[Point]): PointWithDistance =
    points.zipWithIndex.map { case (pt, idx) =>
      PointWithDistance(pt, idx, pt.distance(ref))
    }.minBy(_.distance)

  def centroid(points: Iterable[Point]): Point = points.reduce(_ + _) / points.size

  /**
   * Heuristic to fast compute the diameter of a set of points. The result is
   * still approximate and not guaranteed to be exact.
   *
   * @param locs A collection of points
   * @return The diameter
   * @see http://stackoverflow.com/questions/16865291/greatest-distance-between-set-of-longitude-latitude-points
   */
  def fastDiameter(locs: Iterable[Point]): Distance = {
    val c = Point.centroid(locs)
    val p0 = farthest(c, locs).point
    val p1 = farthest(p0, locs).point
    p0.distance(p1)
  }

  /**
   * Compute the farthest point in a collection from a reference point. If two
   * points are at the same distance, the first encountered point is kept.
   *
   * @param ref  Reference point
   * @param locs A collection of points
   * @return Farthest point with distance to reference point
   */
  def farthest(ref: Point, locs: Iterable[Point]): PointWithDistance =
    locs.zipWithIndex.map { case (pt, idx) =>
      new PointWithDistance(pt, idx, pt.distance(ref))
    }.maxBy(_.distance)

  /**
   * Exact computation of the diameter of a set of points. It computes
   * distances between each pair of points and takes the maximum.
   * (i.e., computing distance between each pair of points) is not suitable when
   * the number of points grows.
   *
   * @param locs A collection of points
   * @return The diameter
   */
  def exactDiameter(locs: Iterable[Point]): Distance = {
    var diameter = Distance.Zero
    var i = 0
    val size = locs.size
    for (l1 <- locs) {
      for (l2 <- locs.takeRight(size - i)) {
        if (l1 != l2) {
          val d = l1.distance(l2)
          if (d > diameter) {
            diameter = d
          }
        }
      }
      i += 1
    }
    diameter
  }
}

case class PointWithDistance(point: Point, idx: Int, distance: Distance)
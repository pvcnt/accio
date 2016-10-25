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

package fr.cnrs.liris.common.geo

import com.google.common.geometry.{S1Angle, S2, S2LatLng, S2Point}

/**
 * Trait for something that represents a 2D location.
 */
trait Location extends Serializable {
  /**
   * Compute the distance with another location.
   *
   * @param other Another location.
   */
  def distance(other: Location): Distance

  /**
   * Return a new location being the translation of this one from a given distance following a given direction.
   *
   * @param angle    Direction to follow when translating, expressed as an angle.
   * @param distance Distance to translate by.
   */
  def translate(angle: S1Angle, distance: Distance): Location

  /**
   * Return this location as a point in a cartesian space.
   */
  def toPoint: Point

  /**
   * Return this location as a latitude/longitude pair.
   */
  def toLatLng: LatLng

  /**
   * Return this location as a list of coordinates.
   */
  def toSeq: Seq[Double]
}

/**
 * A geographical location represented as a pair of latitude and longitude. It uses the S2 library for the internal
 * representation.
 *
 * @param inner Corresponding location with S2.
 */
class LatLng private(private val inner: S2LatLng) extends Location {
  /**
   * Return the latitude of this location as an angle.
   */
  def lat: S1Angle = inner.lat

  /**
   * Return the longitude of this location as an angle.
   */
  def lng: S1Angle = inner.lng

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

  /**
   * Return this location as an S2 latitude/longitude pair.
   */
  def toS2: S2LatLng = inner

  override def toPoint: Point = Point(Mercator.lng2x(lng), Mercator.lat2y(lat))

  override def toLatLng: LatLng = this

  override def toSeq: Seq[Double] = Seq(lng.degrees, lat.degrees)

  override def equals(other: Any): Boolean = other match {
    case ll: LatLng => ll.inner == inner
    case _ => false
  }

  override def hashCode: Int = inner.hashCode

  override def toString: String = s"${lat.degrees},${lng.degrees}"
}

/**
 * Factory for [[LatLng]].
 */
object LatLng {
  /**
   * Radius of the Earth.
   */
  val EarthRadius = Distance.meters(S2LatLng.EARTH_RADIUS_METERS)

  /**
   * Parse a string into a latitude/longitude pair.
   *
   * @param str String to parse.
   * @throws IllegalArgumentException If the string is not formatted as a valid lat/lng.
   */
  @throws[IllegalArgumentException]
  def parse(str: String): LatLng = str.split(",") match {
    case Array(lat, lng) => LatLng.degrees(lat.trim.toDouble, lng.trim.toDouble)
    case _ => throw new IllegalArgumentException(s"Invalid lat/lng string: $str")
  }

  /**
   * Create a location from an S2 latitude/longitude pair.
   *
   * @param latLng S2 latitude/longitude.
   */
  def apply(latLng: S2LatLng): LatLng = new LatLng(latLng)

  /**
   * Create a location from an S2 point.
   *
   * @param point S2 point.
   */
  def apply(point: S2Point): LatLng = new LatLng(new S2LatLng(point))

  /**
   * Create a location from a latitude and longitude.
   *
   * @param lat Latitude.
   * @param lng Longitude.
   */
  def apply(lat: S1Angle, lng: S1Angle): LatLng = new LatLng(new S2LatLng(lat, lng))

  /**
   * Create a location from a latitude and a longitude expressed in degrees.
   *
   * @param lat Latitude (in degrees).
   * @param lng Longitude (in degrees).
   */
  def degrees(lat: Double, lng: Double): LatLng = new LatLng(S2LatLng.fromDegrees(lat, lng))

  /**
   * Create a location from a latitude and a longitude expressed in radians.
   *
   * @param lat Latitude (in radians).
   * @param lng Longitude (in radians).
   */
  def radians(lat: Double, lng: Double): LatLng = {
    val normLat = math.atan(math.sin(lat) / math.abs(math.cos(lat)))
    val normLng = math.atan2(math.sin(lng), math.cos(lng))
    new LatLng(S2LatLng.fromRadians(normLat, normLng))
  }
}

/**
 * A 2D location represented with cartesian coordinates. It uses meters as measurement unit.
 *
 * @param x X-axis coordinate (in meters).
 * @param y Y-axis coordinate (in meters).
 */
case class Point(x: Double, y: Double) extends Location {
  /**
   * Interpolate along a line formed by this point and another one by a given ratio.
   *
   * @param b     Destination point to form a line.
   * @param ratio Ratio to interpolate by (if <= 1, resulting point will be on the segment before `b`, if > 1 it will be after `b`).
   */
  def interpolate(b: Point, ratio: Double): Point = {
    val dx = b.x - x
    val dy = b.y - y
    new Point(x + ratio * dx, y + ratio * dy)
  }

  /**
   * Return a new point being the sum of this point and another point.
   *
   * @param other Another point.
   */
  def +(other: Point): Point = Point(x + other.x, y + other.y)

  /**
   * Return a new point being the difference between this point and another point.
   *
   * @param other Another point.
   */
  def -(other: Point): Point = Point(x - other.x, y - other.y)

  /**
   * Return a new point being this point multiplied by some constant.
   *
   * @param factor Factor to multiply this point by.
   */
  def *(factor: Double): Point = Point(x * factor, y * factor)

  /**
   * Return a new point being this point divided by some constant.
   *
   * @param factor Factor to divide this point by.
   */
  def /(factor: Double): Point = Point(x / factor, y / factor)

  override def distance(other: Location): Distance = other match {
    case point: Point => Distance.meters(math.sqrt(math.pow(point.x - x, 2) + Math.pow(point.y - y, 2)))
    case latLng: LatLng => distance(latLng.toPoint)
  }

  override def translate(direction: S1Angle, distance: Distance): Point = {
    val dx = distance.meters * math.cos(direction.radians)
    val dy = distance.meters * math.sin(direction.radians)
    new Point(x + dx, y + dy)
  }

  override def toLatLng: LatLng = LatLng(Mercator.y2lat(y), Mercator.x2lng(x))

  override def toPoint: Point = this

  override def toSeq: Seq[Double] = Seq(x, y)
}

/**
 * Factory and helper methods for [[Point]].
 */
object Point {
  /**
   * Compute the nearest point in a collection from a reference point. If two points are at the same distance, the
   * first encountered point is kept.
   *
   * @param ref    Reference point.
   * @param points List of points.
   * @return Nearest point from the reference point.
   */
  def nearest(ref: Point, points: Iterable[Point]): PointWithDistance = {
    points.zipWithIndex.map { case (pt, idx) =>
      PointWithDistance(pt, idx, pt.distance(ref))
    }.minBy(_.distance)
  }

  /**
   * Compute the centroid of a list of points.
   *
   * @param points List of points.
   */
  def centroid(points: Iterable[Point]): Point = points.reduce(_ + _) / points.size

  /**
   * Compute the farthest point in a collection from a reference point. If two points are at the same distance, the
   * first encountered point is kept.
   *
   * @param ref    Reference point.
   * @param points List of points.
   * @return Farthest point from the reference point.
   */
  def farthest(ref: Point, points: Iterable[Point]): PointWithDistance = {
    points.zipWithIndex.map { case (pt, idx) =>
      PointWithDistance(pt, idx, pt.distance(ref))
    }.maxBy(_.distance)
  }

  /**
   * Compute the approximate diameter of a set of points using a heuristic.
   *
   * @param points List of points.
   * @return Distance between the two farthest points.
   * @see http://stackoverflow.com/questions/16865291/greatest-distance-between-set-of-longitude-latitude-points
   */
  def fastDiameter(points: Iterable[Point]): Distance = {
    val c = Point.centroid(points)
    val p0 = farthest(c, points).point
    val p1 = farthest(p0, points).point
    p0.distance(p1)
  }

  /**
   * Compute the exact diameter of a set of points. It computes distances between each pair of points and takes
   * the maximum. It is an O(n2) method that may not scale with the number of points.
   *
   * @param points List of points.
   * @return Distance between the two farthest points.
   */
  def exactDiameter(points: Iterable[Point]): Distance = {
    var diameter = Distance.Zero
    var i = 0
    val size = points.size
    for (l1 <- points) {
      for (l2 <- points.takeRight(size - i)) {
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

/**
 * A point associated with a distance. It is used as a result for some methods.
 *
 * @param point    Point.
 * @param idx      Position of this point within the original list.
 * @param distance Distance (depends on the context).
 */
case class PointWithDistance(point: Point, idx: Int, distance: Distance)

/**
 * Helper methods handling conversions back and forth between [[LatLng]]s and [[Point]]s.
 */
private object Mercator {
  /**
   * Convert a cartesian X coordinate into a longitude.
   *
   * @param x X-axis coordinate (in meters).
   * @return Corresponding longitude angle.
   */
  def x2lng(x: Double): S1Angle = S1Angle.radians(x / LatLng.EarthRadius.meters)

  /**
   * Convert a longitude into a cartesian X coordinate.
   *
   * @param lng Longitude angle.
   * @return Corresponding X-axis coordinate (in meters).
   */
  def lng2x(lng: S1Angle): Double = lng.radians * LatLng.EarthRadius.meters

  /**
   * Convert a cartesian Y coordinate into a latitude.
   *
   * @param y Y-axis coordinate (in meters).
   * @return Corresponding latitude angle.
   */
  def y2lat(y: Double): S1Angle = S1Angle.radians(2 * math.atan(math.exp(y / LatLng.EarthRadius.meters)) - S2.M_PI_2)

  /**
   * Convert a latitude into a cartesian Y coordinate.
   *
   * @param lat Latitude angle.
   * @return Corresponding Y-axis coordinate (in meters).
   */
  def lat2y(lat: S1Angle): Double = Math.log(Math.tan(S2.M_PI_4 + lat.radians / 2)) * LatLng.EarthRadius.meters
}
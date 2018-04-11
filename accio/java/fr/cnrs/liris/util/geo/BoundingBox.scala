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

package fr.cnrs.liris.util.geo

/**
 * A bounding box is a spatial area. It is delimited by two corners, thus forming a square.
 *
 * @param lower Lower corner.
 * @param upper Upper corner.
 */
case class BoundingBox(lower: Point, upper: Point) {
  require(lower.x <= upper.x && lower.y <= upper.y, "Lower point must be lower than upper point")

  def contains(point: Point): Boolean = {
    point.x >= lower.x && point.y >= lower.y && point.x <= upper.x && point.y <= upper.y
  }

  def contains(other: BoundingBox): Boolean = contains(other.lower) && contains(other.upper)

  def contained(other: BoundingBox): Boolean = other.contains(this)

  def intersection(other: BoundingBox): BoundingBox = {
    val newLower = Point(math.min(lower.x, other.lower.x), math.min(lower.y, other.lower.y))
    val newUpper = Point(math.max(upper.x, other.upper.x), math.max(upper.y, other.upper.y))
    BoundingBox(newLower, newUpper)
  }

  def intersects(other: BoundingBox): Boolean = contains(other.lower) || contains(other.upper)

  def union(other: BoundingBox): BoundingBox = {
    val minX = math.min(lower.x, other.lower.x)
    val minY = math.min(lower.y, other.lower.y)
    val maxX = math.min(upper.x, other.upper.x)
    val maxY = math.min(upper.y, other.upper.y)
    BoundingBox(Point(minX, minY), Point(maxX, maxY))
  }

  def diagonal: Distance = lower.distance(upper)
}
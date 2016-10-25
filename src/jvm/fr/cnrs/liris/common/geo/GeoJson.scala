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

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonProperty, JsonSubTypes, JsonTypeInfo}

/**
 * Base trait for GeoJSON objects, ready for Jackson (de)serialization.
 */
sealed trait GeoJson {
  @JsonProperty("type")
  def kind: String
}

/**
 * Base trait for GeoJSON Geometry objects, ready for Jackson (de)serialization.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(Array(
  new Type(value = classOf[GeoPoint], name = "Point"),
  new Type(value = classOf[MultiPoint], name = "MultiPoint"),
  new Type(value = classOf[LineString], name = "LineString"),
  new Type(value = classOf[MultiLineString], name = "MultiLineString"),
  new Type(value = classOf[Polygon], name = "Polygon"),
  new Type(value = classOf[MultiPolygon], name = "MultiPolygon")
))
sealed trait Geometry extends GeoJson

case class GeoPoint(coordinates: Seq[Double]) extends Geometry {
  def kind: String = "Point"
}

object GeoPoint {
  def apply(point: Point): GeoPoint = apply(point.toLatLng)

  def apply(latLng: LatLng): GeoPoint = new GeoPoint(Seq(latLng.lng.degrees, latLng.lat.degrees))

  def coords(x: Double, y: Double): GeoPoint = new GeoPoint(Seq(x, y))

  def coords(x: Double, y: Double, z: Double): GeoPoint = new GeoPoint(Seq(x, y, z))
}

case class MultiPoint(coordinates: Seq[Seq[Double]]) extends Geometry {
  def kind: String = "MultiPoint"
}

object MultiPoint {
  def points(points: Seq[GeoPoint]): MultiPoint = new MultiPoint(points.map(_.coordinates))
}

case class LineString(coordinates: Seq[Seq[Double]]) extends Geometry {
  def kind: String = "LineString"

  def isRing: Boolean = coordinates.length >= 4 && coordinates.head.equals(coordinates.last)
}

object LineString {
  def points(points: Seq[GeoPoint]): LineString = {
    require(points.length >= 2, "A LineString must be formed of at least two points.")
    new LineString(points.map(_.coordinates))
  }
}

case class MultiLineString(coordinates: Seq[Seq[Seq[Double]]]) extends Geometry {
  def kind: String = "MultiLineString"
}

object MultiLineString {
  def lines(lines: Seq[LineString]): MultiLineString = new MultiLineString(lines.map(_.coordinates))
}

case class Polygon(coordinates: Seq[Seq[Seq[Double]]]) extends Geometry {
  def kind: String = "Polygon"
}

object Polygon {
  def lines(rings: Seq[LineString]): Polygon = {
    require(rings.forall(_.isRing), "A Polygon must be formed of rings.")
    new Polygon(rings.map(_.coordinates))
  }
}

case class MultiPolygon(coordinates: Seq[Seq[Seq[Seq[Double]]]]) extends Geometry {
  def kind: String = "MultiPolygon"
}

object MultiPolygon {
  def ofPolygons(polygons: Seq[Polygon]): MultiPolygon = new MultiPolygon(polygons.map(_.coordinates))
}

case class GeometryCollection(geometries: Seq[Geometry]) extends Geometry {
  def kind: String = "GeometryCollection"
}

case class Feature(geometry: Geometry, properties: Map[String, Any] = Map.empty[String, Any]) extends GeoJson {
  def kind: String = "Feature"

  def withProperty(key: String, value: Any): Feature = Feature(geometry, properties + (key -> value))
}

case class FeatureCollection(features: Seq[Feature], bbox: Option[Seq[Double]] = None) extends GeoJson {
  def kind: String = "FeatureCollection"
}
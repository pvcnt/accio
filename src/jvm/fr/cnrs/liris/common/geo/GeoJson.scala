/*
 * Copyright LIRIS-CNRS (2016)
 * Contributors: Vincent Primault <vincent.primault@liris.cnrs.fr>
 *
 * This software is a computer program whose purpose is to study location privacy.
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */

package fr.cnrs.liris.common.geo

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonProperty, JsonSubTypes, JsonTypeInfo}

/**
 * Base trait for GeoJSON objects, ready for Jackson (de)serialization.
 */
sealed trait GeoJson {
  @JsonProperty
  def `type`: String
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
  def `type`: String = "Point"
}

object GeoPoint {
  def apply(point: Point): GeoPoint = apply(point.toLatLng)

  def apply(latLng: LatLng): GeoPoint = new GeoPoint(Seq(latLng.lng.degrees, latLng.lat.degrees))

  def coords(x: Double, y: Double): GeoPoint = new GeoPoint(Seq(x, y))

  def coords(x: Double, y: Double, z: Double): GeoPoint = new GeoPoint(Seq(x, y, z))
}

case class MultiPoint(coordinates: Seq[Seq[Double]]) extends Geometry {
  def `type`: String = "MultiPoint"
}

object MultiPoint {
  def points(points: Seq[GeoPoint]): MultiPoint = new MultiPoint(points.map(_.coordinates))
}

case class LineString(coordinates: Seq[Seq[Double]]) extends Geometry {
  def `type`: String = "LineString"

  def isRing: Boolean = coordinates.length >= 4 && coordinates.head.equals(coordinates.last)
}

object LineString {
  def points(points: Seq[GeoPoint]): LineString = {
    require(points.length >= 2, "A LineString must be formed of at least two points.")
    new LineString(points.map(_.coordinates))
  }
}

case class MultiLineString(coordinates: Seq[Seq[Seq[Double]]]) extends Geometry {
  def `type`: String = "MultiLineString"
}

object MultiLineString {
  def lines(lines: Seq[LineString]): MultiLineString = new MultiLineString(lines.map(_.coordinates))
}

case class Polygon(coordinates: Seq[Seq[Seq[Double]]]) extends Geometry {
  def `type`: String = "Polygon"
}

object Polygon {
  def lines(rings: Seq[LineString]): Polygon = {
    require(rings.forall(_.isRing), "A Polygon must be formed of rings.")
    new Polygon(rings.map(_.coordinates))
  }
}

case class MultiPolygon(coordinates: Seq[Seq[Seq[Seq[Double]]]]) extends Geometry {
  def `type`: String = "MultiPolygon"
}

object MultiPolygon {
  def ofPolygons(polygons: Seq[Polygon]): MultiPolygon = new MultiPolygon(polygons.map(_.coordinates))
}

case class GeometryCollection(geometries: Seq[Geometry]) extends Geometry {
  def `type`: String = "GeometryCollection"
}

case class Feature(geometry: Geometry, properties: Map[String, Any] = Map.empty[String, Any]) extends GeoJson {
  def `type`: String = "Feature"

  def withProperty(key: String, value: Any): Feature =
    new Feature(geometry, properties + (key -> value))
}

case class FeatureCollection(features: Seq[Feature], bbox: Option[Seq[Double]] = None) extends GeoJson {
  def `type`: String = "FeatureCollection"
}
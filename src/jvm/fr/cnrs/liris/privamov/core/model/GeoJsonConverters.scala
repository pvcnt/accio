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

package fr.cnrs.liris.privamov.core.model

import fr.cnrs.liris.common.geo.{Feature, FeatureCollection, GeoPoint, Location}

object GeoJsonConverters {
  implicit def locationToGeoJson(location: Location): LocationToGeoJson =
    new LocationToGeoJson(location)

  implicit def eventToGeoJson(event: Event): EventToGeoJson = new EventToGeoJson(event)

  implicit def poiToGeoJson(poi: Poi): PoiToGeoJson = new PoiToGeoJson(poi)

  implicit def traceToGeoJson(trace: Trace): TraceToGeoJson = new TraceToGeoJson(trace)
}

class LocationToGeoJson(location: Location) {
  /**
   * Convert this location into GeoJSON.
   */
  def toGeoJson: GeoPoint = {
    val latLng = location.toLatLng
    GeoPoint.coords(latLng.lng.degrees, latLng.lat.degrees)
  }
}

class EventToGeoJson(event: Event) {
  /**
   * Convert this event into GeoJSON.
   */
  def toGeoJson: Feature = {
    val properties = Map(
      "time" -> event.time.toString,
      "user" -> event.user
    ) ++ event.props
    Feature(new LocationToGeoJson(event.point).toGeoJson, properties)
  }
}

class PoiToGeoJson(poi: Poi) {
  /**
   * Convert this POI into GeoJSON.
   */
  def toGeoJson: Feature = {
    val properties = Map(
      "user" -> poi.user,
      "size" -> poi.size,
      "start" -> poi.firstSeen.toString,
      "end" -> poi.lastSeen.toString,
      "duration" -> poi.duration.toString,
      "diameter" -> poi.diameter.meters.toInt)
    Feature(new LocationToGeoJson(poi.centroid).toGeoJson, properties)
  }
}

class TraceToGeoJson(trace: Trace) {
  /**
   * Convert this trace into GeoJSON.
   */
  def toGeoJson(trace: Trace): FeatureCollection =
    FeatureCollection(trace.events.map(new EventToGeoJson(_).toGeoJson))
}
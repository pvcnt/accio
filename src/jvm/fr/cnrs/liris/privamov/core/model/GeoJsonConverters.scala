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
      "start" -> poi.start.toString,
      "end" -> poi.end.toString,
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
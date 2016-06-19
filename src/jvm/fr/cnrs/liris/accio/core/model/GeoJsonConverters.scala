package fr.cnrs.liris.accio.core.model

import fr.cnrs.liris.common.geo.{Feature, FeatureCollection, GeoPoint, Location}

object GeoJsonConverters {
  implicit def locationToGeoJson(location: Location): LocationToGeoJson =
    new LocationToGeoJson(location)

  implicit def recordToGeoJson(record: Record): RecordToGeoJson = new RecordToGeoJson(record)

  implicit def recordToGeoJson(poi: Poi): PoiToGeoJson = new PoiToGeoJson(poi)

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

class RecordToGeoJson(record: Record) {
  /**
   * Convert this record into GeoJSON.
   */
  def toGeoJson: Feature = {
    val properties = Map(
      "time" -> record.time.toString,
      "user" -> record.user
    ) ++ record.props
    Feature(new LocationToGeoJson(record.point).toGeoJson, properties)
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
    FeatureCollection(trace.records.map(new RecordToGeoJson(_).toGeoJson))
}
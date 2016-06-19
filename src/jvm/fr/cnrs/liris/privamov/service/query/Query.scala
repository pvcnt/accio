package fr.cnrs.liris.privamov.service.query

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.twitter.finatra.validation.{Max, Min, PastTime}
import org.joda.time.DateTime

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(Array(
  new Type(value = classOf[QueryFilterTimeRange], name = "time_range"),
  new Type(value = classOf[QueryFilterTimeDay], name = "time_day"),
  new Type(value = classOf[QueryFilterTimeHour], name = "time_hour"),
  new Type(value = classOf[QueryFilterGeoDist], name = "geo_dist"),
  new Type(value = classOf[QueryFilterGeoBox], name = "geo_box"),
  new Type(value = classOf[QueryFilterGeoCell], name = "geo_cell"),
  new Type(value = classOf[QueryFilterGeoCountry], name = "geo_country"),
  new Type(value = classOf[QueryFilterGeoCity], name = "geo_city"),
  new Type(value = classOf[QueryFilterSource], name = "source"),
  new Type(value = classOf[QueryFilterSpeedRange], name = "speed_range"),
  new Type(value = classOf[QueryFilterSpeedStaying], name = "speed_staying"),
  new Type(value = classOf[QueryFilterSpeedMoving], name = "speed_moving")
))
sealed trait QueryFilter {
  def category: String
}

case class QueryFilterTimeRange(@PastTime from: Option[DateTime], @PastTime to: Option[DateTime]) extends QueryFilter {
  override def category: String = "time"
}

case class QueryFilterTimeDay(@Min(1) @Max(7) dayOfWeek: Int) extends QueryFilter {
  override def category: String = "time"
}

case class QueryFilterTimeHour(@Min(0) @Max(23) hourOfDay: Int) extends QueryFilter {
  override def category: String = "time"
}

case class QueryFilterGeoDist(@Min(0) distance: Double, location: (Double, Double)) extends QueryFilter {
  override def category: String = "geo"
}

case class QueryFilterGeoBox(box: ((Double, Double), (Double, Double))) extends QueryFilter {
  override def category: String = "geo"
}

case class QueryFilterGeoCell(cell: String) extends QueryFilter {
  override def category: String = "geo"
}

case class QueryFilterGeoCountry(countryCode: String) extends QueryFilter {
  override def category: String = "geo"
}

case class QueryFilterGeoCity(zipcode: String, countryCode: String) extends QueryFilter {
  override def category: String = "geo"
}

case class QueryFilterSource(source: String) extends QueryFilter {
  override def category: String = "source"
}

case class QueryFilterSpeedRange(@Min(0) min: Option[Double], @Min(0) max: Option[Double]) extends QueryFilter {
  override def category: String = "speed"
}

case class QueryFilterSpeedStaying() extends QueryFilter {
  override def category: String = "speed"
}

case class QueryFilterSpeedMoving() extends QueryFilter {
  override def category: String = "speed"
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(Array(
  new Type(value = classOf[QueryAggregateTimeseries], name = "timeseries"),
  new Type(value = classOf[QueryAggregateHeatmap], name = "heatmap")
))
sealed trait QueryAggregate

case class QueryAggregateTimeseries(period: String) extends QueryAggregate

case class QueryAggregateHeatmap(@Min(0) @Max(30) level: Int) extends QueryAggregate
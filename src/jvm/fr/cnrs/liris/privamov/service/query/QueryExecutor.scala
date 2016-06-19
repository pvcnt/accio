package fr.cnrs.liris.privamov.service.query

import com.google.common.geometry.S2CellId
import com.google.inject.Inject
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ElasticClient, QueryDefinition}
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.elasticsearch.search.aggregations.metrics.max.Max
import org.elasticsearch.search.aggregations.metrics.min.Min
import org.joda.time.DateTime

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class HeatmapBucket(cellId: S2CellId, count: Long)

class QueryExecutor @Inject()(elasticClient: ElasticClient) {
  def events(dataset: String, filters: Seq[QueryFilter], aggregate: QueryAggregateHeatmap): Future[Seq[HeatmapBucket]] = {
    elasticClient.execute {
      search in "event" -> dataset query {
        constantScoreQuery(getQueryDefinition(filters))
      } aggregations {
        aggregation terms "cells" field s"cell_${aggregate.level}" size 0
      } limit 0
    }.map { resp =>
      resp.aggregations.get[Terms]("cells").getBuckets.asScala.map { bucket =>
        val cellId = S2CellId.fromToken(bucket.getKeyAsString)
        HeatmapBucket(cellId, bucket.getDocCount)
      }
    }
  }

  def timeRange(dataset: String, filters: Seq[QueryFilter]): Future[(DateTime, DateTime)] = {
    elasticClient.execute {
      search in "event" -> dataset query {
        constantScoreQuery(getQueryDefinition(filters))
      } aggregations(
          aggregation min "min_time" field "time",
          aggregation max "max_time" field "time"
        )
    }.map { resp =>
      val minTime = new DateTime(resp.aggregations.get[Min]("min_time").getValue.toLong)
      val maxTime = new DateTime(resp.aggregations.get[Max]("max_time").getValue.toLong)
      (minTime, maxTime)
    }
  }

  private def getQueryDefinition(filters: Seq[QueryFilter]) = {
    val clauses: Seq[QueryDefinition] = filters.flatMap {
      case QueryFilterTimeRange(Some(startAfter), Some(endBefore)) =>
        Seq(rangeQuery("time") from startAfter to endBefore)
      case QueryFilterTimeRange(Some(startAfter), None) =>
        Seq(rangeQuery("time") gte startAfter.toString)
      case QueryFilterTimeRange(None, Some(endBefore)) =>
        Seq(rangeQuery("time") lte endBefore.toString)
      case QueryFilterTimeDay(dayOfWeek) => Seq(termsQuery("time_dayofweek", dayOfWeek.asInstanceOf[AnyRef]))
      //case QueryFilterTimeHour(hourOfDay)  =>
      //case QueryFilterGeoDist(distance, location) =>
      //case QueryFilterGeoBox(box) =>
      case QueryFilterGeoCell(token) =>
        val cellId = S2CellId.fromToken(token)
        Seq(termsQuery(s"cell_${cellId.level}", token))
      case QueryFilterGeoCountry(countryCode) => Seq(termsQuery("country", countryCode))
      case QueryFilterGeoCity(zipcode, countryCode) =>
        Seq(termsQuery("country", countryCode), termsQuery("zipcode", zipcode))
      case QueryFilterSource(source) =>
        Seq(termsQuery("source", source))
      case QueryFilterSpeedRange(Some(min), Some(max)) =>
        Seq(rangeQuery("speed") from min to max)
      case QueryFilterSpeedRange(Some(min), None) =>
        Seq(rangeQuery("speed") gte min)
      case QueryFilterSpeedRange(None, Some(max)) =>
        Seq(rangeQuery("speed") lte max)
      //case _: QueryFilterSpeedStaying =>
      //case _ :QueryFilterSpeedMoving =>
      case _ => Seq.empty
    }
    filter(clauses)
  }
}

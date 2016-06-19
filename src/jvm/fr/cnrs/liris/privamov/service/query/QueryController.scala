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

package fr.cnrs.liris.privamov.service.query

import com.google.common.geometry.{S2Cell, S2Point}
import com.google.inject.{Inject, Singleton}
import com.twitter.bijection.twitter_util.UtilBijections
import com.twitter.finatra.http.Controller
import com.twitter.finatra.request.{QueryParam, RouteParam}
import com.twitter.finatra.validation.OneOf
import com.twitter.util.{Future => TwitterFuture}
import fr.cnrs.liris.common.geo.{FeatureCollection, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future => ScalaFuture}

case class ListDatasetsRequest(filters: Seq[QueryFilter] = Seq.empty)

case class GetDatasetRequest(
    @RouteParam dataset: String,
    filters: Seq[QueryFilter] = Seq.empty)

case class QueryEventsRequest(
    @RouteParam dataset: String,
    filters: Seq[QueryFilter] = Seq.empty,
    aggregate: Option[QueryAggregate],
    limit: Option[Int],
    offset: Option[Int],
    @QueryParam @OneOf(Array("geojson", "json")) format: String = "json")

/**
 * Controller handling API endpoints.
 *
 * @author Vincent Primault <vincent.primault@liris.cnrs.fr>
 */
@Singleton
class QueryController @Inject()(queryExecutor: QueryExecutor) extends Controller {
  private[this] val datasetNames = Seq("cabspotting")

  get("/datasets") { request: ListDatasetsRequest =>
    val futures = datasetNames.map { name =>
      val future = queryExecutor.timeRange(name, request.filters)
      toTwitterFuture(future).map(timeRange => Dataset(name, timeRange._1, timeRange._2))
    }
    TwitterFuture.collect(futures)
  }

  get("/datasets/:dataset") { request: GetDatasetRequest =>
    if (datasetNames.contains(request.dataset)) {
      val future = queryExecutor.timeRange(request.dataset, request.filters)
      toTwitterFuture(future).map(timeRange => Dataset(request.dataset, timeRange._1, timeRange._2))
    } else {
      response.notFound
    }
  }

  post("/datasets/:dataset/events") { request: QueryEventsRequest =>
    val future = request.aggregate match {
      case Some(agg: QueryAggregateHeatmap) =>
        queryExecutor.events(request.dataset, request.filters, agg)
      case _ => ScalaFuture.failed(new IllegalArgumentException("Unsupported aggregate"))
    }
    toTwitterFuture(future)
        .map(buckets => if (request.format == "geojson") createHeatMap(buckets) else buckets)
        .map(response.ok.contentTypeJson().body)
  }

  private def createHeatMap(buckets: Seq[HeatmapBucket]) = {
    val features = buckets.map { bucket =>
      val cell = new S2Cell(bucket.cellId)
      val ring = (Seq.tabulate(4)(cell.getVertex) ++ Seq(cell.getVertex(0))).map(point => GeoPoint(LatLng(point)))
      val properties = Map("id" -> bucket.cellId.toToken, "value" -> bucket.count)
      Feature(Polygon.lines(Seq(LineString.points(ring))), properties)
    }
    val bbox = boundingBox(buckets.map(bucket => new S2Cell(bucket.cellId).getCenter))
    FeatureCollection(features, bbox)
  }

  private[this] val pointOrdering = implicitly[Ordering[S2Point]]

  private def boundingBox(points: Iterable[S2Point]) = {
    if (points.nonEmpty) {
      val min = LatLng(points.min(pointOrdering))
      val max = LatLng(points.max(pointOrdering))
      Some(Seq(min.lng.degrees, min.lat.degrees, max.lng.degrees, max.lat.degrees))
    } else {
      None
    }
  }

  private def toTwitterFuture[T](future: ScalaFuture[T]) =
    UtilBijections.twitter2ScalaFuture(global).invert(future)
}
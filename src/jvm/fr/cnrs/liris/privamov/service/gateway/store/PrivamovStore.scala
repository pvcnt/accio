package fr.cnrs.liris.privamov.service.gateway.store

import com.google.inject.Inject
import com.google.inject.name.Named
import com.twitter.querulous.driver.DatabaseDriver
import com.twitter.querulous.evaluator.{QueryEvaluator, QueryEvaluatorFactory}
import fr.cnrs.liris.common.geo.LatLng
import fr.cnrs.liris.accio.core.model.Record
import org.joda.time.{DateTime, Instant}

import scala.collection.mutable

/**
 * Data provider reading data from a PostgreSQL/PostGIS database using standard Priva'Mov schema.
 *
 * @author Vincent Primault <vincent.primault@liris.cnrs.fr>
 * @param evaluator A query evaluator
 * @param name      Store name
 */
class PrivamovStore(evaluator: QueryEvaluator, override val name: String) extends EventStore {
  override def contains(user: String): Boolean = getSourceId(user).isDefined

  override def sources(views: Set[View], limit: Option[Int], startAfter: Option[String]): Seq[Source] = {
    var sqlTail = sourcesWhereClause(views)
    startAfter.foreach { startAfter =>
      sqlTail += s" and device::character varying > '${imeiToUuid(startAfter)}'"
    }
    sqlTail += " order by device::character varying"
    limit.foreach { limit =>
      sqlTail += s" limit $limit"
    }
    val sql = s"select distinct device::character varying from source where $sqlTail"
    val ids = evaluator.select(sql)(rs => uuidToImei(rs.getString(1)))
    ids.map(id => Source(id))
  }

  override def countSources(views: Set[View]): Int = {
    val whereClause = sourcesWhereClause(views)
    evaluator.count(s"select count(1) from source where $whereClause")
  }

  override def features(views: Set[View], limit: Option[Int] = None, sample: Boolean = false): Seq[Record] = {
    var selectTail = ""
    var joinClause = ""
    val whereClause = s" where ${locationWhereClause(views)}"
    val limitClause = if (limit.isDefined && !sample) s" limit ${limit.get}" else ""

    if (views.exists(_.source.isDefined)) {
      selectTail += ", device::character varying"
      joinClause += " join source on source.id = location.source"
    }
    if (limit.isDefined && sample) {
      selectTail += ", row_number() over() rnum"
    }

    var sql = s"select extract(epoch from timestamp), st_y(position), st_x(position) $selectTail" +
        s" from location $joinClause $whereClause order by timestamp $limitClause"

    if (limit.isDefined && sample) {
      val count = evaluator.count(s"select count(1) from location $whereClause")
      if (count > limit.get) {
        val outOf = count.toDouble / limit.get
        sql = s"select * from ($sql) q where rnum % $outOf < 1"
      }
    }
    //println(sql)

    evaluator.select(sql) { rs =>
      val imei = uuidToImei(rs.getString(4))
      val point = LatLng.degrees(rs.getDouble(2), rs.getDouble(3)).toPoint
      val time = new Instant(rs.getLong(1) * 1000)
      Record(imei, point, time)
    }
  }

  override def countFeatures(views: Set[View]): Int = {
    val whereClause = locationWhereClause(views)
    evaluator.count(s"select count(1) from location where $whereClause")
  }

  override def apply(id: String): Source = {
    val whereClause = s"source = ${sourceId(id)}"
    val countSql = s"select count(1) from location where $whereClause"
    val count = evaluator.count(countSql)
    if (count > 0) {
      val firstLastSeenSql = s"select extract(epoch from min(timestamp)), extract(epoch from max(timestamp)) from location where $whereClause"
      val firstLastSeen = evaluator.selectOne(firstLastSeenSql)(rs => (new DateTime(rs.getLong(1) * 1000), new DateTime(rs.getLong(2) * 1000)))

      Source(id, firstLastSeen.map(_._1), firstLastSeen.map(_._2), Some(count))
    } else {
      Source(id, None, None, Some(0))
    }
  }

  private def uuidToImei(uuid: String) = uuid.replace("-", "").take(15)

  private def imeiToUuid(imei: String) = {
    require(imei.length == 15, s"Invalid IMEI number (got '$imei')")
    imei.substring(0, 8) + '-' + imei.substring(8, 12) + '-' + imei.substring(12, 15) + "0-0000-000000000000"
  }

  private def getSourceId(id: String): Option[Int] = {
    val sql = "select id from source where device::character varying = ?"
    evaluator.selectOne(sql, imeiToUuid(id))(_.getInt(1))
  }

  private def sourceId(id: String): Int = {
    getSourceId(id) match {
      case Some(source) => source
      case None => throw new IllegalArgumentException(s"Invalid IMEI number $id")
    }
  }

  private def sourcesWhereClause(views: Set[View]): String = {
    if (views.exists(_.source.isEmpty)) {
      "true"
    } else if (views.isEmpty) {
      "false"
    } else {
      val uuids = views.flatMap(_.source.map(imeiToUuid)).map(uuid => s"'$uuid'")
      s"device::character varying in(${uuids.mkString(",")})"
    }
  }

  private def locationWhereClause(views: Set[View]): String = {
    if (views.isEmpty) {
      "false"
    } else {
      views.map(view => locationWhereClause(view)).mkString(" or ")
    }
  }

  private def locationWhereClause(view: View): String = {
    val where = mutable.ListBuffer.empty[String]
    view.source.foreach { id =>
      where += s"source = ${sourceId(id)}"
    }
    view.startAfter.foreach { startAfter =>
      where += s"timestamp >= timestamp '$startAfter'"
    }
    view.endBefore.foreach { startAfter =>
      where += s"timestamp <= timestamp '$startAfter'"
    }
    if (where.nonEmpty) where.mkString(" and ") else "true"
  }
}

/**
 * Factory for [[PrivamovStore]].
 *
 * @author Vincent Primault <vincent.primault@liris.cnrs.fr>
 * @param queryEvaluatorFactory A query evaluator factory
 */
class PrivamovStoreFactory @Inject()(@Named("privamov") queryEvaluatorFactory: QueryEvaluatorFactory) extends EventStoreFactory {
  override def create(name: String): EventStore = {
    val upperName = name.toUpperCase
    Set("HOST", "BASE", "PASS", "USER")
        .foreach(v => require(sys.env.contains(s"QUERULOUS__${upperName}_$v"), s"You must provide a value for envvar QUERULOUS__${upperName}_$v"))
    val hostname = sys.env(s"QUERULOUS__${upperName}_HOST")
    val dbname = sys.env(s"QUERULOUS__${upperName}_BASE")
    val username = sys.env(s"QUERULOUS__${upperName}_USER")
    val password = sys.env(s"QUERULOUS__${upperName}_PASS")
    val driver = DatabaseDriver("postgresql")
    val queryEvaluator = queryEvaluatorFactory.apply(Seq(hostname), dbname, username, password, driver = driver)
    new PrivamovStore(queryEvaluator, name)
  }
}
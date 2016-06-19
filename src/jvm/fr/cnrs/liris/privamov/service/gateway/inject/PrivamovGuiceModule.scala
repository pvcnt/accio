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

package fr.cnrs.liris.privamov.service.gateway.inject

import com.google.inject.Provides
import com.google.inject.name.Named
import com.twitter.conversions.time._
import com.twitter.finatra.httpclient.{HttpClient, RichHttpClient}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.inject.TwitterModule
import com.twitter.querulous.database.{ApachePoolingDatabaseFactory, DatabaseFactory}
import com.twitter.querulous.evaluator.{QueryEvaluatorFactory, StandardQueryEvaluatorFactory}
import com.twitter.querulous.query.{QueryFactory, SqlQueryFactory}
import fr.cnrs.liris.privamov.service.gateway.auth.{Firewall, FleetFirewall}
import fr.cnrs.liris.privamov.service.gateway.store.{EventStoreFactory, PrivamovStoreFactory}
import net.codingwell.scalaguice.ScalaMapBinder

/**
 * Provide support for Priva'Mov event stores. They are essentially PostGIS stores interacting with
 * the Fleet service to determine which data to display.
 *
 * @author Vincent Primault <vincent.primault@liris.cnrs.fr>
 */
object PrivamovGuiceModule extends TwitterModule {
  val fleetServer = flag[String]("viz.fleet_server", "privamov.liris.cnrs.fr:80", "Address of the Fleet API")

  override protected def configure(): Unit = {
    val stores = ScalaMapBinder.newMapBinder[String, EventStoreFactory](binder)
    stores.addBinding("privamov").to[PrivamovStoreFactory]

    bind[Firewall].to[FleetFirewall]
  }

  @Provides
  @Named("privamov")
  def providesDatabaseFactory: DatabaseFactory =
    new ApachePoolingDatabaseFactory(10, 10, 1 second, 10 millis, false, 60 seconds)

  @Provides
  @Named("privamov")
  def providesQueryFactory: QueryFactory = new SqlQueryFactory

  @Provides
  @Named("privamov")
  def providesQueryEvaluatorFactory(@Named("privamov") databaseFactory: DatabaseFactory, @Named("privamov") queryFactory: QueryFactory): QueryEvaluatorFactory =
    new StandardQueryEvaluatorFactory(databaseFactory, queryFactory)

  @Provides
  @Named("fleet")
  def providesFleetHttpClient(mapper: FinatraObjectMapper): HttpClient = {
    val address = fleetServer().split(":")
    val hostname = address.head
    val httpService = RichHttpClient.newClientService(dest = fleetServer())
    new HttpClient(hostname, httpService, mapper = mapper)
  }
}
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

package fr.cnrs.liris.privamov.service.gateway

import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import com.twitter.conversions.time._
import com.twitter.finatra.httpclient.{HttpClient, RequestBuilder}
import com.twitter.util.{Await, Try}
import org.joda.time.DateTime

/**
 * A lending represents a device lent to somebody.
 *
 * @param imei      Device's IMEI
 * @param firstName Person's first name
 * @param lastName  Person's first name
 * @param email     Person's email address
 * @param segment   Campaign identifier
 * @param started   Lending start time
 * @param ended     Lending end time (not set if the lending is still on-going)
 */
case class Lending(
    imei: String,
    segment: Option[String],
    firstName: Option[String],
    lastName: Option[String],
    email: Option[String],
    started: DateTime,
    ended: Option[DateTime])

/**
 * Simple client providing access to the Fleet API.
 *
 * @param httpClient HTTP client, bound to Fleet server
 */
@Singleton
class FleetClient @Inject()(@Named("fleet") httpClient: HttpClient) {
  /**
   * Return the lending associated with a given token, if it exists.
   *
   * @param token Token
   */
  def getLending(token: String): Option[Lending] = {
    val future = httpClient.executeJson[Lending](RequestBuilder.get(s"/fleet/api/lending/$token"))
    Try {
      Await.result(future, 5.seconds)
    }.toOption
  }
}
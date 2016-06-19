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

import com.twitter.inject.TwitterModule
import fr.cnrs.liris.privamov.service.gateway.store.EventStoreFactory
import net.codingwell.scalaguice.ScalaMapBinder

object VizGuiceModule extends TwitterModule {
  flag[String]("viz.stores", "", "Comma-separated list of stores to use (type.name)")
  flag[Int]("viz.standard_limit", 100, "Maximum number of elements that can be retrieved in standard listings)")
  flag[Int]("viz.extended_limit", 2000, "Maximum number of elements that can be retrieved in extended listings)")

  override protected def configure(): Unit = {
    // Event stores are configured as plugins added to this map binder.
    ScalaMapBinder.newMapBinder[String, EventStoreFactory](binder)
  }
}
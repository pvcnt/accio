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

package fr.cnrs.liris.privamov.service.gateway.auth

import fr.cnrs.liris.privamov.service.gateway.store.{EventStore, View}

case class AccessToken(scopes: Set[Scope], acl: AccessControlList) {
  def in(scope: Scope): Boolean = scopes.contains(scope)
}

object AccessToken {
  def everything: AccessToken = new AccessToken(Scope.values, AccessControlList.everything)

  def apply(scopes: Set[Scope], aces: AccessControlEntry*): AccessToken =
    new AccessToken(scopes, AccessControlList(aces.toSet))
}

sealed trait Scope

object Scope {

  case object Datasets extends Scope

  case object Exports extends Scope

  def values: Set[Scope] = Set(Datasets, Exports)

}

case class AccessControlList(entries: Set[AccessControlEntry]) {
  def accessible(store: EventStore): Boolean =
    entries.exists(e => e.dataset.isEmpty || e.dataset.contains(store.name))

  def resolve(dataset: String): AccessControlEntry = {
    val allowedViews = entries.filter(e => e.dataset.isEmpty || e.dataset.contains(dataset)).flatMap(_.views)
    AccessControlEntry(Some(dataset), allowedViews)
  }
}

object AccessControlList {
  def everything: AccessControlList = new AccessControlList(Set(AccessControlEntry.everything))

  def empty: AccessControlList = new AccessControlList(Set.empty)
}

case class AccessControlEntry(dataset: Option[String], views: Set[View]) {
  def canonicalize: AccessControlEntry = {
    var prevSize = 0
    var res = views
    do {
      prevSize = res.size
      res = res.filterNot(v1 => res.exists(v2 => v1 != v2 && v2.includes(v1)))
    } while (res.size > 1 && res.size < prevSize)
    AccessControlEntry(dataset, views)
  }

  def resolve(views: Set[View]): AccessControlEntry = {
    val allowedViews = views.flatMap(_.restrict(this.views))
    val ace = AccessControlEntry(dataset, allowedViews)
    ace.canonicalize
  }
}

object AccessControlEntry {
  def everything: AccessControlEntry = new AccessControlEntry(None, Set(View.everything))
}
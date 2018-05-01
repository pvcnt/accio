/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
 *
 * Accio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Accio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Accio.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.cnrs.liris.lumos.domain

import scala.collection.mutable

case class LabelSelector(requirements: Set[LabelSelector.Requirement]) {
  def matches(labels: Map[String, String]): Boolean = requirements.forall(_.matches(labels))

  def +(other: LabelSelector): LabelSelector = LabelSelector(requirements ++ other.requirements)
}

object LabelSelector {

  case class Requirement(key: String, op: Op, values: Set[String]) {
    def matches(labels: Map[String, String]): Boolean = op.matches(labels.get(key), values)
  }

  sealed trait Op {
    def matches(value: Option[String], values: Set[String]): Boolean
  }

  case object In extends Op {
    override def matches(value: Option[String], values: Set[String]): Boolean = {
      value.exists(values.contains)
    }
  }

  case object NotIn extends Op {
    override def matches(value: Option[String], values: Set[String]): Boolean = {
      value.forall(v => !values.contains(v))
    }
  }

  case object Present extends Op {
    override def matches(value: Option[String], values: Set[String]): Boolean = value.isDefined
  }

  case object Absent extends Op {
    override def matches(value: Option[String], values: Set[String]): Boolean = value.isEmpty
  }

  def present(key: String): LabelSelector = LabelSelector(Set(Requirement(key, Present, Set.empty)))

  def absent(key: String): LabelSelector = LabelSelector(Set(Requirement(key, Absent, Set.empty)))

  def in(key: String, values: Set[String]): LabelSelector = LabelSelector(Set(Requirement(key, In, values)))

  def notIn(key: String, values: Set[String]): LabelSelector = LabelSelector(Set(Requirement(key, NotIn, values)))

  def equal(key: String, value: String): LabelSelector = LabelSelector(Set(Requirement(key, In, Set(value))))

  def notEqual(key: String, value: String): LabelSelector = LabelSelector(Set(Requirement(key, NotIn, Set(value))))

  def parse(str: String): LabelSelector = ???
}
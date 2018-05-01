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

package fr.cnrs.liris.util.collect

import scala.math.Ordered.orderingToOrdered

/**
 * Partial port of Guava's own Range class to Scala, in order to support [[Ordering]]s instead
 * of [[Comparable]]s.
 *
 * @param getLowerEndpoint Lower endpoint, if any.
 * @param lowerBoundType   Lower bound type.
 * @param getUpperEndpoint Upper endpoint, if any.
 * @param upperBoundType   Upper bound type.
 * @tparam T Type of elements inside this range.
 */
case class Range[T: Ordering] private(
  getLowerEndpoint: Option[T],
  lowerBoundType: Range.BoundType,
  getUpperEndpoint: Option[T],
  upperBoundType: Range.BoundType)
  extends Serializable {

  require(isValid, s"Invalid range: $this")

  def hasLowerBound: Boolean = getLowerEndpoint.isDefined

  def hasUpperBound: Boolean = getUpperEndpoint.isDefined

  def lowerEndpoint: T = getLowerEndpoint.get

  def upperEndpoint: T = getUpperEndpoint.get

  def isEmpty: Boolean = {
    getLowerEndpoint.isDefined &&
      getUpperEndpoint.isDefined &&
      lowerEndpoint == upperEndpoint &&
      lowerBoundType != upperBoundType
  }

  def contains(value: T): Boolean = {
    (!hasLowerBound || (lowerBoundType == Range.Closed && lowerEndpoint <= value || lowerEndpoint < value)) &&
      (!hasUpperBound || (upperBoundType == Range.Closed && upperEndpoint >= value || upperEndpoint > value))
  }

  def isConnected(other: Range[T]): Boolean = {
    val (lower, lowerType, upper, upperType) = getIntersection(other)
    isConnected(lower, lowerType, upper, upperType)
  }

  def intersection(other: Range[T]): Range[T] = {
    val (lower, lowerType, upper, upperType) = getIntersection(other)
    require(isConnected(lower, lowerType, upper, upperType), s"$this is not connected with $other")
    new Range(lower, lowerType, upper, upperType)
  }

  override def toString: String = {
    val sb = new StringBuilder
    lowerBoundType match {
      case Range.Open => sb ++= "("
      case Range.Closed => sb ++= "["
    }
    getLowerEndpoint match {
      case Some(v) => sb ++= v.toString
      case None => sb ++= "-\u221e"
    }
    sb ++= ".."
    getUpperEndpoint match {
      case Some(v) => sb ++= v.toString
      case None => sb ++= "+\u221e"
    }
    upperBoundType match {
      case Range.Open => sb ++= ")"
      case Range.Closed => sb ++= "]"
    }
    sb.toString
  }

  private def isValid =
    if (!hasLowerBound || !hasUpperBound) {
      true
    } else if (lowerEndpoint < upperEndpoint) {
      true
    } else if (lowerEndpoint == upperEndpoint && (lowerBoundType == Range.Closed || upperBoundType == Range.Closed)) {
      true
    } else {
      false
    }

  private def getIntersection(other: Range[T]) = {
    val (lower, lowerType) = if (hasLowerBound && other.hasLowerBound) {
      if (lowerEndpoint > other.lowerEndpoint) (getLowerEndpoint, lowerBoundType) else (other.getLowerEndpoint, other.lowerBoundType)
    } else if (hasLowerBound) {
      (getLowerEndpoint, lowerBoundType)
    } else if (other.hasLowerBound) {
      (other.getLowerEndpoint, other.lowerBoundType)
    } else {
      (None, Range.Open)
    }
    val (upper, upperType) = if (hasUpperBound && other.hasUpperBound) {
      if (upperEndpoint < other.upperEndpoint) (getUpperEndpoint, upperBoundType) else (other.getUpperEndpoint, other.upperBoundType)
    } else if (hasUpperBound) {
      (getUpperEndpoint, upperBoundType)
    } else if (other.hasUpperBound) {
      (other.getUpperEndpoint, other.upperBoundType)
    } else {
      (None, Range.Open)
    }
    (lower, lowerType, upper, upperType)
  }

  private def isConnected(lower: Option[T], lowerType: Range.BoundType, upper: Option[T], upperType: Range.BoundType) = {
    if (upper < lower) {
      false
    } else if (upper == lower && lowerType == Range.Open && upperType == Range.Open) {
      false
    } else {
      true
    }
  }
}

/**
 * Factory of [[Range]].
 */
object Range {

  /**
   * Bound types for range.
   */
  sealed trait BoundType

  /**
   * Open bound type.
   */
  case object Open extends BoundType

  /**
   * Closed bound type.
   */
  case object Closed extends BoundType

  def range[T: Ordering](lower: T, lowerType: BoundType, upper: T, upperType: BoundType): Range[T] =
    new Range(Some(lower), lowerType, Some(upper), upperType)

  def downTo[T: Ordering](lower: T, lowerType: BoundType): Range[T] =
    new Range(Some(lower), lowerType, None, Open)

  def upTo[T: Ordering](upper: T, upperType: BoundType): Range[T] =
    new Range(None, Open, Some(upper), upperType)

  def open[T: Ordering](lower: T, upper: T): Range[T] =
    new Range(Some(lower), Open, Some(upper), Open)

  def closed[T: Ordering](lower: T, upper: T): Range[T] =
    new Range(Some(lower), Closed, Some(upper), Closed)

  def closedOpen[T: Ordering](lower: T, upper: T): Range[T] =
    new Range(Some(lower), Closed, Some(upper), Open)

  def openClosed[T: Ordering](lower: T, upper: T): Range[T] =
    new Range(Some(lower), Open, Some(upper), Closed)

  def greaterThan[T: Ordering](lower: T): Range[T] = new Range(Some(lower), Open, None, Open)

  def atLeast[T: Ordering](lower: T): Range[T] = new Range(Some(lower), Closed, None, Open)

  def lessThan[T: Ordering](upper: T): Range[T] = new Range(None, Open, Some(upper), Open)

  def atMost[T: Ordering](upper: T): Range[T] = new Range(None, Open, Some(upper), Closed)

  def all[T: Ordering]: Range[T] = new Range(None, Open, None, Open)
}
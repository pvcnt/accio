package fr.cnrs.liris.common.collect

/**
 * Partial port of Guava's own Range class to Scala, in order to support [[Ordering]]s instead of [[Comparable]]s.
 *
 * @param getLowerEndpoint Lower endpoint, if any.
 * @param lowerBoundType   Lower bound type.
 * @param getUpperEndpoint Upper endpoint, if any.
 * @param upperBoundType   Upper bound type.
 * @tparam T Type of elements inside this range.
 */
case class Range[T: Ordering] private(getLowerEndpoint: Option[T], lowerBoundType: BoundType, getUpperEndpoint: Option[T], upperBoundType: BoundType) extends Serializable {

  import scala.math.Ordered.orderingToOrdered

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
    (!hasLowerBound || (lowerBoundType == BoundType.Closed && lowerEndpoint <= value || lowerEndpoint < value)) &&
      (!hasUpperBound || (upperBoundType == BoundType.Closed && upperEndpoint >= value || upperEndpoint > value))
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
      case BoundType.Open => sb ++= "("
      case BoundType.Closed => sb ++= "["
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
      case BoundType.Open => sb ++= ")"
      case BoundType.Closed => sb ++= "]"
    }
    sb.toString
  }

  private def isValid =
    if (!hasLowerBound || !hasUpperBound) {
      true
    } else if (lowerEndpoint < upperEndpoint) {
      true
    } else if (lowerEndpoint == upperEndpoint && (lowerBoundType == BoundType.Closed || upperBoundType == BoundType.Closed)) {
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
      (None, BoundType.Open)
    }
    val (upper, upperType) = if (hasUpperBound && other.hasUpperBound) {
      if (upperEndpoint < other.upperEndpoint) (getUpperEndpoint, upperBoundType) else (other.getUpperEndpoint, other.upperBoundType)
    } else if (hasUpperBound) {
      (getUpperEndpoint, upperBoundType)
    } else if (other.hasUpperBound) {
      (other.getUpperEndpoint, other.upperBoundType)
    } else {
      (None, BoundType.Open)
    }
    println(s"intersect: $lower, $lowerType, $upper, $upperType")
    (lower, lowerType, upper, upperType)
  }

  private def isConnected(lower: Option[T], lowerType: BoundType, upper: Option[T], upperType: BoundType) = {
    if (upper < lower) {
      false
    } else if (upper == lower && lowerType == BoundType.Open && upperType == BoundType.Open) {
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
  def range[T: Ordering](lower: T, lowerType: BoundType, upper: T, upperType: BoundType): Range[T] =
    new Range(Some(lower), lowerType, Some(upper), upperType)

  def downTo[T: Ordering](lower: T, lowerType: BoundType): Range[T] =
    new Range(Some(lower), lowerType, None, BoundType.Open)

  def upTo[T: Ordering](upper: T, upperType: BoundType): Range[T] =
    new Range(None, BoundType.Open, Some(upper), upperType)

  def open[T: Ordering](lower: T, upper: T): Range[T] =
    new Range(Some(lower), BoundType.Open, Some(upper), BoundType.Open)

  def closed[T: Ordering](lower: T, upper: T): Range[T] =
    new Range(Some(lower), BoundType.Closed, Some(upper), BoundType.Closed)

  def closedOpen[T: Ordering](lower: T, upper: T): Range[T] =
    new Range(Some(lower), BoundType.Closed, Some(upper), BoundType.Open)

  def openClosed[T: Ordering](lower: T, upper: T): Range[T] =
    new Range(Some(lower), BoundType.Open, Some(upper), BoundType.Closed)

  def greaterThan[T: Ordering](lower: T): Range[T] =
    new Range(Some(lower), BoundType.Open, None, BoundType.Open)

  def atLeast[T: Ordering](lower: T): Range[T] =
    new Range(Some(lower), BoundType.Closed, None, BoundType.Open)

  def lessThan[T: Ordering](upper: T): Range[T] =
    new Range(None, BoundType.Open, Some(upper), BoundType.Open)

  def atMost[T: Ordering](upper: T): Range[T] =
    new Range(None, BoundType.Open, Some(upper), BoundType.Closed)

  def all[T: Ordering]: Range[T] = new Range(None, BoundType.Open, None, BoundType.Open)
}

/**
 * Bound types for range.
 */
sealed trait BoundType

object BoundType {

  /**
   * Open bound type.
   */
  case object Open extends BoundType

  /**
   * Closed bound type.
   */
  case object Closed extends BoundType

}

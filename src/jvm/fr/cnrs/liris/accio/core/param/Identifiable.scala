package fr.cnrs.liris.accio.core.param

import java.util.concurrent.atomic.AtomicInteger

/**
 * Trait for objects that can be uniquely identified by an identifier.
 */
trait Identifiable {
  /**
   * Return the unique identifier of this object. It should be unique among all objects of the same
   * type. Multiple calls to this methods should always return the same value.
   */
  def uid: String

  override def equals(other: Any): Boolean = other match {
    case o: Identifiable => o.uid == uid
    case _ => false
  }

  override def hashCode: Int = uid.hashCode
}

/**
 * Helper for [[Identifiable]] objects.
 */
object Identifiable {
  private[this] val generator = new AtomicInteger()

  /**
   * Generate a unique identifier including a given prefix.
   *
   * @param prefix A prefix
   */
  def uniqid(prefix: String): String = s"$prefix-${generator.incrementAndGet()}"
}
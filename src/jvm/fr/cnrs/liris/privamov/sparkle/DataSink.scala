package fr.cnrs.liris.privamov.sparkle

trait DataSink[T] {
  def write(elements: Iterator[T]): Unit
}

/**
 * An encoder converts a plain object into a (binary) record.
 *
 * @tparam T Plain object type
 */
trait Encoder[T] {
  /**
   * Encodes an object into a binary record.
   *
   * @param obj Plain object
   * @return Binary content
   */
  def encode(obj: T): Array[Byte]
}
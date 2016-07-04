package fr.cnrs.liris.accio.core.dataset

trait DataSink[T] {
  def write(key: String, elements: Iterator[T]): Unit
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
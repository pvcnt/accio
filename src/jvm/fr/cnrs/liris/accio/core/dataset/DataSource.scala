package fr.cnrs.liris.accio.core.dataset

import java.nio.charset.Charset
import java.nio.file.{Files, Paths}

import com.google.common.base.Charsets

trait DataSource[T] {
  def keys: Seq[String]

  def read(key: String): Iterable[T]
}

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

/**
 * A decoder converts a binary (record) into a plain object.
 *
 * @tparam T Plain object type
 */
trait Decoder[T] {
  /**
   * Decodes a binary record into an object.
   *
   * @param key   Key associated with the file containing this record
   * @param bytes Binary content
   * @return Plain object
   */
  def decode(key: String, bytes: Array[Byte]): Option[T]
}

class DirectorySource[T](url: String, extension: String, decoder: Decoder[T]) extends DataSource[T] {
  private[this] val path = Paths.get(url)

  override final def keys: Seq[String] =
    path.toFile.listFiles.map(_.toPath.getFileName.toString.dropRight(extension.length))

  override final def read(key: String): Iterable[T] =
    decoder.decode(key, Files.readAllBytes(path.resolve(s"$key$extension"))).toIterable
}

class TextLineDecoder[T](decoder: Decoder[T], headerLines: Int = 0, charset: Charset = Charsets.UTF_8) extends Decoder[Seq[T]] {
  override def decode(key: String, bytes: Array[Byte]): Option[Seq[T]] = {
    val lines = new String(bytes, charset).split("\n").drop(headerLines)
    val elements = lines.flatMap(line => decoder.decode(key, line.getBytes(charset)))
    if (elements.nonEmpty) Some(elements) else None
  }
}
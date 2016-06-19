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

package fr.cnrs.liris.common.io.source

import java.nio.charset.Charset

import com.google.common.base.Charsets

trait Encoder[T] extends Serializable {
  def encode(obj: T): Array[Byte]
}

trait Decoder[T] extends Serializable {
  def decode(record: EncodedRecord): Option[T]
}

trait Codec[T] extends Encoder[T] with Decoder[T]

/**
 * Codec doing nothing and handling raw bytes sequences.
 */
class IdentityCodec extends Codec[Array[Byte]] {
  override def decode(record: EncodedRecord): Option[Array[Byte]] = Some(record.bytes)

  override def encode(obj: Array[Byte]): Array[Byte] = obj
}

/**
 * Codec handling the (de)serialization of strings from bytes.
 *
 * @param charset Charset to use (UTF-8 by default)
 */
class StringCodec(charset: Charset = Charsets.UTF_8) extends Codec[String] {
  override def decode(record: EncodedRecord): Option[String] =
    Some(new String(record.bytes, charset))

  override def encode(obj: String): Array[Byte] = obj.getBytes(charset)
}
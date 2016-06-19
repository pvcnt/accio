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
import fr.cnrs.liris.common.io.getter.Getter
import fr.cnrs.liris.common.collect.BaseIterator

import scala.collection.AbstractIterable


/**
 * A record reader extracts binary records from a file. Implementations should be thread-safe.
 */
trait RecordReader extends Serializable {
  /**
   * Read records inside `file`.
   *
   * @param file A file where to read records from
   */
  def read(file: IndexedFile): Iterable[EncodedRecord]
}

/**
 * An encoded (or binary) record stored inside a file.
 *
 * @param url    URL of the file containing this record
 * @param labels Labels applied on this record
 * @param bytes  Binary representation
 */
case class EncodedRecord(url: String, labels: Set[String], bytes: Array[Byte])

abstract class BaseRecordReader extends RecordReader {
  override final def read(file: IndexedFile): Iterable[EncodedRecord] = {
    val getter = Getter(file.url, file.offset, file.length)
    new AbstractIterable[EncodedRecord] {
      override def iterator: Iterator[EncodedRecord] = new BaseIterator[EncodedRecord] {
        private[this] var idx = 0

        override protected def computeNext(): Option[EncodedRecord] = {
          val res = readNext(idx, file, getter)
          if (res.isEmpty) {
            getter.close()
          } else {
            idx += 1
          }
          res.map(bytes => EncodedRecord(file.url, file.labels, bytes))
        }
      }
    }
  }

  protected def readNext(idx: Int, file: IndexedFile, getter: Getter): Option[Array[Byte]]
}

abstract class BaseSingleRecordReader extends BaseRecordReader {
  override protected def readNext(idx: Int, file: IndexedFile, getter: Getter): Option[Array[Byte]] =
    if (idx == 0) Some(computeValue(file, getter)) else None

  protected def computeValue(file: IndexedFile, getter: Getter): Array[Byte]
}

/**
 * Reader producing records delimited by a variable length. Each record is preceded by its length,
 * coded as a 4-bytes integer.
 */
class VariableLengthRecordReader extends BaseRecordReader {
  override protected def readNext(idx: Int, file: IndexedFile, getter: Getter): Option[Array[Byte]] =
    getter.readInt().flatMap(getter.read)
}

/**
 * Reader producing a record per line.
 *
 * @param headerLines Number of lines to skip at the beginning
 * @param charset     Charset to use to decode lines
 */
class TextLineReader(headerLines: Int = 0, charset: Charset = Charsets.UTF_8) extends BaseRecordReader {
  require(headerLines >= 0, s"headerLines must be >= 0 (got $headerLines)")

  override protected def readNext(idx: Int, file: IndexedFile, getter: Getter): Option[Array[Byte]] = {
    if (idx == 0 && headerLines > 0) {
      (0 until headerLines).foreach(_ => getter.readLine())
    }
    getter.readLine().map(line => line.getBytes(charset))
  }
}

/**
 * Reader producing records delimited by a constant length for each.
 *
 * @param recordLength Length of each record (in bytes)
 * @param headerLength Number of bytes to skip at the beginning
 */
class FixedLengthRecordReader(recordLength: Int, headerLength: Int = 0) extends BaseRecordReader {
  require(headerLength >= 0, s"headerLength must be >= 0 (got $headerLength)")
  require(recordLength >= 1, s"recordLength must be >= 1 (got $recordLength)")

  override protected def readNext(idx: Int, file: IndexedFile, getter: Getter): Option[Array[Byte]] = {
    if (idx == 0 && headerLength > 0) {
      getter.skip(headerLength)
    }
    getter.read(recordLength)
  }
}

/**
 * Reader producing a single record for each file, containing the whole file contents.
 */
class WholeFileReader extends BaseSingleRecordReader {
  override protected def computeValue(file: IndexedFile, getter: Getter): Array[Byte] =
    getter.readFully()
}

/**
 * Reader producing a single record for each file, containing the file URL.
 */
class FilenameReader extends BaseSingleRecordReader {
  override protected def computeValue(file: IndexedFile, getter: Getter): Array[Byte] =
    file.url.getBytes
}
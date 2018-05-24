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

package fr.cnrs.liris.util.scrooge

import java.io.OutputStreamWriter
import java.nio.ByteBuffer
import java.nio.charset.Charset

import com.google.common.base.Charsets
import com.google.common.escape.Escapers
import com.twitter.util.Base64StringEncoder
import org.apache.thrift.TException
import org.apache.thrift.protocol._
import org.apache.thrift.transport.TTransport

import scala.collection.mutable
import scala.util.control.NonFatal

private[scrooge] class TTextProtocol(transport: TTransport, charset: Charset) extends TProtocol(transport)
  with WriteOnlyTProtocol {

  private[this] val contextStack = mutable.ListBuffer[ParsingContext](ParsingContext.Null)
  private[this] val writer = new OutputStreamWriter(new TTransportOutputStream(transport), charset)
  reset()

  override def writeI16(i: Short): Unit = writeValue(i.toString)

  override def writeByte(b: Byte): Unit = writeValue(b.toString)

  override def writeFieldBegin(field: TField): Unit = wrapExceptions {
    writer.write("  " * contextStack.map(_.indent).sum)
    writer.write(s"${field.name} = ")
  }

  override def writeFieldEnd(): Unit = {
    writer.write("\n")
  }

  override def writeFieldStop(): Unit = {}

  override def writeListBegin(list: TList): Unit = writeSequenceBegin()

  override def writeListEnd(): Unit = writeSequenceEnd()

  override def writeSetBegin(set: TSet): Unit = writeSequenceBegin()

  override def writeSetEnd(): Unit = writeSequenceEnd()

  override def writeStructBegin(struct: TStruct): Unit = writeObjectBegin(new ParsingContext.Struct)

  override def writeStructEnd(): Unit = writeObjectEnd()

  override def writeMapBegin(tMap: TMap): Unit = writeObjectBegin(new ParsingContext.Map)

  override def writeMapEnd(): Unit = writeObjectEnd()

  override def writeBinary(byteBuffer: ByteBuffer): Unit = {
    writeValue(Base64StringEncoder.encode(byteBuffer.array))
  }

  override def writeBool(b: Boolean): Unit = writeValue(b.toString)

  override def writeI32(i: Int): Unit = writeValue(i.toString)

  override def writeDouble(v: Double): Unit = writeValue(v.toString)

  override def writeI64(l: Long): Unit = writeValue(l.toString)

  override def writeString(s: String): Unit = writeValue(TTextProtocol.escaper.escape(s))

  override def writeMessageBegin(tMessage: TMessage): Unit = throw new UnsupportedOperationException

  override def writeMessageEnd(): Unit = throw new UnsupportedOperationException

  private def writeValue(s: String): Unit = wrapExceptions {
    currentContext.write()
    if (currentContext.isMapKey) {
      writer.write(s"$s = ")
    } else {
      writer.write(s)
    }
  }

  private def writeSequenceBegin(): Unit = {
    currentContext.write()
    if (currentContext.isMapKey) {
      throw new TException("Can't have a sequence (list or set) as a key in a map!")
    }
    pushContext(new ParsingContext.Sequence)
    wrapExceptions {
      writer.write("[\n")
    }
  }

  private def writeSequenceEnd(): Unit = {
    popContext()
    wrapExceptions {
      writer.write("  " * contextStack.map(_.indent).sum)
      writer.write("]")
    }
  }

  /**
   * Helper to write out the beginning of a Thrift type (either struct or map),
   * both of which are written as objects.
   */
  private def writeObjectBegin(context: ParsingContext): Unit = {
    currentContext.write()
    if (currentContext.isMapKey) {
      throw new TException("Can't have a sequence (list or set) as a key in a map!")
    }
    pushContext(context)
    wrapExceptions {
      writer.write("{\n")
    }
  }

  /**
   * Helper to write out the end of a Thrift type (either struct or map), both of which are
   * written as objects.
   */
  private def writeObjectEnd(): Unit = wrapExceptions {
    popContext()
    writer.write("  " * contextStack.map(_.indent).sum)
    writer.write("}")
    // Flush at the end of the final struct.
    if (contextStack.size == 1) {
      writer.write("\n")
      writer.flush()
    }
  }

  /**
   * Return the current parsing context
   */
  private def currentContext = contextStack.last

  /**
   * Add a new parsing context onto the parse context stack
   */
  private def pushContext(c: ParsingContext): Unit = contextStack += c

  /**
   * Pop a parsing context from the parse context stack
   */
  private def popContext(): Unit = contextStack.remove(contextStack.length - 1)

  private def wrapExceptions[T](fn: => T): T = {
    try {
      fn
    } catch {
      case NonFatal(e) => throw new TException(e)
    }
  }
}

object TTextProtocol {
  private val escaper = Escapers.builder().addEscape('"', "\\\"").addEscape('\\', "\\\\").build

  final class Factory extends TProtocolFactory {
    override def getProtocol(transport: TTransport): TProtocol = {
      new TTextProtocol(transport, Charsets.UTF_8)
    }
  }

}
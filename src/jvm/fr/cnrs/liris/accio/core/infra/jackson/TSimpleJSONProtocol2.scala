/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package fr.cnrs.liris.accio.core.infra.jackson

import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer

import org.apache.thrift.TException
import org.apache.thrift.protocol._
import org.apache.thrift.transport.TTransport

import scala.collection.mutable

object TSimpleJSONProtocol2 {

  /**
   * Factory
   */
  class Factory extends TProtocolFactory {
    def getProtocol(trans: TTransport): TProtocol = new TSimpleJSONProtocol2(trans)
  }

  val COMMA = Array[Byte](',')
  val COLON = Array[Byte](':')
  val LBRACE = Array[Byte]('{')
  val RBRACE = Array[Byte]('}')
  val LBRACKET = Array[Byte]('[')
  val RBRACKET = Array[Byte](']')
  val QUOTE = '"'

  def ANONYMOUS_STRUCT = new TStruct

  val ANONYMOUS_FIELD = new TField
  val EMPTY_MESSAGE = new TMessage
  val EMPTY_SET = new TSet
  val EMPTY_LIST = new TList
  val EMPTY_MAP = new TMap
  val LIST = "list"
  val SET = "set"
  val MAP = "map"


}

/**
 * JSON protocol implementation for thrift.
 *
 * This protocol is write-only and produces a simple output format
 * suitable for parsing by scripting languages.  It should not be
 * confused with the full-featured TJSONProtocol.
 *
 */
final class TSimpleJSONProtocol2(trans: TTransport) extends TProtocol(trans) {

  protected class Context {
    @throws[TException]
    def write(): Unit = {}

    /**
     * Returns whether the current value is a key in a map
     */
    def isMapKey: Boolean = false
  }

  protected class ListContext extends Context {
    private[this] var first = true

    override def write(): Unit = {
      if (first) {
        first = false
      } else {
        trans_.write(TSimpleJSONProtocol2.COMMA)
      }
    }
  }

  protected class StructContext extends Context {
    private[this] var first = true
    private[this] var colon = true

    override def write(): Unit = {
      if (first) {
        first = false
        colon = true
      } else {
        trans_.write(if (colon) TSimpleJSONProtocol2.COLON else TSimpleJSONProtocol2.COMMA)
        colon = !colon
      }
    }
  }

  protected class MapContext extends StructContext {
    protected var isKey = true

    @Override
    override def write(): Unit = {
      super.write()
      isKey = !isKey
    }

    override def isMapKey: Boolean = {
      // we want to coerce map keys to json strings regardless
      // of their type
      isKey
    }
  }

  /**
   * Stack of nested contexts that we may be in.
   */
  protected val writeContextStack = new mutable.Stack[Context]

  /**
   * Current context that we are in
   */
  protected var writeContext = new Context

  /**
   * Push a new write context onto the stack.
   */
  protected def pushWriteContext(c: Context): Unit = {
    writeContextStack.push(writeContext)
    writeContext = c
  }

  /**
   * Pop the last write context off the stack
   */
  protected def popWriteContext(): Unit = {
    writeContext = writeContextStack.pop()
  }

  /**
   * Reset the write context stack to its initial state.
   */
  protected def resetWriteContext(): Unit = {
    while (writeContextStack.nonEmpty) {
      popWriteContext()
    }
  }

  /**
   * Used to make sure that we are not encountering a map whose keys are containers
   */
  @throws[CollectionMapKeyException]
  protected def assertContextIsNotMapKey(invalidKeyType: String): Unit = {
    if (writeContext.isMapKey) {
      throw new CollectionMapKeyException(s"Cannot serialize a map with keys that are of type $invalidKeyType")
    }
  }

  @throws[TException]
  override def writeMessageBegin(message: TMessage): Unit = {
    resetWriteContext() // THRIFT-3743
    trans_.write(TSimpleJSONProtocol2.LBRACKET)
    pushWriteContext(new ListContext)
    writeString(message.name)
    writeByte(message.`type`)
    writeI32(message.seqid)
  }

  @throws[TException]
  override def writeMessageEnd(): Unit = {
    popWriteContext()
    trans_.write(TSimpleJSONProtocol2.RBRACKET)
  }

  @throws[TException]
  override def writeStructBegin(struct: TStruct): Unit = {
    writeContext.write()
    trans_.write(TSimpleJSONProtocol2.LBRACE)
    pushWriteContext(new StructContext)
  }

  @throws[TException]
  override def writeStructEnd(): Unit = {
    popWriteContext()
    trans_.write(TSimpleJSONProtocol2.RBRACE)
  }

  @throws[TException]
  override def writeFieldBegin(field: TField): Unit = {
    // Note that extra type information is omitted in JSON!
    writeString(field.name)
  }

  override def writeFieldEnd(): Unit = {}

  override def writeFieldStop(): Unit = {}

  @throws[TException]
  def writeMapBegin(map: TMap): Unit = {
    assertContextIsNotMapKey(TSimpleJSONProtocol2.MAP)
    writeContext.write()
    trans_.write(TSimpleJSONProtocol2.LBRACE)
    pushWriteContext(new MapContext)
    // No metadata!
  }

  @throws[TException]
  def writeMapEnd(): Unit = {
    popWriteContext()
    trans_.write(TSimpleJSONProtocol2.RBRACE)
  }

  @throws[TException]
  def writeListBegin(list: TList): Unit = {
    assertContextIsNotMapKey(TSimpleJSONProtocol2.LIST)
    writeContext.write()
    trans_.write(TSimpleJSONProtocol2.LBRACKET)
    pushWriteContext(new ListContext)
    // No metadata!
  }

  @throws[TException]
  def writeListEnd(): Unit = {
    popWriteContext()
    trans_.write(TSimpleJSONProtocol2.RBRACKET)
  }

  @throws[TException]
  def writeSetBegin(set: TSet): Unit = {
    assertContextIsNotMapKey(TSimpleJSONProtocol2.SET)
    writeContext.write()
    trans_.write(TSimpleJSONProtocol2.LBRACKET)
    pushWriteContext(new ListContext)
    // No metadata!
  }

  @throws[TException]
  def writeSetEnd(): Unit = {
    popWriteContext()
    trans_.write(TSimpleJSONProtocol2.RBRACKET)
  }

  @throws[TException]
  def writeBool(b: Boolean): Unit = {
    writeString(b.toString)
  }

  @throws[TException]
  def writeByte(b: Byte): Unit = {
    writeI32(b)
  }

  @throws[TException]
  def writeI16(i16: Short): Unit = {
    writeI32(i16)
  }

  @throws[TException]
  def writeI32(i32: Int): Unit = {
    if (writeContext.isMapKey) {
      writeString(i32.toString)
    } else {
      writeContext.write()
      _writeStringData(i32.toString)
    }
  }

  @throws[TException]
  def _writeStringData(s: String): Unit = {
    try {
      trans_.write(s.getBytes("UTF-8"))
    } catch {
      case _: UnsupportedEncodingException => throw new TException("JVM DOES NOT SUPPORT UTF-8")
    }
  }

  @throws[TException]
  def writeI64(i64: Long): Unit = {
    if (writeContext.isMapKey) {
      writeString(i64.toString)
    } else {
      writeContext.write()
      _writeStringData(i64.toString)
    }
  }

  @throws[TException]
  def writeDouble(dub: Double): Unit = {
    if (writeContext.isMapKey) {
      writeString(dub.toString)
    } else {
      writeContext.write()
      _writeStringData(dub.toString)
    }
  }

  @throws[TException]
  def writeString(str: String): Unit = {
    writeContext.write()
    val length = str.length
    val escape = new StringBuffer(length + 16)
    escape.append(TSimpleJSONProtocol2.QUOTE)
    for (i <- 0 until length) {
      val c = str.charAt(i)
      c match {
        case '"' =>
          escape.append('\\')
          escape.append(c)
        case '\\' =>
          escape.append('\\')
          escape.append(c)
        case '\b' =>
          escape.append('\\')
          escape.append('b')
        case '\f' =>
          escape.append('\\')
          escape.append('f')
        case '\n' =>
          escape.append('\\')
          escape.append('n')
        case '\r' =>
          escape.append('\\')
          escape.append('r')
        case '\t' =>
          escape.append('\\')
          escape.append('t')
        case _ =>
          // Control characters! According to JSON RFC u0020 (space)
          if (c < ' ') {
            val hex = Integer.toHexString(c)
            escape.append('\\')
            escape.append('u')
            var j = 4
            while (j > hex.length) {
              escape.append('0')
              j -= 1
            }
            escape.append(hex)
          } else {
            escape.append(c)
          }
      }
    }
    escape.append(TSimpleJSONProtocol2.QUOTE)
    _writeStringData(escape.toString)
  }

  @throws[TException]
  def writeBinary(bin: ByteBuffer): Unit = {
    try {
      // TODO(mcslee): Fix this
      writeString(new String(bin.array(), bin.position() + bin.arrayOffset(), bin.limit() - bin.position() - bin.arrayOffset(), "UTF-8"))
    } catch {
      case _: UnsupportedEncodingException => throw new TException("JVM DOES NOT SUPPORT UTF-8")
    }
  }

  /**
   * Reading methods.
   */

  def readMessageBegin(): TMessage = TSimpleJSONProtocol2.EMPTY_MESSAGE

  @throws[TException]
  def readMessageEnd() {}

  def readStructBegin(): TStruct = TSimpleJSONProtocol2.ANONYMOUS_STRUCT

  @throws[TException]
  def readStructEnd() {}

  def readFieldBegin(): TField = TSimpleJSONProtocol2.ANONYMOUS_FIELD

  @throws[TException]
  def readFieldEnd() {}

  def readMapBegin(): TMap = TSimpleJSONProtocol2.EMPTY_MAP

  @throws[TException]
  def readMapEnd() {}

  def readListBegin(): TList = TSimpleJSONProtocol2.EMPTY_LIST

  @throws[TException]
  def readListEnd() {}

  def readSetBegin(): TSet = TSimpleJSONProtocol2.EMPTY_SET

  @throws[TException]
  def readSetEnd() {}

  def readBool(): Boolean = readByte() == 1

  def readByte(): Byte = 0

  def readI16(): Short = 0

  def readI32(): Int = 0

  def readI64(): Long = 0

  def readDouble(): Double = 0

  def readString(): String = ""

  def readStringBody(size: Int): String = ""

  def readBinary(): ByteBuffer = ByteBuffer.wrap(Array.empty[Byte])

  class CollectionMapKeyException(message: String) extends TException(message)

}
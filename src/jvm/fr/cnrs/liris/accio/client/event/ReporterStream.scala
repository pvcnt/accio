package fr.cnrs.liris.accio.client.event

import java.io.OutputStream
import java.util

/**
 * An OutputStream that delegates all writes to an EventHandler.
 */
final class ReporterStream(handler: EventHandler, eventKind: EventKind) extends OutputStream {
  override def close(): Unit = {
    // NOP.
  }

  override def flush(): Unit = {
    // NOP.
  }

  override def write(b: Int): Unit = handler.handle(Event(eventKind, Array(b.toByte)))

  override def write(bytes: Array[Byte]): Unit = write(bytes, 0, bytes.length)

  override def write(bytes: Array[Byte], offset: Int, len: Int): Unit = {
    handler.handle(Event(eventKind, util.Arrays.copyOfRange(bytes, offset, offset + len)))
  }
}
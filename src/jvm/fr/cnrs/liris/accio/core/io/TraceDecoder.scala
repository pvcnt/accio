package fr.cnrs.liris.accio.core.io

import java.nio.charset.Charset

import com.google.common.base.Charsets
import fr.cnrs.liris.accio.core.model.{Record, Trace}
import fr.cnrs.liris.common.io.source.{Decoder, EncodedRecord}

class TraceDecoder(decoder: Decoder[Record], charset: Charset = Charsets.UTF_8) extends Decoder[Trace] {
  override def decode(record: EncodedRecord): Option[Trace] = {
    val content = new String(record.bytes, charset)
    val records = content.split("\n").flatMap { line =>
      decoder.decode(record.copy(bytes = line.getBytes(charset)))
    }
    if (records.nonEmpty) Some(Trace(records)) else None
  }
}

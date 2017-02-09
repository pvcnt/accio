/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.privamov.core.io

import com.google.inject.Inject
import fr.cnrs.liris.dal.core.io.{Codec, TextLineDecoder}
import fr.cnrs.liris.common.util.ByteUtils
import fr.cnrs.liris.privamov.core.model._

import scala.reflect._


/**
 * Codec for our CSV format handling traces.
 *
 * @param codec Codec handling events in our CSV format.
 */
class CsvTraceCodec @Inject()(codec: CsvEventCodec) extends Codec[Trace] {
  private[this] val decoder = new TextLineDecoder(codec)

  override def encode(obj: Trace): Array[Byte] = {
    val encodedEvents = obj.events.map(codec.encode)
    ByteUtils.foldLines(encodedEvents)
  }

  override def decode(key: String, bytes: Array[Byte]): Option[Trace] = {
    val events = decoder.decode(key, bytes).getOrElse(Seq.empty).sortBy(_.time.getMillis)
    Some(Trace(key, events))
  }

  override def elementClassTag: ClassTag[Trace] = classTag[Trace]
}
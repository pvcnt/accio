package fr.cnrs.liris.privamov.ops

import fr.cnrs.liris.privamov.core.model.{Event, Trace}

import scala.collection.mutable

/**
 * Base trait for transformers using previous event and current one to take a decision about
 * whether to split a trace into multiple parts.
 */
private[ops] trait SlidingSplitting {
  protected def transform(input: Trace, split: (Seq[Event], Event) => Boolean): Seq[Trace] = {
    val output = mutable.ListBuffer.empty[Trace]
    val buffer = mutable.ListBuffer.empty[Event]
    var idx = 0
    for (event <- input.events) {
      if (buffer.nonEmpty && split(buffer, event)) {
        output += Trace(s"${input.id}-$idx", buffer.toList)
        buffer.clear()
        idx += 1
      }
      buffer += event
    }
    if (buffer.nonEmpty) {
      output += Trace(s"${input.id}-$idx", buffer.toList)
    }
    output
  }
}
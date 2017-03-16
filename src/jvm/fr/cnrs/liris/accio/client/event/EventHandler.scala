package fr.cnrs.liris.accio.client.event

import java.util.concurrent.atomic.AtomicInteger

import scala.collection.mutable

trait EventHandler {
  def handle(event: Event): Unit
}

final class CollectorEventHandler(handler: EventHandler) extends EventHandler {
  private[this] val _events = mutable.ListBuffer.empty[Event]

  override def handle(event: Event): Unit = synchronized {
    _events ++ Seq(event)
    handler.handle(event)
  }

  def events: Iterable[Event] = _events.toList
}

final class FilterEventHandler(mask: Set[EventKind], handler: EventHandler) extends EventHandler {
  override def handle(event: Event): Unit = {
    if (mask.contains(event.kind)) {
      handler.handle(event)
    }
  }
}

final class StoreEventHandler extends EventHandler {
  private[this] val _events = mutable.ListBuffer.empty[Event]

  override def handle(event: Event): Unit = synchronized {
    _events ++ Seq(event)
  }

  def replay(handler: EventHandler): Unit = synchronized {
    _events.foreach(handler.handle)
    _events.clear()
  }

  def clear(): Unit = synchronized {
    _events.clear()
  }
}

final class SensorEventHandler(mask: Set[EventKind]) extends EventHandler {
  private[this] val _count = new AtomicInteger(0)

  override def handle(event: Event): Unit = {
    if (mask.contains(event.kind)) {
      _count.incrementAndGet()
    }
  }

  def count: Int = _count.get
}
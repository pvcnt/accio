package fr.cnrs.liris.accio.client.event

/**
 * The ExceptionListener is the primary means of reporting exceptions.
 */
trait ExceptionListener {
  def error(message: String, e: Throwable): Unit
}
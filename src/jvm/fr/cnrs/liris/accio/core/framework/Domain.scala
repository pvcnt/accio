package fr.cnrs.liris.accio.core.framework

trait Domain[T] {
  def size: Long

  def random(): T

  def neighbor(value: T): T
}

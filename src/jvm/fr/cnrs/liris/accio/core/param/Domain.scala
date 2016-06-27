package fr.cnrs.liris.accio.core.param

trait Domain[T] {
  def size: Long

  def random(): T

  def neighbor(value: T): T
}

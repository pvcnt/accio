package fr.cnrs.liris.sparkle

final class DataFrameWriter[T](partitioner: Option[T => Any]) {
  def partitionBy(fn: T => Any): DataFrameWriter[T] = new DataFrameWriter(partitioner = Some(fn))

  //def sortBy[U](fn: T => U)(implicit cmp: Ordering[U]): DataFrameWriter[T] = new DataFrameWriter(partitioner = Some(fn))
}
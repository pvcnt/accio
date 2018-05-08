package fr.cnrs.liris.sparkle

import fr.cnrs.liris.sparkle.filesystem.PosixFilesystem
import fr.cnrs.liris.sparkle.io.RowEncoder
import fr.cnrs.liris.sparkle.io.csv.CsvDataFormat

import scala.collection.parallel.{ForkJoinTaskSupport, ParSeq}
import scala.concurrent.forkjoin.ForkJoinPool
import scala.reflect.runtime.universe.TypeTag

final class DatasetSupport(parallelism: Int) {
  private[sparkle] val filesystem = PosixFilesystem
  private[sparkle] val format = CsvDataFormat

  def read[T: TypeTag](uri: String): ParSeq[T] = {
    val encoder = RowEncoder[T]
    val it = filesystem.list(uri).flatMap { uri =>
      format
        .readerFor(encoder.structType)
        .read(filesystem.createInputStream(uri))
        .map(encoder.deserialize)
    }
    val parSeq = it.toSeq.view.par
    parSeq.tasksupport = new ForkJoinTaskSupport(new ForkJoinPool(parallelism))
    parSeq
  }

  def write[T: TypeTag](parSeq: ParSeq[T], uri: String): Unit = {
    val encoder = RowEncoder[T]

  }
}

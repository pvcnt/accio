package fr.cnrs.liris.common.io.source

trait RecordWriter {
  def write(records: Iterable[EncodedRecord], url: String): Unit
}

trait Indexer[T] {
  def index(record: T): IndexedFile
}
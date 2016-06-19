package fr.cnrs.liris.common.io.source

/**
 * Unit tests for [[VariableLengthRecordReader]].
 */
class VariableLengthRecordReaderSpec extends BaseRecordReaderSpec {
  protected def getFiles: Seq[(String, Set[String], Array[Byte])] = Seq(
    ("foo.txt", Set.empty[String], Array.concat(toBytes(LongSize), toBytes(1L), toBytes(IntSize), toBytes(2), toBytes(LongSize), toBytes(3L))),
    ("bar.txt", Set("bar"), Array.concat(toBytes(LongSize), toBytes(4L), toBytes(LongSize), toBytes(5L))),
    ("foobar.txt", Set.empty[String], Array.concat(toBytes(LongSize), toBytes(1L), toBytes(LongSize), toBytes(2L), toBytes(IntSize), toBytes(3), toBytes(IntSize), toBytes(4)))
  )

  "VariableLengthRecordReader" should "return all records in order" in {
    val reader = new VariableLengthRecordReader
    assertRecords(reader.read(files(0)), Seq(toBytes(1L), toBytes(2), toBytes(3L)), "foo.txt")
    assertRecords(reader.read(files(1)), Seq(toBytes(4L), toBytes(5L)), "bar.txt", Set("bar"))
    assertRecords(reader.read(files(2)), Seq(toBytes(1L), toBytes(2L), toBytes(3), toBytes(4)), "foobar.txt")
  }

  it should "reject non-existant files" in {
    val reader = new VariableLengthRecordReader
    an[IllegalArgumentException] shouldBe thrownBy {
      reader.read(IndexedFile("/foo.txt"))
    }
  }
}
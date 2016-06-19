package fr.cnrs.liris.common.io.source

/**
 * Unit tests for [[FixedLengthRecordReader]].
 */
class FixedLengthRecordReaderSpec extends BaseRecordReaderSpec {
  protected def getFiles: Seq[(String, Set[String], Array[Byte])] = Seq(
    ("foo.txt", Set.empty[String], Array.concat(toBytes(1), toBytes(2), toBytes(3))),
    ("bar.txt", Set("bar"), Array.concat(toBytes(4), toBytes(5))),
    ("foobar.txt", Set.empty[String], Array.concat(toBytes(1), toBytes(2), toBytes(3), toBytes(4)))
  )

  "FixedLengthRecordReader" should "return all records in order" in {
    val reader = new FixedLengthRecordReader(4)
    assertRecords(reader.read(files(0)), Seq(toBytes(1), toBytes(2), toBytes(3)), "foo.txt")
    assertRecords(reader.read(files(1)), Seq(toBytes(4), toBytes(5)), "bar.txt", Set("bar"))
    assertRecords(reader.read(files(2)), Seq(toBytes(1), toBytes(2), toBytes(3), toBytes(4)), "foobar.txt")
  }

  it should "reject a negative header length" in {
    an[IllegalArgumentException] shouldBe thrownBy {
      new FixedLengthRecordReader(4, headerLength = -1)
    }
  }

  it should "reject a zero record length" in {
    an[IllegalArgumentException] shouldBe thrownBy {
      new FixedLengthRecordReader(0)
    }
  }

  it should "ignore header" in {
    val reader = new FixedLengthRecordReader(4, headerLength = 8)
    assertRecords(reader.read(files(0)), Seq(toBytes(3)), "foo.txt")
    assertRecords(reader.read(files(1)), Seq(), "bar.txt")
    assertRecords(reader.read(files(2)), Seq(toBytes(3), toBytes(4)), "foobar.txt")
  }

  it should "reject non-existant files" in {
    val reader = new FixedLengthRecordReader(4)
    an[IllegalArgumentException] shouldBe thrownBy {
      reader.read(IndexedFile("/foo.txt"))
    }
  }
}

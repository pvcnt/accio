package fr.cnrs.liris.common.io.source

/**
 * Unit tests for [[WholeFileReader]].
 */
class WholeFileReaderSpec extends BaseTextFileReaderSpec {
  "WholeFileReader" should "return all records in order" in {
    val reader = new WholeFileReader
    assertRecords(reader.read(files(0)), Seq("first foo\nsecond foo\nthird foo".getBytes), "foo.txt")
    assertRecords(reader.read(files(1)), Seq("first bar\nsecond bar\n".getBytes), "bar.txt", Set("bar"))
    assertRecords(reader.read(files(2)), Seq("first bar\nsecond bar\nthird bar\n ".getBytes), "foobar.txt")
  }

  it should "reject non-existant files" in {
    val reader = new WholeFileReader
    an[IllegalArgumentException] shouldBe thrownBy {
      reader.read(IndexedFile("/foo.txt"))
    }
  }
}
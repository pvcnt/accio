package fr.cnrs.liris.common.io.source

/**
 * Unit tests for [[TextLineReader]].
 */
class TextLineReaderSpec extends BaseTextFileReaderSpec {
  "TextLineReader" should "return all records in order" in {
    val reader = new TextLineReader
    assertRecords(reader.read(files(0)), Seq("first foo".getBytes, "second foo".getBytes, "third foo".getBytes), "foo.txt")
    //Empty lines are ignored...
    assertRecords(reader.read(files(1)), Seq("first bar".getBytes, "second bar".getBytes), "bar.txt", Set("bar"))
    //... but blank lines are kept
    assertRecords(reader.read(files(2)), Seq("first bar".getBytes, "second bar".getBytes, "third bar".getBytes, " ".getBytes), "foobar.txt")
  }

  it should "reject a negative number of header lines" in {
    an[IllegalArgumentException] shouldBe thrownBy {
      new TextLineReader(-1)
    }
  }

  it should "ignore header lines" in {
    val reader = new TextLineReader(headerLines = 2)
    assertRecords(reader.read(files(0)), Seq("third foo".getBytes), "foo.txt")
    assertRecords(reader.read(files(1)), Seq(), "bar.txt")
    assertRecords(reader.read(files(2)), Seq("third bar".getBytes, " ".getBytes), "foobar.txt")
  }

  it should "reject non-existant files" in {
    val reader = new TextLineReader
    an[IllegalArgumentException] shouldBe thrownBy {
      reader.read(IndexedFile("/foo.txt"))
    }
  }
}
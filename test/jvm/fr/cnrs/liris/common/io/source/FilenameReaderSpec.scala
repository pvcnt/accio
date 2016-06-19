package fr.cnrs.liris.common.io.source

/**
 * Unit tests for [[FilenameReader]].
 */
class FilenameReaderSpec extends BaseTextFileReaderSpec {
  "FilenameReader" should "return all records in order" in {
    val reader = new FilenameReader
    assertRecords(reader.read(files(0)), Seq(rootPath.resolve("foo.txt").toString.getBytes), "foo.txt")
    assertRecords(reader.read(files(1)), Seq(rootPath.resolve("bar.txt").toString.getBytes), "bar.txt", Set("bar"))
    assertRecords(reader.read(files(2)), Seq(rootPath.resolve("foobar.txt").toString.getBytes), "foobar.txt")
  }

  it should "reject non-existant files" in {
    val reader = new FilenameReader
    an[IllegalArgumentException] shouldBe thrownBy {
      reader.read(IndexedFile("/foo.txt"))
    }
  }
}
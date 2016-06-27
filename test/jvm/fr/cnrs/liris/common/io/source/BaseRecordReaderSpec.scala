package fr.cnrs.liris.common.io.source

import java.io.{ByteArrayOutputStream, DataOutputStream}
import java.nio.file.{Files, Path}
import java.util

import fr.cnrs.liris.common.util.FileUtils
import fr.cnrs.liris.testing.UnitSpec
import org.scalatest.BeforeAndAfter

trait BaseRecordReaderSpec extends UnitSpec with BeforeAndAfter {
  protected val IntSize = 4
  protected val LongSize = 8
  protected var rootPath: Path = null
  protected var files: Seq[IndexedFile] = null

  before {
    rootPath = Files.createTempDirectory(getClass.getSimpleName)
    files = getFiles.map { case (filename, labels, contents) =>
      Files.write(rootPath.resolve(filename), contents)
      IndexedFile(rootPath.resolve(filename).toString, labels = labels)
    }
  }

  after {
    FileUtils.safeDelete(rootPath)
    rootPath = null
  }

  protected def getFiles: Seq[(String, Set[String], Array[Byte])]

  protected def assertRecords(records: Iterable[EncodedRecord], expected: Seq[Array[Byte]], filename: String, labels: Set[String] = Set.empty): Unit = {
    val seq = records.toSeq
    seq should have size expected.size
    seq.zipWithIndex.foreach { case (record, idx) =>
      util.Arrays.equals(record.bytes, expected(idx)) shouldBe true
      record.labels shouldBe labels
    }
  }

  protected def toBytes(i: Int): Array[Byte] = {
    val baos = new ByteArrayOutputStream
    val dos = new DataOutputStream(baos)
    dos.writeInt(i)
    baos.toByteArray
  }

  protected def toBytes(l: Long): Array[Byte] = {
    val baos = new ByteArrayOutputStream
    val dos = new DataOutputStream(baos)
    dos.writeLong(l)
    baos.toByteArray
  }
}

trait BaseTextFileReaderSpec extends BaseRecordReaderSpec {
  override protected def getFiles: Seq[(String, Set[String], Array[Byte])] = Seq(
    ("foo.txt", Set.empty[String], "first foo\nsecond foo\nthird foo".getBytes),
    ("bar.txt", Set("bar"), "first bar\nsecond bar\n".getBytes),
    ("foobar.txt", Set.empty[String], "first bar\nsecond bar\nthird bar\n ".getBytes)
  )
}
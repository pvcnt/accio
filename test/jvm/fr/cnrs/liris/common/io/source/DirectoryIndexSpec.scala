package fr.cnrs.liris.common.io.source

import java.nio.file.{Files, Path}

import fr.cnrs.liris.common.util.FileUtils
import fr.cnrs.liris.testing.UnitSpec
import org.scalatest.BeforeAndAfter

/**
 * Unit tests for [[DirectoryIndex]].
 */
class DirectoryIndexSpec extends UnitSpec with BeforeAndAfter {
  private var rootPath: Path = null
  private val files = Seq("foo.csv", "bar.csv", "foo/bar.csv", "foo/foo.csv", "bar/foo.csv")

  before {
    rootPath = Files.createTempDirectory(getClass.getSimpleName)
    Files.createDirectory(rootPath.resolve("foo"))
    Files.createDirectory(rootPath.resolve("bar"))
    files.foreach(fname => Files.write(rootPath.resolve(fname), "dummy".getBytes))
  }

  after {
    FileUtils.safeDelete(rootPath)
    rootPath = null
  }

  "DirectoryIndex" should "reject an invalid url" in {
    an[IllegalArgumentException] shouldBe thrownBy {
      DirectoryIndex("/foo")
    }
  }

  it should "read correct files" in {
    val index = DirectoryIndex(url(rootPath))
    val expected = files.map(fname => file(fname)).toSet
    index.read(None) should contain theSameElementsAs expected
    index.read(Some("boo")) should contain theSameElementsAs Set.empty
  }

  it should "support label extraction" in {
    val index = new DirectoryIndex(url(rootPath), labelize = path => {
      Option(path.getParent).map(_.toString).toSet
    })
    var expected = Set(files(2), files(3)).map(fname => file(fname, Set("foo")))
    index.read(Some("foo")) should contain theSameElementsAs expected
    expected = Set(files(4)).map(fname => file(fname, Set("bar")))
    index.read(Some("bar")) should contain theSameElementsAs expected
  }

  private def file(fname: String, labels: Set[String] = Set.empty) =
    IndexedFile(url(rootPath.resolve(fname)), labels = labels)

  private def url(path: Path) = path.toAbsolutePath.toString
}
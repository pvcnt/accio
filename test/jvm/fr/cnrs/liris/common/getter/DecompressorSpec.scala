package fr.cnrs.liris.common.getter

import java.io.{BufferedOutputStream, FileOutputStream}
import java.nio.file.{Files, Path}

import com.google.common.io.Resources
import fr.cnrs.liris.common.util.FileUtils
import fr.cnrs.liris.testing.UnitSpec
import org.apache.commons.compress.utils.IOUtils
import org.scalatest.BeforeAndAfterEach

abstract class DecompressorSpec extends UnitSpec with BeforeAndAfterEach {
  protected[this] var tmpDir: Path = null

  protected def decompressor: Decompressor

  override protected def beforeEach(): Unit = {
    tmpDir = Files.createTempDirectory("getter-test-")
  }

  override protected def afterEach(): Unit = {
    FileUtils.safeDelete(tmpDir)
    tmpDir = null
  }

  protected def copyResource(resourceName: String): Path = {
    val dst = tmpDir.resolve(resourceName.split("/").last)
    val in = Resources.asByteSource(Resources.getResource(resourceName)).openBufferedStream()
    val out = new BufferedOutputStream(new FileOutputStream(dst.toFile))
    try {
      IOUtils.copy(in, out)
      dst
    } finally {
      try {
        in.close()
      } finally {
        out.close()
      }
    }
  }
}

abstract class SingleDecompressorSpec extends DecompressorSpec {
  protected def assertSingleFile(resourceName: String): Unit = {
    val src = copyResource(resourceName)
    val dst = tmpDir.resolve("result")
    decompressor.decompress(src, dst)

    dst.toFile.isFile shouldBe true
    new String(Files.readAllBytes(dst)) shouldBe "foobar\n"
  }
}

abstract class ArchiveDecompressorSpec extends DecompressorSpec {
  protected def assertSingleFile(resourceName: String): Unit = {
    val src = copyResource(resourceName)
    val dst = tmpDir.resolve("result")
    decompressor.decompress(src, dst)

    dst.toFile.isDirectory shouldBe true
    dst.resolve("foobar.txt").toFile.isFile shouldBe true
    new String(Files.readAllBytes(dst.resolve("foobar.txt"))) shouldBe "foobar\n"
  }

  protected def assertTree(resourceName: String): Unit = {
    val src = copyResource(resourceName)
    val dst = tmpDir.resolve("result")
    decompressor.decompress(src, dst)

    dst.toFile.isDirectory shouldBe true
    dst.resolve("foobar.txt").toFile.isFile shouldBe true
    new String(Files.readAllBytes(dst.resolve("foobar.txt"))) shouldBe "foobar\n"
    dst.resolve("foo/foo.txt").toFile.isFile shouldBe true
    new String(Files.readAllBytes(dst.resolve("foo/foo.txt"))) shouldBe "foo\n"
    dst.resolve("foo/bar.txt").toFile.isFile shouldBe true
    new String(Files.readAllBytes(dst.resolve("foo/bar.txt"))) shouldBe "bar\n"
    dst.resolve("bar/foo.txt").toFile.isFile shouldBe true
    new String(Files.readAllBytes(dst.resolve("bar/foo.txt"))) shouldBe "foo\n"
    dst.resolve("bar/bar.txt").toFile.isFile shouldBe true
    new String(Files.readAllBytes(dst.resolve("bar/bar.txt"))) shouldBe "bar\n"
  }
}

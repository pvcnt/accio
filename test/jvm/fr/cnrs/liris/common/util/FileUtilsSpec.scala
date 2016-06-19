package fr.cnrs.liris.common.util

import java.nio.file.Files

import fr.cnrs.liris.testing.UnitSpec

class FileUtilsSpec extends UnitSpec {
  "FileUtils::safeDelete" should "delete a directory" in {
    val dir = Files.createTempDirectory("FileUtilsSpec-")
    dir.toFile.deleteOnExit()
    dir.toFile.exists() shouldBe true

    FileUtils.safeDelete(dir)
    dir.toFile.exists() shouldBe false
  }

  it should "delete a file" in {
    val file  = Files.createTempFile("FileUtilsSpec-", ".txt")
    file.toFile.deleteOnExit()
    file.toFile.exists() shouldBe true

    FileUtils.safeDelete(file)
    file.toFile.exists() shouldBe false
  }

  it should "recursively delete a directory" in {
    val dir = Files.createTempDirectory("FileUtilsSpec-")
    Files.createDirectory(dir.resolve("foo"))
    Files.createDirectory(dir.resolve("bar"))
    Files.createFile(dir.resolve("foo/foo.txt"))
    Files.createFile(dir.resolve("foo/foo2.txt"))
    dir.toFile.exists() shouldBe true
    dir.toFile.deleteOnExit()

    FileUtils.safeDelete(dir)
    dir.toFile.exists() shouldBe false
  }

  "FileUtils::replaceHome" should "replace an initial '~' with the home directory" in {
    FileUtils.replaceHome("~/foo/bar") shouldBe sys.props("user.home") + "/foo/bar"
    FileUtils.replaceHome("abc/~foo/bar") shouldBe "abc/~foo/bar"
  }
}
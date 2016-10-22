package fr.cnrs.liris.common.util

import fr.cnrs.liris.testing.UnitSpec

class SeqsSpec extends UnitSpec {
  "Seqs" should "compute a cross-product" in {
    val data = Seq(Seq(1, 2), Seq(3, 4, 5), Seq(6, 7, 8))
    Seqs.crossProduct(data) should contain theSameElementsAs Seq(
      Seq(1, 3, 6),
      Seq(1, 4, 6),
      Seq(1, 5, 6),
      Seq(1, 3, 7),
      Seq(1, 4, 7),
      Seq(1, 5, 7),
      Seq(1, 3, 8),
      Seq(1, 4, 8),
      Seq(1, 5, 8),
      Seq(2, 3, 6),
      Seq(2, 4, 6),
      Seq(2, 5, 6),
      Seq(2, 3, 7),
      Seq(2, 4, 7),
      Seq(2, 5, 7),
      Seq(2, 3, 8),
      Seq(2, 4, 8),
      Seq(2, 5, 8))
  }

  it should "compute the cross-product of an empty list" in {
    Seqs.crossProduct(Seq.empty) shouldBe Seq(Seq.empty)
  }
}
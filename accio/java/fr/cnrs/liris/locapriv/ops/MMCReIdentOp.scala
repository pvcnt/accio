/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016 Vincent Primault <v.primault@ucl.ac.uk>
 *
 * Accio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Accio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Accio.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.cnrs.liris.locapriv.ops

/*@Op(
  category = "metric",
  help = "Re-identification attack using mobility Markov chains.",
  cpus = 6,
  ram = "3G")
case class MmcReidentOp(
  @Arg(help = "Input train dataset")
  train: RemoteFile,
  @Arg(help = "Input test dataset")
  test: RemoteFile,
  @Arg(help = "Clustering parameter : minimum points in a cluster")
  minPts: Int = 10,
  @Arg(help = "Clustering parameter : maximum size cluster")
  diameter: Distance = new Distance(3000),
  @Arg(help = "Clustering parameter : maximum cluster duration")
  duration: Duration = new Duration(3600),
  @Arg(help = "Attack")
  attack: String = "gambs")
  extends ScalaOperator[MmcReidentOut] with SparkleOperator {
  override def execute(ctx: OpContext): MmcReidentOut = {
    val dstrain = read[Trace](train)
    val dstest = read[Trace](test)

    val mapPOisTrain = formPOIs(dstrain)
    val mapPOisTest = formPOIs(dstest)

    // form MMC transition matrices
    val mapMMCTrain = formMMCMap(dstrain, mapPOisTrain)
    val mapMMCTest = formMMCMap(dstest, mapPOisTest)
    val (rate, userMatches) = reIdentAttackGambs(dstest.keys.size, mapPOisTrain, mapMMCTrain, mapPOisTest, mapMMCTest)
    var matches = userMatches
    dstrain.keys.union(dstest.keys).toSet.foreach { u: String =>
      if (!matches.contains(u)) matches += u -> "-"
    }
    MmcReidentOut(matches, rate)
  }

  private def reIdentAttackGambs(nbUser: Int, mapPOisTrain: Map[String, Seq[Cluster]], mapMMCTrain: Map[String, (SparseMatrix[Double], Array[Double])], mapPOisTest: Map[String, Seq[Cluster]], mapMMCTest: Map[String, (SparseMatrix[Double], Array[Double])]): (Double, Map[String, String]) = {
    var matches = Map[String, String]()
    var nbMatches = 0
    mapPOisTest.par.foreach { case (k, states_k) =>
      var order = Map[String, Double]()
      mapPOisTrain.foreach { case (u, states_u) =>
        val dist = d(states_k, mapMMCTest(k), states_u, mapMMCTrain(u))
        order += (u -> dist)
      }
      val seq = order.toSeq.sortBy(_._2)
      synchronized(matches += (k -> seq.head._1))
      if (k == seq.head._1) nbMatches += 1
    }
    val rate = nbMatches.toDouble / nbUser.toDouble
    (rate, matches)
  }

  private def d(states_k: Seq[Cluster], mmc_k: (SparseMatrix[Double], Array[Double]), states_u: Seq[Cluster], mmc_u: (SparseMatrix[Double], Array[Double])): Double = {
    val stat = stationary_distance(states_k, mmc_k, states_u, mmc_u)
    val prox = proximity_distance(states_k, mmc_k, states_u, mmc_u)
    if (prox < 100000 && stat > 2000) prox else stat
  }

  private def stationary_distance(states_k: Seq[Cluster], mmc_k: (SparseMatrix[Double], Array[Double]), states_u: Seq[Cluster], mmc_u: (SparseMatrix[Double], Array[Double])): Double = {
    var dist = 0.0
    for (i <- states_k.indices) {
      val pi = states_k(i)
      var min_distance = new Distance(1E8)
      for (j <- states_u.indices) {
        val pj = states_u(j)
        val currentDistance = pi.centroid.distance(pj.centroid)
        if (currentDistance < min_distance) min_distance = currentDistance
      }
      dist = dist + min_distance.meters * mmc_k._2(i)
    }
    dist
  }

  private def sym_stationary_distance(states_k: Seq[Cluster], mmc_k: (SparseMatrix[Double], Array[Double]), states_u: Seq[Cluster], mmc_u: (SparseMatrix[Double], Array[Double])): Double = {
    val dku = stationary_distance(states_k, mmc_k, states_u, mmc_u)
    val duk = stationary_distance(states_u, mmc_u, states_k, mmc_k)
    //TODO: parenthesis missing?
    dku + duk / 2.0
  }

  private def proximity_distance(states_k: Seq[Cluster], mmc_k: (SparseMatrix[Double], Array[Double]), states_u: Seq[Cluster], mmc_u: (SparseMatrix[Double], Array[Double])): Double = {
    val delta = new Distance(100)
    var rank = 10
    val v1 = mmc_k._2
    val v2 = mmc_u._2
    val vo1 = v1.zipWithIndex.sortBy(_._1)
    val vo2 = v2.zipWithIndex.sortBy(_._1)
    var score = 0

    //for(i <- 0 to  math.min(states_k.size,states_u.size)){
    for (i <- states_k.indices.intersect(states_u.indices)) {
      val pk = states_k(vo1(i)._2)
      val pu = states_u(vo2(i)._2)
      if (pk.centroid.distance(pu.centroid) < delta) {
        score += rank
      }
      rank = rank / 2
      if (rank == 0) rank = 1

    }
    if (score > 0) 1.0 / score.toDouble else 100000
  }

  private def formPOIs(ds: DataFrame[Trace]): Map[String, Seq[Cluster]] = {
    var mmcMapPOisTrain = Map[String, Seq[Cluster]]()
    val clusterMachine = new PoisClusterer(duration, diameter, minPts)
    ds.foreach { t =>
      val poiSet = clusterMachine.clusterKeepCluster(t.events)
      if (poiSet.nonEmpty) synchronized(mmcMapPOisTrain += (t.id -> poiSet))
    }
    mmcMapPOisTrain
  }

  private def formMMCMap(ds: DataFrame[Trace], mapPOis: Map[String, Seq[Cluster]]): Map[String, (SparseMatrix[Double], Array[Double])] = {
    var mapTransMat = Map[String, (SparseMatrix[Double], Array[Double])]()
    ds.foreach { t =>
      val poiset = mapPOis.get(t.id)
      poiset match {
        case Some(pois) =>
          if (pois.nonEmpty) synchronized(mapTransMat += (t.id -> formTransitionMatrix(t.events, pois)))
        case None => // Do nothing.
      }
    }
    mapTransMat
  }

  private def formTransitionMatrix(events: Seq[Event], states: Seq[Cluster]): (SparseMatrix[Double], Array[Double]) = {
    val labels = Array.fill[Int](events.size)(-1)
    val mat = new SparseMatrix[Int](states.size, states.size)
    val count = Array.fill[Int](states.size)(0)
    var prev = -1
    var lastPoi = -1
    for (i <- events.indices) {
      val e = events(i)
      for (j <- states.indices) {
        val state = states(j)
        if (state.events.contains(e)) labels(i) = j
      }
      val dest = labels(i)
      if (dest != -1) count(dest) = count(dest) + 1
      if (lastPoi == -1) {
        lastPoi = dest
      } else {
        // transition  prev -> dest
        if (prev != dest && dest != -1) {
          mat.inc(lastPoi, dest)
        }
      }
      prev = dest
    }
    val s = count.sum.toDouble
    val vector = count.map(c => c.toDouble / s)
    (mat.proportional, vector)
  }
}

case class MmcReidentOut(
  @Arg(help = "Matches between users")
  matches: immutable.Map[String, String],
  @Arg(help = "Re-Ident rate")
  rate: Double)


*/
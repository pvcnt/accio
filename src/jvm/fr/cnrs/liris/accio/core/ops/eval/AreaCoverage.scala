/*
 * Copyright LIRIS-CNRS (2016)
 * Contributors: Vincent Primault <vincent.primault@liris.cnrs.fr>
 *
 * This software is a computer program whose purpose is to study location privacy.
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */

package fr.cnrs.liris.accio.core.ops.eval

import com.google.common.geometry.S2CellId
import fr.cnrs.liris.accio.core.framework.{Evaluator, Metric, Op}
import fr.cnrs.liris.accio.core.model.Trace
import fr.cnrs.liris.accio.core.param.Param

@Op(
  category = "metric",
  help = "Compute area coverage difference between two datasets of traces"
)
case class AreaCoverage(
    @Param(help = "S2 cells levels")
    level: Int
) extends Evaluator {

  override def evaluate(reference: Trace, result: Trace): Seq[Metric] = {
    val refCells = getCells(reference, level)
    val resCells = getCells(result, level)
    val matched = resCells.intersect(refCells).size
    MetricUtils.informationRetrieval(refCells.size, resCells.size, matched)
  }

  override def metrics: Seq[String] = MetricUtils.informationRetrievalMetrics

  private def getCells(trace: Trace, level: Int) =
    trace.records.map(rec => S2CellId.fromLatLng(rec.point.toLatLng.toS2).parent(level)).toSet
}
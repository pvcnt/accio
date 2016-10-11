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

package fr.cnrs.liris.accio.ops

import fr.cnrs.liris.accio.core.framework.{Mapper, Op}
import fr.cnrs.liris.accio.core.model.Trace
import fr.cnrs.liris.accio.core.param.Param

@Op(
  help = "Split traces sequentially, according to chronological order"
)
case class SequentialSplittingOp(
    @Param(help = "Percentage of events at which a trace begins") percentBegin: Double,
    @Param(help = "Percentage of events at which a trace ends") percentEnd: Double,
    @Param(help = "Whether to take the complement trace") complement: Boolean = false
) extends Mapper {
  require(percentBegin >= 0 && percentBegin <= 100, s"Begin percentage must be in [0,100] (got $percentBegin)")
  require(percentEnd >= 0 && percentEnd <= 100, s"End percentage must be in [0,100] (got $percentEnd)")
  require(percentBegin <= percentEnd, s"End percentage must be greater than begin percentage")

  override def map(trace: Trace): Trace = {
    val from = math.max(0, (percentBegin * trace.size / 100).floor.toInt)
    val until = math.min(trace.size, (percentEnd * trace.size / 100).ceil.toInt)
    if (complement) {
      trace.replace(events => events.slice(0, from) ++ events.slice(until, events.size))
    } else {
      trace.replace(_.slice(from, until))
    }
  }
}
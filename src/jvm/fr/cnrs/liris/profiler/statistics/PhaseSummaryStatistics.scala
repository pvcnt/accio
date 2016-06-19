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

package fr.cnrs.liris.profiler.statistics

import com.twitter.util.Duration
import fr.cnrs.liris.profiler.{ProfileInfo, ProfilePhase}

/**
 * Extracts and keeps summary statistics from all [[ProfilePhase]]s for formatting to various
 * outputs.
 */
class PhaseSummaryStatistics(infos: Seq[ProfileInfo]) extends Iterable[ProfilePhase] {
  private val durations = ProfilePhase.values.flatMap { phase =>
    infos.flatMap { info =>
      info.phaseTask(phase) match {
        case Some(phaseTask) =>
          val phaseDuration = info.phaseDuration(phaseTask)
          Some(phase -> phaseDuration)
        case None => None
      }
    }
  }.toMap

  val totalDuration: Duration = durations.values.foldLeft(Duration.Zero)(_ + _)

  /**
   * @return whether the given [[ProfilePhase]] was executed
   */
  def contains(phase: ProfilePhase): Boolean = durations.contains(phase)

  /**
   * @return the execution duration of a given [[ProfilePhase]]
   * @throws NoSuchElementException if the given [[ProfilePhase]] was not executed
   */
  @throws[NoSuchElementException]
  def duration(phase: ProfilePhase): Duration = {
    checkContains(phase)
    durations(phase)
  }

  /**
   * @return The duration of the phase relative to the sum of all phase durations
   * @throws NoSuchElementException if the given [[ProfilePhase]] was not executed
   */
  @throws[NoSuchElementException]
  def relativeDuration(phase: ProfilePhase): Double = {
    checkContains(phase)
    durations(phase).inNanoseconds / totalDuration.inNanoseconds
  }

  override def iterator: Iterator[ProfilePhase] = durations.keysIterator

  private def checkContains(phase: ProfilePhase) = {
    if (!contains(phase)) {
      throw new NoSuchElementException(s"Phase $phase was not executed")
    }
  }
}

object PhaseSummaryStatistics {
  def apply(infos: ProfileInfo*): PhaseSummaryStatistics = new PhaseSummaryStatistics(infos)
}
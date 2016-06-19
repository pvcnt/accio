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

package fr.cnrs.liris.profiler

import scala.collection.mutable

/**
 * Phase markers, used as a separators between different phases.
 *
 * @param name        Short name
 * @param description Human readable description
 * @param color       Default color of tasks inside this phase, when rendered in a chart
 */
class ProfilePhase private(val name: String, val description: String, val color: Int)

object ProfilePhase {
  private[this] val registry = mutable.ListBuffer.empty[ProfilePhase]

  def apply(name: String, description: String, color: Int): ProfilePhase = {
    val phase = new ProfilePhase(name, description, color)
    registry += phase
    phase
  }

  def values: Seq[ProfilePhase] = registry.toSeq

  val Launch = ProfilePhase("launch", "Launch Accio", 0x3F9FCF9F /* 9C9 */)
  val Init = ProfilePhase("init", "Initialize command", 0x3F9F9FCF /* 99C */)
  val Exec = ProfilePhase("exec", "Execution", 0x3F9F9FCF /* 99C */)
  val Finish = ProfilePhase("finish", "Complete build", 0x3FFFCFFF /* FCF */)
}
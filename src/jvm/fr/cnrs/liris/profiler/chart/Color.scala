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

package fr.cnrs.liris.profiler.chart

/**
 * Represents a color in ARGB format, 8 bits per channel.
 */
class Color(argb: Int) {
  def red: Int = (argb >> 16) & 0xFF

  def green: Int = (argb >> 8) & 0xFF

  def blue: Int = argb & 0xFF

  def alpha: Int = (argb >> 24) & 0xFF
}

object Color {
  val Red = Color(0xff0000)
  val Green = Color(0x00ff00)
  val Gray = Color(0x808080)
  val Black = Color(0x000000)

  def apply(rgb: Int, hasAlpha: Boolean = false): Color =
    if (hasAlpha) new Color(rgb) else new Color(rgb | 0xff000000)
}
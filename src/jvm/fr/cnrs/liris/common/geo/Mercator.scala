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

package fr.cnrs.liris.common.geo

import com.google.common.geometry.{S1Angle, S2}

private[geo] object Mercator {
  def x2lng(x: Double): S1Angle = S1Angle.radians(x / LatLng.EarthRadius.meters)

  def lng2x(lng: S1Angle): Double = lng.radians * LatLng.EarthRadius.meters

  def y2lat(y: Double): S1Angle = S1Angle.radians(2 * math.atan(math.exp(y / LatLng.EarthRadius.meters)) - S2.M_PI_2)

  def lat2y(lat: S1Angle): Double = Math.log(Math.tan(S2.M_PI_4 + lat.radians / 2)) * LatLng.EarthRadius.meters
}
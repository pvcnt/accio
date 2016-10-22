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

package fr.cnrs.liris.privamov.lppm

/**
 * Started with code donated by K. Briggs; added error estimates, GSL foo, and minor tweaks. Some Lambert-ology from
 * [Corless, Gonnet, Hare, and Jeffrey, "On Lambert's W Function".]
 *
 * @author G. Jungman
 */
private[lppm] object LambertW {

  case class Result(value: Double, err: Double, outcome: Outcome.Value)

  object Outcome {

    sealed trait Value

    case object GSL_SUCCESS extends Value

    case object GSL_EMAXITER extends Value

    case object GSL_EDOM extends Value

  }

  private val M_E = 2.71828182845904523536028747135266250
  private val GSL_DBL_EPSILON = 2.2204460492503131e-16
  private val c = Array(
    -1.0,
    2.331643981597124203363536062168,
    -1.812187885639363490240191647568,
    1.936631114492359755363277457668,
    -2.353551201881614516821543561516,
    3.066858901050631912893148922704,
    -4.175335600258177138854984177460,
    5.858023729874774148815053846119,
    -8.401032217523977370984161688514,
    12.250753501314460424,
    -18.100697012472442755,
    27.029044799010561650)

  def lambertW0(x: Double): Double = {
    val res = lambertW0E(x)
    if (res.outcome eq Outcome.GSL_EMAXITER) {
      throw new RuntimeException("Too many iterations")
    }
    res.value
  }

  def lambertWm1(x: Double): Double = {
    val res = lambertWm1E(x)
    if (res.outcome == Outcome.GSL_EMAXITER) {
      throw new RuntimeException("Too many iterations")
    }
    res.value
  }

  def lambertW0E(x: Double): Result = {
    val one_over_E = 1.0 / M_E
    val q = x + one_over_E
    if (x == 0.0) {
      Result(value = 0.0, err = 0.0, Outcome.GSL_SUCCESS)
    } else if (q < 0.0) {
      Result(value = -1.0, err = Math.sqrt(-q), Outcome.GSL_EDOM)
    } else if (q == 0.0) {
      Result(value = -1.0, err = GSL_DBL_EPSILON, Outcome.GSL_SUCCESS)
    } else if (q < 1.0e-03) {
      val r = Math.sqrt(q)
      val value = seriesEval(r)
      Result(value = value, err = 2.0 * GSL_DBL_EPSILON * Math.abs(value), Outcome.GSL_SUCCESS)
    } else {
      val MAX_ITERS = 10
      var w = .0
      if (x < 1.0) {
        val p = Math.sqrt(2.0 * M_E * q)
        w = -1.0 + p * (1.0 + p * (-1.0 / 3.0 + p * 11.0 / 72.0))
      } else {
        w = Math.log(x)
        if (x > 3.0) {
          w -= Math.log(w)
        }
      }
      halleyIteration(x, w, MAX_ITERS)
    }
  }

  def lambertWm1E(x: Double): Result = {
    if (x > 0.0) {
      lambertW0E(x)
    } else if (x == 0.0) {
      Result(value = 0.0, err = 0.0, Outcome.GSL_SUCCESS)
    } else {
      val MAX_ITERS = 32
      val one_over_E = 1.0 / M_E
      val q = x + one_over_E
      var w = .0
      if (q < 0.0) {
        return Result(value = -1.0, err = Math.sqrt(-q), Outcome.GSL_EDOM)
      }
      if (x < -1.0e-6) {
        val r = -Math.sqrt(q)
        w = seriesEval(r)
        if (q < 3.0e-3) {
          return Result(value = w, err = 5.0 * GSL_DBL_EPSILON * Math.abs(w), Outcome.GSL_SUCCESS)
        }
      } else {
        val L_1 = Math.log(-x)
        val L_2 = Math.log(-L_1)
        w = L_1 - L_2 + L_2 / L_1
      }
      halleyIteration(x, w, MAX_ITERS)
    }
  }

  /**
   * Halley iteration (equation 5.12, Corless et al)
   */
  private def halleyIteration(x: Double, w_initial: Double, max_iters: Int): Result = {
    var w: Double = w_initial
    var i: Int = 0
    while (i < max_iters) {
      {
        var tol: Double = .0
        val e: Double = Math.exp(w)
        val p: Double = w + 1.0
        var t: Double = w * e - x
        if (w > 0) {
          t = (t / p) / e
        }
        else {
          t /= e * p - 0.5 * (p + 1.0) * t / p
        }
        w -= t
        tol = 10 * GSL_DBL_EPSILON * Math.max(Math.abs(w), 1.0 / (Math.abs(p) * e))
        if (Math.abs(t) < tol) {
          return Result(value = w, err = 2.0 * tol, Outcome.GSL_SUCCESS)
        }
        i += 1
      }
    }
    Result(value = w, err = Math.abs(w), Outcome.GSL_EMAXITER)
  }

  /**
   * series which appears for q near zero; only the argument is different for
   * the different branches
   */
  private def seriesEval(r: Double): Double = {
    val t_8 = c(8) + r * (c(9) + r * (c(10) + r * c(11)))
    val t_5 = c(5) + r * (c(6) + r * (c(7) + r * t_8))
    val t_1 = c(1) + r * (c(2) + r * (c(3) + r * (c(4) + r * t_5)))
    c(0) + r * t_1
  }
}


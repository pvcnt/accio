/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.privamov.core.optimization

import com.typesafe.scalalogging.StrictLogging

import scala.util.Random

/**
 * Implementation of simulated annealing. The goal is to approximate the global optimum of a given
 * function, without exploring the entire search space.
 *
 * @param system          System to evaluate
 * @param coolingSchedule Cooling schedule
 * @param iters           Number of iterations at each step
 * @tparam T Type of evaluated element
 * @see http://katrinaeg.com/simulated-annealing.html
 * @see https://en.wikipedia.org/wiki/Simulated_annealing
 */
final class SimulatedAnnealing[T](system: AnnealingSystem[T], coolingSchedule: CoolingSchedule, iters: Int = 1) extends StrictLogging {
  require(iters > 0, s"Number of iterations per step must be stricly positive (got $iters)")
  private[this] val random = new Random

  /**
   * Run the optimization.
   *
   * @return The "optimal" solution found with its cost
   */
  def run(): AnnealingResult[T] = {
    // Initial solution is provided by the system (with its associated cost).
    var result = initialSolution()
    if (result.cost == 0) {
      // If the cost of the initial solution is null, we can terminate now.
      logger.debug(s"Accepted solution ${result.value} (cost=0, initial)")
      return result
    }

    // Initial temperature is provided by the cooling schedule.
    var temp = coolingSchedule.start
    logger.debug(s"Initial solution ${result.value} (cost=${result.cost}) at T=$temp until ${coolingSchedule.minimum}")

    // We keep a trace of the best solution found so far. That way, if we end up with a worst
    // solution than one we have seen so far, we can return it.
    var bestSoFar = result
    while (temp > coolingSchedule.minimum) {
      // For each temperature, the simulation will be ran several times.
      for (i <- 0 until iters) {
        val newResult = neighbor(result.value)
        if (newResult.cost == 0) {
          // If the cost of the new solution is null, we can terminate now.
          logger.debug(s"Accepted solution ${newResult.value} (cost=0) at T=$temp, iter=$i")
          return newResult
        }
        // We compute an acceptance probability for the new solution.
        val ap = system.acceptanceProbability(result.cost, newResult.cost, temp)
        require(ap >= 0 && ap <= 1, s"Acceptance probability must be in [0,1] (got $ap for oldCost=${result.cost}, newCost=${newResult.cost}, T=$temp)")
        if (ap == 1 || ap >= random.nextDouble()) {
          logger.debug(s"Accepted ${newResult.value} (cost=${newResult.cost}, ap=$ap) at T=$temp, iter=$i")
          result = newResult
          if (result.cost < bestSoFar.cost) {
            bestSoFar = result
          }
        } else {
          logger.debug(s"Rejected ${newResult.value} (cost=${newResult.cost}, ap=$ap) at T=$temp, iter=$i")
        }
      }
      // We gradually decrease the temperature.
      temp = coolingSchedule.decrease(temp)
      logger.debug(s"Decreased temperature to $temp")
    }
    if (bestSoFar.cost < result.cost) {
      logger.info(s"Accepted solution ${bestSoFar.value} (cost=${bestSoFar.cost}, best so far)")
      bestSoFar
    } else {
      logger.info(s"Accepted solution ${result.value} (cost=${result.cost})")
      result
    }
  }

  /**
   * Compute the initial solution and its associated cost.
   */
  private def initialSolution(): AnnealingResult[T] = {
    val solution = system.initialSolution()
    val cost = system.cost(solution)
    AnnealingResult(solution, cost)
  }

  /**
   * Compute a neighbor solution and its associated cost.
   */
  private def neighbor(solution: T): AnnealingResult[T] = {
    val newSolution = system.neighbor(solution)
    val cost = system.cost(newSolution)
    AnnealingResult(newSolution, cost)
  }
}

/**
 * Result of a simulated annealing.
 *
 * @param value The optimal solution found
 * @param cost  Cost of this solution
 * @tparam T Type of solution
 */
case class AnnealingResult[T](value: T, cost: Double) extends Ordered[AnnealingResult[T]] {
  override def compare(that: AnnealingResult[T]): Int = cost.compare(that.cost)
}

/**
 * A cooling schedule manages the temperature during a simulated annealing.
 */
trait CoolingSchedule {
  /**
   * Return the initial temperature.
   */
  def start: Double

  /**
   * Decrease the temperature. If it goes below the minimum temperature, simulation will stop.
   *
   * @param temp Actuel temperature
   * @return New temperature
   */
  def decrease(temp: Double): Double

  /**
   * Minimum temperature under which the simulation will stop.
   */
  def minimum: Double
}

object CoolingSchedule {
  /**
   * @param start       Initial temperature
   * @param minimum     Minimum temperature (simulation stops when it is reached)
   * @param coolingRate Cooling rate (temperature will be multiplied by this factor after each step)
   */
  def apply(start: Double = 1d, minimum: Double = 0.00001, coolingRate: Double = 0.9): CoolingSchedule =
    new BasicCoolingSchedule(start, minimum, coolingRate)
}

/**
 * @param start       Initial temperature
 * @param minimum     Minimum temperature (simulation stops when it is reached)
 * @param coolingRate Cooling rate (temperature will be multiplied by this factor after each step)
 */
private class BasicCoolingSchedule(override val start: Double, override val minimum: Double, coolingRate: Double) extends CoolingSchedule {
  require(start >= minimum, s"Initial temperature must be greater than minimal temperature (got $start < $minimum)")
  require(coolingRate > 0 && coolingRate < 1, s"Cooling rate must be in [0,1] (got $coolingRate)")

  override def decrease(temp: Double): Double = temp * coolingRate
}

/**
 * A system being evaluated through simulated annealing.
 *
 * @tparam T Type of element to evaluate
 */
trait AnnealingSystem[T] {
  /**
   * Generate an initial solution. It should include some randomness.
   */
  def initialSolution(): T

  /**
   * Return the cost of a given solution. It should be deterministic.
   *
   * @param solution A solution to evaluate
   */
  def cost(solution: T): Double

  /**
   * Generate a neighboring solution from a given solution. It should include some randomness.
   *
   * @param solution A solution
   */
  def neighbor(solution: T): T

  /**
   * Return the acceptance probability.
   *
   * @param oldCost Cost of the actual solution
   * @param newCost Cost of the candidate solution
   * @param temp    Current temperature
   * @return A probability for the new solution to be accepted (hence in [0,1])
   */
  def acceptanceProbability(oldCost: Double, newCost: Double, temp: Double): Double
}
package fr.cnrs.liris.accio.core.pipeline

import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.framework.{GraphDef, Optimization, Report}
import fr.cnrs.liris.accio.core.param.ParamMap
import fr.cnrs.liris.common.random.RandomUtils
import fr.cnrs.liris.privamov.lib.optimization.CoolingSchedule

import scala.util.Random

class SimulatedAnnealingStrategy(graph: GraphDef, optimization: Optimization) extends ExecutionStrategy with StrictLogging {
  private[this] val costEvaluator = new CostEvaluator(optimization.objectives)
  private[this] val random = new Random
  private[this] val coolingSchedule = CoolingSchedule()

  private[this] var temp = coolingSchedule.start
  private[this] var iter = 0
  private[this] var previous: Option[(GraphDef, Double)] = None

  override def next: Seq[GraphDef] = {
    val solution = optimization.paramGrid.random()
    Seq(graph.set(solution))
  }

  override def next(graph: GraphDef, report: Report): Seq[GraphDef] = {
    val nextGraph = if (temp < coolingSchedule.minimum) {
      // We reached the minimal temperature.
      logger.debug(s"Reached minimal temperature at T=$temp")
      None
    } else if (previous.isEmpty) {
      val cost = costEvaluator.compute(report)
      if (cost == 0) {
        // If the cost of the solution is null, we can terminate now.
        logger.debug(s"Got null cost at T=$temp, iter=$iter")
        None
      } else {
        previous = Some(graph -> cost)
        Some(graph.set(neighbor(getSolution(graph))))
      }
    } else {
      val cost = costEvaluator.compute(report)
      if (cost == 0) {
        // If the cost of the solution is null, we can terminate now.
        logger.debug(s"Got null cost at T=$temp, iter=$iter")
        None
      } else {
        // We compute an acceptance probability for the new solution.
        val ap = acceptanceProbability(previous.get._2, cost, temp)
        if (ap == 1 || ap >= random.nextDouble()) {
          val solution = getSolution(graph)
          logger.debug(s"Accepted $solution (cost=$cost, ap=$ap) at T=$temp, iter=$iter")
          previous = Some(graph -> cost)
          Some(graph.set(neighbor(solution)))
        } else {
          logger.debug(s"Rejected ${getSolution(graph)} (cost=$cost, ap=$ap) at T=$temp, iter=$iter")
          Some(graph.set(neighbor(getSolution(previous.get._1))))
        }
      }
    }
    nextGraph match {
      case None => Seq.empty
      case Some(g) =>
        iter += 1
        if (iter >= optimization.iters) {
          iter = 0
          temp = coolingSchedule.decrease(temp)
          logger.debug(s"Decreased temperature to $temp")
        }
        Seq(g)
    }
  }

  private def getSolution(graph: GraphDef) = graph.params.filter(optimization.paramGrid.keys)

  private def acceptanceProbability(oldCost: Double, newCost: Double, temp: Double): Double = {
    if (oldCost == 0) {
      0
    } else if (newCost < oldCost) {
      1
    } else {
      1d / (1 + math.exp((newCost - oldCost) / (0.5 * temp * optimization.objectives.size)))
    }
  }

  private def neighbor(solution: ParamMap): ParamMap = {
    val eligible = solution.toSeq.map(_._1).intersect(optimization.paramGrid.keys.toSeq).filter(optimization.paramGrid(_).length > 1)
    if (eligible.isEmpty) {
      logger.warn("No eligible param to compute neighboring solution")
      solution
    } else {
      val elected = RandomUtils.randomElement(eligible)
      solution.set(elected -> neighbor(solution(elected), optimization.paramGrid(elected).asInstanceOf[Array[Any]]))
    }
  }

  private def neighbor[T](value: T, domain: Array[T]): T = {
    val pos = domain.indexOf(value)
    require(pos > -1, s"Value $value is not part of domain")
    if (domain.length == 1) {
      value
    } else {
      var size = math.min(domain.length, math.ceil(domain.length * optimization.contraction).toInt)
      if ((size % 2) == 0) {
        size += 1
      }
      val middle = (size - 1) / 2
      //TODO: very very innefficient.
      val neighborhood = domain.sliding(size).filter(_.contains(value)).toSeq.sortBy(w => math.abs(middle - w.indexOf(value))).head.filterNot(_ == value)
      val el = RandomUtils.randomElement(neighborhood)
      el
    }
  }
}
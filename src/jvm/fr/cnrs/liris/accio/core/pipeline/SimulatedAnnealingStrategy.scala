package fr.cnrs.liris.accio.core.pipeline

import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.param.ParamMap
import fr.cnrs.liris.common.random.RandomUtils
import fr.cnrs.liris.privamov.lib.optimization.CoolingSchedule

import scala.util.Random

case class AnnealingMeta(temp: Double, iter: Int, previous: Option[(ParamMap, Double)])

class SimulatedAnnealingStrategy(graphUnderOptimization: GraphDef, optimization: Optimization) extends ExecutionStrategy with StrictLogging {
  private[this] val costEvaluator = new CostEvaluator(optimization.objectives)
  private[this] val random = new Random
  private[this] val coolingSchedule = CoolingSchedule()

  override def next: Seq[(GraphDef, Any)] = {
    val solution = optimization.paramGrid.random()
    Seq(graphUnderOptimization.setParams(solution) -> AnnealingMeta(coolingSchedule.start, 1, None))
  }

  override def next(graphDef: GraphDef, meta: Any, report: Report): Seq[(GraphDef, Any)] =
    meta match {
      case m: AnnealingMeta => next(graphDef, m, report)
    }

  private def next(graphDef: GraphDef, meta: AnnealingMeta, report: Report) = {
    val solution = getSolution(graphDef)
    val cost = costEvaluator.compute(report)

    val nextState = if (meta.temp < coolingSchedule.minimum) {
      // We reached the minimal temperature.
      logger.debug(s"Reached minimal temperature at T=${meta.temp}")
      State.Terminate
    } else if (meta.previous.isEmpty) {
      if (cost == 0) {
        // If the cost of the solution is null, we can terminate now.
        logger.debug(s"Got null cost at T=${meta.temp}, iter=${meta.iter}")
        State.Terminate
      } else {
        State.Accept
      }
    } else {
      if (cost == 0) {
        // If the cost of the solution is null, we can terminate now.
        logger.debug(s"Got null cost at T=${meta.temp}, iter=${meta.iter}")
        State.Terminate
      } else {
        // We compute an acceptance probability for the new solution.
        val ap = acceptanceProbability(meta.previous.get._2, cost, meta.temp)
        if (ap == 1 || ap >= random.nextDouble()) {
          logger.debug(s"Accepted $solution (cost=$cost, ap=$ap) at T=${meta.temp}, iter=${meta.iter}")
          State.Accept
        } else {
          logger.debug(s"Rejected ${getSolution(graphUnderOptimization)} (cost=$cost, ap=$ap) at T=${meta.temp}, iter=${meta.iter}")
          State.Reject
        }
      }
    }
    val nextSolution = nextState match {
      case State.Terminate => None
      case State.Accept =>
        val nextGraphDef = graphUnderOptimization.setParams(neighbor(solution))
        val nextMeta = nextStep(meta).copy(previous = Some(solution -> cost))
        Some(nextGraphDef -> nextMeta)
      case State.Reject =>
        val nextGraphDef = graphUnderOptimization.setParams(neighbor(meta.previous.get._1))
        val nextMeta = nextStep(meta)
        Some(nextGraphDef -> nextMeta)
    }
    nextSolution.toSeq
  }

  private def nextStep(meta: AnnealingMeta): AnnealingMeta = {
    if (meta.iter >= optimization.iters - 1) {
      val nextTemp = coolingSchedule.decrease(meta.temp)
      logger.debug(s"Decreased temperature to $nextTemp")
      meta.copy(iter = 0, temp = nextTemp)
    } else {
      meta.copy(iter = meta.iter + 1)
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

private sealed trait State

private object State {

  case object Terminate extends State

  case object Reject extends State

  case object Accept extends State

}
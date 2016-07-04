package fr.cnrs.liris.accio.core.pipeline

import breeze.stats._
import com.typesafe.scalalogging.LazyLogging

/**
 * Evaluate the cost resulting of a report according to a set of objectives.
 *
 * @param objectives Objectives
 */
class CostEvaluator(objectives: Set[Objective]) extends LazyLogging {
  /**
   * Compute the cost associated with a given report.
   *
   * @param report Report
   */
  def compute(report: RunReport): Double = objectives.map(compute(_, report)).sum

  private def compute(objective: Objective, report: RunReport): Double = {
    val values = report.artifacts.filter(art => art.name == objective.metric)
    if (values.isEmpty) {
      logger.warn(s"No artifact found with name ${objective.metric}")
    } else if (values.size > 1) {
      logger.warn(s"Multiple artifacts found with name ${objective.metric}")
    }
    val costs = values.map {
      case s: ScalarArtifact => compute(objective, s.value)
      case d: DistributionArtifact => mean(d.values.map(compute(objective, _)))
      case a =>
        logger.warn(s"Invalid artifact on which to compute cost: $a")
        0d
    }
    costs.sum
  }

  private def compute(objective: Objective, value: Double): Double = {
    val cost = objective match {
      case o: Objective.Maximize =>
        // We should maximize, which means the lower the value, the higher the cost.
        1 - scale(value, o.threshold)
      case o: Objective.Minimize =>
        // We should minimize, which means the higher the value, the higher the cost.
        scale(value, o.threshold)
    }
    cost
  }

  private def scale(value: Double, threshold: Option[Double]): Double =
    threshold.map(v => math.min(value, v) / v).getOrElse(value)
}

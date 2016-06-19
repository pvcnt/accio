package fr.cnrs.liris.accio.core.framework

import fr.cnrs.liris.accio.core.dataset.{Dataset, DatasetEnv}
import fr.cnrs.liris.accio.core.model.Trace
import fr.cnrs.liris.accio.core.param.{Identifiable, Parameterizable}

sealed trait Operator extends Parameterizable with Identifiable {
  override val uid: String = Identifiable.uniqid(getClass.getSimpleName)
}

trait Source extends Operator {
  def get(env: DatasetEnv): Dataset[Trace]
}

trait Analyzer extends Operator {
  def analyze(trace: Trace): Seq[Metric]
}

/**
 * Evaluators compute metrics by comparing two traces, the first coming from reference data and
 * the other from a treatment.
 */
trait Evaluator extends Operator {
  def evaluate(reference: Trace, result: Trace): Seq[Metric]
}

/**
 * A metric evaluates an output and produces a single scalar.
 *
 * @param name  Metric name
 * @param value Metric result
 */
case class Metric(name: String, value: Double)

trait Transformer extends Operator {
  def transform(trace: Trace): Seq[Trace]
}

trait Mapper extends Transformer {
  override def transform(trace: Trace): Seq[Trace] = Seq(map(trace))

  def map(trace: Trace): Trace
}

trait Filter extends Transformer {
  override def transform(trace: Trace): Seq[Trace] =
    if (filter(trace)) Seq(trace) else Seq.empty

  def filter(trace: Trace): Boolean
}
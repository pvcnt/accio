package fr.cnrs.liris.accio.core.framework

import fr.cnrs.liris.accio.core.dataset.{Dataset, DatasetEnv}
import fr.cnrs.liris.accio.core.model.Trace
import fr.cnrs.liris.accio.core.param.Parameterizable

/**
 * Operators are the basic processing unit of Accio.
 */
sealed trait Operator extends Parameterizable

/**
 * A source creates a dataset of traces. It has no dependencies and is therefore used as a
 * root operator.
 */
trait Source extends Operator {
  /**
   * Return a dataset of trace.
   *
   * @param env Dataset environment
   */
  def get(env: DatasetEnv): Dataset[Trace]
}

/**
 * An analyzer computes metrics from a single input trace.
 */
trait Analyzer extends Operator {
  /**
   * Analyze an input trace.
   *
   * @param trace Input trace
   * @return Some metrics
   */
  def analyze(trace: Trace): Seq[Metric]
}

/**
 * An evaluator computes metrics by comparing two versions of the same trace, one from a training
 * dataset (i.e., the reference trace) and the other from a testing dataset (i.e., a modified
 * version of the reference trace).
 */
trait Evaluator extends Operator {
  /**
   * Compare a train trace with a test trace.
   *
   * @param train Train trace
   * @param test  Test trace
   * @return Some metrics
   */
  def evaluate(train: Trace, test: Trace): Seq[Metric]
}

/**
 * A metric value is basically a named double.
 *
 * @param name  Metric name
 * @param value Metric result
 */
case class Metric(name: String, value: Double)

/**
 * A transformer produces zero, one or many traces from a single input trace. It can be used to
 * implement filtering (cf. [[Filter]]), mapping (cf. [[Mapper]]) or more complex operations like
 * splitting data.
 */
trait Transformer extends Operator {
  /**
   * Transform an input trace into zero, one or many other traces.
   *
   * @param trace Input trace
   * @return Output trace(s)
   */
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
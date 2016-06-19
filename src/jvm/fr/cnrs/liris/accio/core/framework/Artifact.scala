package fr.cnrs.liris.accio.core.framework

import fr.cnrs.liris.accio.core.dataset.Dataset
import fr.cnrs.liris.accio.core.model.Trace

/**
 * An artifact is something produced by an operator and possibly consumed by another operator.
 * Artifacts have a unique name among all artifacts produced by a given graph.
 */
sealed trait Artifact {
  /**
   * Return the unique name of this artifact.
   */
  def name: String

  def ephemeral: Boolean = false

  def `type`: String
}

/**
 * An artifact holding a dataset of traces.
 *
 * @param name Artifact unique name
 * @param path Path where the dataset has been persisted
 */
case class StoredDatasetArtifact(name: String, path: String) extends Artifact {
  override def `type`: String = "dataset"
}

/**
 * An artifact holding a dataset of traces.
 *
 * @param name Artifact unique name
 * @param data Trace dataset
 */
case class DatasetArtifact(name: String, data: Dataset[Trace]) extends Artifact {
  override def ephemeral: Boolean = true

  override def `type`: String = "dataset"
}

/**
 * An artifact holding a metric as a single double.
 *
 * @param name  Artifact unique name
 * @param value Double value
 */
case class ScalarArtifact(name: String, value: Double) extends Artifact {
  override def `type`: String = "scalar"
}

/**
 * An artifact holding a metric as a distribution of double.
 *
 * @param name   Artifact unique name
 * @param values List of double values
 */
case class DistributionArtifact(name: String, values: Seq[Double]) extends Artifact {
  override def `type`: String = "distribution"
}
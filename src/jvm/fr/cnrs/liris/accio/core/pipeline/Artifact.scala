package fr.cnrs.liris.accio.core.pipeline

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonProperty, JsonSubTypes, JsonTypeInfo}
import fr.cnrs.liris.accio.core.dataset.Dataset
import fr.cnrs.liris.accio.core.model.Trace

/**
 * An artifact is something produced by an operator and possibly consumed by another operator.
 * Artifacts have a unique name among all artifacts produced by a given graph.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(Array(
  new Type(value = classOf[StoredDatasetArtifact], name = "dataset"),
  new Type(value = classOf[ScalarArtifact], name = "scalar"),
  new Type(value = classOf[DistributionArtifact], name = "distribution")
))
sealed trait Artifact {
  /**
   * Return the unique name of this artifact.
   */
  def name: String

  /**
   * Return whether this artifact is ephemeral (i.e., never and only flows between operators)
   * or not (i.e., persisted after being produced).
   */
  def ephemeral: Boolean = false

  /**
   * Return the type of this artifact.
   */
  @JsonProperty
  def `type`: String
}

/**
 * An artifact holding a dataset of traces read from a path.
 *
 * @param name Artifact unique name
 * @param path Path where the dataset is stored
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
package fr.cnrs.liris.accio.core.pipeline

import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonProperty}
import fr.cnrs.liris.accio.core.param.{ParamGrid, ParamMap}
import org.joda.time.{DateTime, Duration}

/**
 * An experiment is a specification about the manner to run one or many variations of a single workflow. It can be
 * either a direct execution of this workflow, an exploration of parameters or an optimization of parameters.
 *
 * @param id           Unique identifier (among all experiments AND runs)
 * @param name         Human-readable name
 * @param workflow     Base workflow
 * @param paramMap     Override parameters map
 * @param exploration  Parameters exploration (cannot be combined with `optimization`)
 * @param optimization Parameters optimization (cannot be combined with `exploration`)
 * @param notes        Some notes
 * @param tags         Some tags
 * @param initiator    User initiating the experiment
 */
case class Experiment(
    id: String,
    name: String,
    workflow: Workflow,
    paramMap: Option[ParamMap],
    exploration: Option[Exploration],
    optimization: Option[Optimization],
    notes: Option[String],
    tags: Set[String],
    initiator: User,
    report: Option[ExperimentReport] = None) {
  require(!(exploration.isDefined && optimization.isDefined), "Cannot define both an exploration and an optimization")

  def shortId: String = id.substring(0, 8)

  def setParams(paramMap: ParamMap): Experiment = {
    val newParamMap = this.paramMap.map(_ ++ paramMap).getOrElse(paramMap)
    copy(paramMap = Some(newParamMap))
  }
}

case class Exploration(paramGrid: ParamGrid)

case class Optimization(
    paramGrid: ParamGrid,
    iters: Int,
    contraction: Double,
    objectives: Set[Objective]) {
  require(iters > 0, s"Number of iterations per step must be strictly positive (got $iters)")
  require(contraction > 0 && contraction <= 1, s"Contraction factor must be in (0,1] (got $contraction)")
}

@JsonIgnoreProperties(ignoreUnknown = true)
case class ExperimentReport(
    startedAt: DateTime = DateTime.now,
    completedAt: Option[DateTime] = None,
    runs: Seq[String] = Seq.empty) {

  /**
   * Return whether the execution is completed, either successfully or not.
   */
  @JsonProperty
  def completed: Boolean = completedAt.nonEmpty

  /**
   * Return the execution duration.
   */
  @JsonProperty
  def duration: Option[Duration] = completedAt.map(end => Duration.millis(end.getMillis - startedAt.getMillis))

  /**
   * Return a copy of this report with a new child node.
   *
   * @param id Run identifier
   */
  @throws[IllegalStateException]
  def addRun(id: String): ExperimentReport = copy(runs = runs ++ Seq(id))

  /**
   * Return a copy of this report with the execution marked as completed.
   *
   * @param at Time at which the execution completed
   */
  def complete(at: DateTime = DateTime.now): ExperimentReport = copy(completedAt = Some(at))
}

/**
 * A workflow is a named graph of operators. Workflows are instantiated through [[Experiment]]'s.
 *
 * @param graph Graph of operators
 * @param name  Human-readable name
 * @param owner User owning this workflow
 */
case class Workflow(graph: GraphDef, name: Option[String] = None, owner: Option[User] = None) {
  /**
   * Return a copy of this workflow with new parameters propagated into the graph.
   *
   * @param paramMap Override parameters map
   */
  def setParams(paramMap: ParamMap): Workflow = copy(graph = graph.setParams(paramMap))

  /**
   * Return a copy of this workflow with a number of runs propagated into the graph to nodes that would otherwise
   * run only one time. For nodes for which an explicit number of runs is already set, this value will not be overriden.
   *
   * @param runs Minimum number of runs
   */
  def setRuns(runs: Int): Workflow = copy(graph = graph.setRuns(runs))
}

/**
 * A user of Accio.
 *
 * @param name  Username
 * @param email Email address
 */
case class User(name: String, email: Option[String] = None) {
  override def toString: String = name + email.map(" <" + _ + ">").getOrElse("")
}

/**
 * Factory of [[User]].
 */
object User {
  private[this] val UserRegex = "(.+)<(.+)>".r

  /**
   * Parse a string into a user.
   * If it includes an email address, it should have the following format: "User name <handle@domain.tld>".
   *
   * @param str String to parse
   */
  def parse(str: String): User = str match {
    case UserRegex(name, email) => new User(name.trim, Some(email.trim))
    case _ => new User(str, None)
  }
}
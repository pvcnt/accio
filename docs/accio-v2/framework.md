Deployment
----------


Experiment
----------
It is possible to overwrite some parameters, or test various parameters values, or even to launch a parameters optimization.

An experiment is formed of:
  * a workflow, at a particular version;
  * a descriptive name, some tags and notes;
  * an owner;
  * a set of parameters.

Workflow run
------------
A run is an instantiation of a particular workflow for a particular set of parameters.
It is always associated with either an experiment or a deployment.

A workflow run is formed of:
  * a workflow, at a particular version;
  * a descriptive name (optional);
  * values of overwritten parameters;
  * a creation date.

Once it is completed, it comes with a report and some artifacts.

Workflow
--------
A worfklow defines a data processing pipeline.
This pipeline can be viewed as a graph, where nodes are instances of operators and edges are datasets flowing between operators.

A workflow is formed of:
  * a descriptive name;
  * a version;
  * an owner;
  * nodes.

Workflow nodes are defined with:
  * an operator type;
  * values for all parameters;
  * a unique name among all nodes inside the same workflow;
  * a scoped status for each input, forcing it to be scoped;
  * an ephemeral status for each output, forcing it to be ephemeral.

Operator
--------
An operator is the basic building block of a workflow.
It has a unique name and is parametrized by some parameters which must be defined when instanting the operator.
Operators can be categorised as (informally, the interface from a technical point of view is the same):
  * a learner, when it produces a single non-learner operator;
  * a source, when it consumes nothing and produces one or several datasets;
  * a transformer, when it consumes one or several datasets and produces one or several datasets;
  * an evaluator, when it consumes one or several datasets and produces only artifacts.

The general contract for operators is the following:
  * it consumes 0..n datasets;
  * it produces either a single operator (that does not produce yet another operator) or 0..n datasets;
  * it produces additionnaly 0..n artifacts.

All inputs and outputs have a name and are statically defined (i.e., they cannot appear or disappear at runtime) and typed.
You will find after a list of supported types for datasets, parameters and artifacts.

Inputs may be marked as scoped, which means it will be executed one time per trace inside the input dataset, and hence is guaranteed to receive only data coming from a single trace.
Outputs may be marked as ephemeral, which means the intermediate result will never be stored (it is only used for artifacts).
An operator may be marked as unstable, which means it behaves in a non-deterministic manner.

Dataset types
-------------
Here is the list of data structures supported as dataset types.
  * Event
  * TODO: Example, Prediction

The page about (the data model)[model.md] contains more details about data types.

Parameter types
---------------
Here is the list of Scala/Java types supported as parameters with annotations that can be added to specify their domain of definition.
  * Numeric:
    * Scala class: Int, Long or Double
    * Relevant annotations: @Min(value), @Max(value), @Log
  * Boolean:
    * Scala class: Boolean
    * Relevant annotations: -
  * String:
    * Scala class: String
    * Relevant annotations: @OneOf(value[])
  * Timestamp:
    * Scala class: com.twitter.util.Time
    * Relevant annotations: @Before(value), @After(value), @Precision(value), @PastTime, @FutureTime
  * Duration:
    * Scala class: com.twitter.util.Duration
    * Relevant annotations: @Min(value), @Max(value), @Precision(value), @Log
  * Location:
    * Scala class: fr.cnrs.liris.geo.LatLng
    * Relevant annotations: -
  * Distance:
    * Scala class: fr.cnrs.liris.geo.Distance
    * Relevant annotations: @Min(value), @Max(value), @Log

Any parameter may be wrapped inside an Option[_] to indicate it is not mandatory.
In this case, its default value should be None.
Launching a pipeline
====================
  * Workflow selection:
    * Workflow to launch
    * Version (last one or specific one)
  * Metadata:
    * Run name
    * Run notes (optional)
    * Tags (optional)
  * Inputs:
    * Training dataset, either:
      * a path to a dataset, possibly restricted, or
      * use currently deployed model (if any)
    * Testing dataset: a path to a dataset, possibly restricted
    * Override any operator parameter, with a single or several (possibly random) value(s)
  * Runtime
    * Launch, either:
      * Once: now or at specific time, or
      * Recurrent: cron-like spec
    * Notifications:
      * Who to alert: workflow owner/run initiator/other people
      * When: start, complete, error

Datasets can be restricted by dates, a random sampling (fraction) or a sequential split (fraction).

Deploy a pipeline to production
===============================
  * Workflow selection:
    * Workflow to launch
    * Version (last one or specific one)
  * Endpoint specification:
    * Path
  * Inputs:
    * Use the training model of a given run
    * Minimum/maximum number of elements inside each mini-batch
    * Operators parameters that can be dynamically adjusted (transformers only)
    * Override any operator parameter
    * Must be scoped or not

Actions on an experiment
========================
  * Clone
  * Stop/cancel
  * Delete
  * Edit metadata
  * Deploy to production

Operator:
  * Parameters: simple values or other operators.
    * Simple values must have a default value. This allow to instantiate default operators that will
    later go through optimization via grid search.
    * Operators must be defined.
  * Inputs: a batch of data. Each operator can have zero, one or many inputs. Last input of an
  operator can be defined as multiple (which means it is actually a list of inputs).
  * Outputs: a batch of data. Each operator can have zero, one or many outputs.
  * Artifacts: simple or complex values.
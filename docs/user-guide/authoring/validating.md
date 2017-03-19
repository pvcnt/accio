---
layout: docs
weight: 30
title: Validating definition files
---

Workflow and run definitions can grow pretty big, especially for complex experiments.
It becomes easy to do a typing mistake that could invalidate them.
Accio client comes with an [`accio validate` command](../../reference/commands/validate.html) that allows to check those files are correct without actually using them.
This command will not create or update anything on the cluster, but only check for the validity of definition files.
Here is a non exhaustive list of things this command will check:

  * JSON is syntaxically correct
  * names of nodes, parameters, etc. are valid
  * graph of operators is acyclic
  * references to other nodes, ports, parameters are valid (i.e., they exist) and allowed (i.e., data types match)
  * values for inputs or parameters are allowed (i.e., data types match)
  * all inputs have a value, whether it is a default or a user-specified one
  * usage of deprecated operators

Once files are validated, they are guaranteed to be accepted.
In case of an error, this command will do its best to provide meaningful information to help fixing the errors.
This command issues two kinds of messages: errors and warnings.
While errors have to be fixed for the file to be valid, warnings do not impact the usability of the definition.
However, it is recommended to take into account warnings (e.g., using a deprecated operator can break at any time, since they might be removed).

The [`accio get` command](../../reference/commands/get.html) can be of a great help to find relevant resources, whether it is a workflow or an operator, as workflow and run definition files need to reference them.
The [`accio describe` command](../../reference/commands/describe.html) can then be used to get all the details about a resource.
It is especially useful to get the list of parameters a workflow accepts, or the inputs and outputs exposed by an operator.

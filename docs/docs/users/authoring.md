---
layout: docs
nav: docs
section: users
title: Authoring experiments
---

The first step of any experiment is to define it in a way that Accio can understand.

## Authoring a workflow
Workflows define the anatomy of an experiment, i.e., how operators are combined to get meaningful results.
Workflows are shared among Accio users, which means that you should first start by verifying if there is not already a workflow defined for what you want to do.
If this is the case, you can directly switch to the next section about [authoring runs](#authoring-a-run).

Workflows are searchable with the [`accio workflows` command](../client/workflows-command.html) or by using the [Web UI](../web-ui.html).
You can search for workflows by their owner or name.
Please note that the web interface is more appropriate when it comes to workflow visualization, because it gives you a graphical view and makes connections between operators more apparent.

Each workflow is identifier with a unique identifier, the `id` field in the DSL.
This identifier is very important because it will have to be specified later when creating runs.
It should be descriptive enough to avoid collisions.

If you want to create a new workflow or update an existing one, you will need to use the [appropriate DSL](../workflow-dsl.html), which is based on JSON.
Do not forget that workflows can be parametrized, allowing you to create generic experiments that can be launched with various different parameters.
It is up to you to find the good granularity when writing workflows;
some will prefer to write a few very generic workflows, taking many parameters allowing to simulate a lot of use cases;
others will prefer to write more workflows, but each one being more focused on a specific use case.

Once you have written your new workflow, or a new version of an existing workflow, you need to push it to the Accio cluster with the [`accio push` command](../client/push-command.html).
If you push a workflow which version already exists, based on the `id` field, a new version will be created.
Workflows are versioned, which allows past runs to reference workflows at previous versions, even if they have been updated in-between.
For the same reason, it is generally not possible to delete a workflow.

## Authoring a run
After having created or found your dream workflow, you need to launch it.
An instance of a workflow is called a run, and is created with the [`accio submit` command](../client/submit-command.html).
The mandatory elements when creating a run are the identifier of the workflow to launch and values for each non-optional parameter.
Moreover, it is possible to provide metadata for the created run: a name and tags, both displayed in lists and used when searching for runs, and some notes, describing the purpose of a run.
This metadata is the only thing about a run that can be modified later, so do not worry too much if you forgot something or made a typo when creating a run.



## Validating definition files
Workflow and run definition files can be validated thanks to the [`accio validate` command](../client/validate-command.html).
This command will not create or update anything on the cluster, but only check there definition files are correct and can be executed without any problem.
In case of an error, this command will give meaningful information to help you fix errors.

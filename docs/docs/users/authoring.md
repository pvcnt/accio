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

If you want to create a new workflow or update an existing one, you will need to use the [appropriate DSL](../workflow-dsl.html), which is based on JSON.

## Authoring a run

## Validating definition files
Workflow and run files can be validated thanks to the [`accio validate`](../client/validate-command.html) command, which takes one or several files as arguments.
This command will not actually push workflows or create runs, but only check there are correct and can be executed without any problem.
In case of an error, it will do its best to provide meaningful information to help you fix errors.
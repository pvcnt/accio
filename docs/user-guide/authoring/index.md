---
layout: docs
weight: 50
separator: true
title: Authoring experiments
---

This section is about creating and launching experiments on Accio.
This is a two-steps process:

  1. **[Creating a workflow](workflows.html):**
  A workflow is a template for an experiment, that will later be instantiated.
  It is created with a workflow definition language.
  Workflows are shared between users of a cluster, allowing to easily reproduce results and test different variations.
  2. **[Creating a run](runs.html):**
  A run is one or several instances of a workflow.
  It is created with a run definition language.
  A run can be as simple as a single execution of a workflow, or as complex as a parameter sweep exploring hundreds variants of a workflow.

<hr/>

This section also contains additional material about workflow and run definitions:

  * [Validating](validating.html): Check that definitions are correct without actually creating them.
  * [Best practices](best-practices.html): Tips about writing reusable and efficient definitions.

---
layout: docs
nav: docs
title: Accio overview
---

Welcome to the Accio documentation!
This is the best place to find information about how to use, administrate or develop Accio.
If you are extremely unlucky and happen to stumble upon a mistake, or just want to extend documentation, Markdown source is [available on GitHub](https://github.com/privamov/accio/tree/master/docs).
Pull requests are welcome!

## What is Accio ?
Accio is scientific workflow management system.
It allows to easily define and run scientific experiments, monitor their execution and analyze their results.
Although Accio is designed as an agnostic scientific workflow management tool, it has been successfully applied to study location privacy in the past years.

The key features of Accio are:

  * **Simple DSL:** A simple JSON-based DSL allows users to easily design their experiments and submit them to Accio.
  The same DSL allows to create as well a single run as an experiment testing thousands of different parameters combinations.
  * **Powerful type system:** All inputs and outputs have a type that is strongly enforce.
  It prevents some basic mistakes, like providing an integer instead of a string, and enables us to build powerful visualization tools, adapted to the data type.
  * **Focus on location privacy:** A complete set of workflow operators designed to study location privacy are provided with Accio.
  Researchers can quickly begin experimenting with state-of-the-art protection mechanisms and metrics.
  Among the various usages that Accio enables, we have extensively used it to evaluate, compare and tune protection mechanisms.
  * **Easy results analysis:** Accio does not stop when experiments are completed.
  It also comes with a set of tools to export or visualize results of experiments.
  * **Simple architectural design:** As opposed to other workflow management system, Accio has a simple architecture and should be easier to deploy.
  It basically only need two components to work, an agent (the server) and a client.
  These two components are available as pre-compiled binaries, allowing you to get started quickly.
  * **Resource isolation and scalability:** By relying on existing orchestration management systems such as Mesos or Kubernetes, Accio inherits all their benefits.
  With resource isolation, experiments have a definite set of resources they are allowed to use and will not be allowed to consume more.
  Many experiments can hence smoothly run in parallel.
  Moreover, Accio should handle any workload supported by the underlying orchestrator, making it highly scalable.
  * **Extensible:** Accio is to be extensible. New workflow operators can be easily added and system-based plugins can be used or written to finely tune Accio behavior and integrate it into any existing architecture.

## What next?
If you are completly new to Accio, you should begin with the [getting started guide](getting-started.html), which will get you quickly familiar with Accio.
Already familiar users might be interested in the [user manual](users/index.html).
More specific guides are available to [administrators](admins/index.html) and [developpers](developpers/index.html).
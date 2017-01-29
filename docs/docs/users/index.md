---
layout: docs
nav: docs
section: users
title: Conducting experiments
---

The goal of Accio is to launch scientific experiments.
Please make sure you are familiar with the [concepts being Accio](../concepts.html) that will be used throughout this section.
Experiments are modelled through instantiations of *workflows*, which are called *runs* in Accio terminology. 

To conduct an experiment, you will need to go through the following steps.

  * **[Authoring](authoring.html):**
  The first step is to create workflows, using the DSL provided by Accio, and publish them.
  Then you will be create runs from them. Runs can be described either directly via the CLI or by using another DSL.
  * **[Monitoring](monitoring.html):**
  Once your run has been submitted to Accio, it is useful to track their progress.
  * **[Analysing](anlysing.html):**
  After your run has completed, Accio does not leave alone and gives you various tools to get quick insights over the results.
  Then, you can generate exhaustive CSV reports, ready for further analysis.
  * **[Debugging](debugging.html):**
  Because we do not live in an ideal world, things can sometimes go wrong and your run will not complete successfully.
  Hopefully, Accio gives you tools to understand what went wrong.
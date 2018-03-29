---
title: Best practices
category: User guide
order: 70
---

This page surveys some best practices that have been outlined by our sustained usage of Accio.

## Writing generic and re-usable workflows
It can be sometimes difficult to find the write level of granularity while writing workflows.
On the once hand, it is possible to write unparametrized workflows that are ready to be executed, but cannot be configured at all.
In that case, it is required to create a brand new workflow to change even a single parameter.
On the other hand, workflows can take tens of required parameters, making them rather difficult to understand and launch.
There is obviously a trade-off to find between these two extreme visions.

We recommend using parameters whenever possible, while giving them a default value when it makes sense.
It allows others to easily test different inputs, while not being overwhelmed by the number of parameters.
Of course, it is better for some parameters to not have a default value.
For example, in our experience, the parameter specifying the dataset to use rarely has a default value, because of its impact on the final outputs.

## Version control everything
As workflow and run definitions are both written into plain text files, it is very easy to store them into a version control system.
[Git](https://git-scm.com/) is our SCM (Source Control Management) software of choice, but their are plenty others such as [Subversion](https://subversion.apache.org/) or [Mercurial](https://www.mercurial-scm.org/).
While it is true that an Accio cluster already keeps track of submitted workflows and runs (and even works with different workflow versions), storing them into your own source control tool allows you to share these files between different clusters and even outside your organization.
Moreover, it allows users to collaborate on experiments, test them, before submitting them on a production cluster.
We have found particularly useful to version experiments (both workflows and runs) alongside our research papers.
Ultimately, they can be shared via a link in the published paper, allowing other reads to replay experiments.

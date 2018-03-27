---
layout: contribute
title: Developing the agent
---

## Server

The agent is the central service providing Accio features.
It is built on top of [Finagle](https://twitter.github.io/finagle/) and [Finatra](https://twitter.github.io/finatra/), two great libraries used by Twitter to build their own services.
The agent is a [Thrift service](https://thrift.apache.org/), which gives well-defined interfaces with type-safety when writing clients.
Using Finagle/Finatra gives us a lot of bonuses, such as an integrated admin interface, built-in support for metrics reporting and service discovery.

Source code for the agent is located under the `fr.cnrs.liris.accio.agent` package.
It contains the implementation of each Thrift endpoint, though each of these should not exceed ~100 lines of code.
Indeed, it essentially on accio.framework class to implement features.
It also provide bindings for essential services such as the scheduler or persistent storage.

## Executor

The executor is a component that is not directly invoked by the user, but instead dynamically launched by the scheduler.
As such, it comes with its own binary, that is independently compiled.
In particular, it is the executor which embeds the actual implementation of every operator.
It has one-way communication with the Agent, implemented using a Finagle client and the Thrift agent interface.

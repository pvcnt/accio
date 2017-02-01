---
layout: docs
nav: docs
title: Developping the server components
---

## Agent
The agent is the central service providing Accio features.
It is built on top of [Finagle](https://twitter.github.io/finagle/) and [Finatra](https://twitter.github.io/finatra/), two great libraries used by Twitter to build their own services.
The agent is a [Thrift service](https://thrift.apache.org/), which gives well-defined interfaces with type-safety when writing clients.
Using Finagle/Finatra gives us a lot of bonuses, such as an integrated admin interface, built-in support for metrics reporting and service discovery.

Source code for the agent is located under the `fr.cnrs.liris.accio.agent` package.
It contains the implementation of each Thrift endpoint, though each of these should not exceed ~100 lines of code.
Indeed, it essentially on Accio core class to implement features.
It also provide bindings for essential services such as the scheduler or persistent storage.

## Executor
The executor is a component that is not directly invoked by the user, but instead dynamically launched by the scheduler.
As such, it comes with its own binary, that is independently compiled.
In particular, it is the executor which embeds the actual implementation of every operator.
It has one-way communication with the Agent, implemented using a Finagle client and the Thrift agent interface.

## Gateway
The gateway is an HTTP interface to Accio, providing a REST API and a Web interface.
Source code for the gateway is located under the `fr.cnrs.liris.accio.gateway` package.

Finatra is used for the REST API and Web UI serving.
This time, we use its HTTP version designed to build HTTP servers.
We use a Finagle client to communicate with the agent.
Like the client, the gateway contains very few logic, acting essentially as a translator between HTTP and Thrift protocols.

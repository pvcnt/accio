---
layout: docs
nav: docs
title: Accio system architecture
---

This section provides a system-oriented view over the Accio architecture.
Reading this section should give a understanding the various parts Accio is made of, but is not required for just using it.

## Overview

Here is the big picture about the components of Accio and the way they interact.

![Architecture overview](../images/architecture.png)

The most important component the **Accio agent**.
Agents are processes in charge of handling users' wills.
Accio is designed around a very simple architecture, where there is no master or slave process.
One or several agents can be launched and will answer users' requests.
It is made possible because agents are stateless; persistent state is managed by storage and state manager plugins, with which agents interact.
Agents speak the [Thrift protocol](https://thrift.apache.org/), which provides an IDL to define typed messages and RPC services.
Users use the **Accio client** to communicate with the agent, which is a CLI application.

The **Accio gateway** is a simple server that speak the HTTP protocol.
It basically make the translation between Thrift and HTTP.
For now, it only provides a read-only access to Accio, meaning it is not possible to create runs or workflows through it.
It also optionally comes with a [Web interface](web-ui.html), allowing to browse Accio data in a convenient way.

Finally, an **Accio executor** is a process dynamically launched on a worker node that will actually execute operators.
Executors are responsible for downloading data needed by an operator, processing it according its inputs and then upload produced data.
They communicate with agents to inform them of their progress, which allows to have almost real-time feedback on run execution.

## Plugins

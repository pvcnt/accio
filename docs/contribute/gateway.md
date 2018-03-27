---
layout: contribute
title: Developing the gateway
---

## Server

The gateway is an HTTP interface to Accio, providing a REST API and a Web interface.
Source code for the gateway is located under the `fr.cnrs.liris.accio.gateway` package.

Finatra is used for the REST API and Web UI serving.
This time, we use its HTTP version designed to build HTTP servers.
We use a Finagle client to communicate with the agent.
Like the client, the gateway contains very few logic, acting essentially as a translator between HTTP and Thrift protocols.

## Web interface

We use [Javascript ES6](https://babeljs.io/docs/learn-es2015/), transpiled thanks to Babel and [ReactJS](https://facebook.github.io/react/) to build reactive user interfaces.
React code is compiled into a single Javascript file, that is then served through the server.
It uses directly the REST API to access Accio data.

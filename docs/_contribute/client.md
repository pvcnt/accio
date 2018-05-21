---
layout: contribute
title: Developing the client
---

Source code for the command-line application is stored under the `fr.cnrs.liris.accio.cli` package.
It is really only a client to an Accio client; its only role is to send request to a cluster and present them to the client.
It contains very few logic on its own, besides input/output handling.
It is built around a small framework facilitating the work of creating new commands and parsing command-line arguments.

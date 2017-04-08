---
layout: docs
weight: 20
title: Installing Accio
---

## Client installation
The most common case is that you have an Accio cluster already installed by your system administrator on some machines, that you want to interact with.
In this case, you only need to install a client to communicate with this cluster.
We have installation guides for the following platforms:

  * [Ubuntu](ubuntu.html)
  * [Mac OS](macos.html)
  * [Windows](windows.html)
  * [From sources](sources.html)

**We recommend installing Accio from a binary distribution**, as it is the easiest method and more robust method.
Compiling from source should be reserved for those who want to live on the edge.
These instructions are for installing a client communicating with an existing Accio cluster.
Instructions about how to deploy an Accio cluster can be found [in the appropriate section](../../deploy/architecture/).

## Local Vagrant cluster
If you want to quickly bootstrap a local cluster, or to develop Accio, you can also use the [Vagrant distribution](vagrant.html).
It allows to create a full cluster on a virtual machine, useful to test all features without having a live cluster.
This is however not recommended for production.

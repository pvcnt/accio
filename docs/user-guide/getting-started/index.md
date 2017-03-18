---
layout: docs
weight: 40
title: Getting started
---

## Start your first experiment
Once logged onto the virtual machine, you have access to the Accio client.
Type `accio` and look at the built-in help.

We will use examples provided with Accio to quickly create a first experiment.
Under `etc/examples` you will find examples of workflow and run definitions ready to be launched.

First, push a workflow to the cluster:

```
$ accio push /vagrant/etc/examples/workflow_Geo-I.json
[OK] Pushed workflow: workflow_Geo-I
[OK] Done in 2.54 s.
$ accio get workflows
Id                              Owner            Created          Name
workflow_Geo-I                  John Doe         moments ago      Geo-I nominal workflow
```

Then, create a first run for this workflow and check it is running:

```
$ accio submit workflow_Geo-I url=/vagrant/etc/examples/geolife epsilon=0.01
[OK] Created run: 97db38ca34d248a8b9357e1fdd0ccb89
[OK] Done in 1.808 s.
$ accio get runs
Run id                            Workflow id      Created          Run name         Status
97db38ca34d248a8b9357e1fdd0ccb89  workflow_Geo-I   moments ago      <no name>        Running
```

You can also view your run in the Web interface at [http://192.168.50.4/#/runs](http://192.168.50.4/#/runs).
Once your run is completed, you may be able to visualize its outputs.

---
title: Hello World tutorial
order: 20
---

This tutorial is intended to feature Accio main capabilities.
It assumes that you are working from [a local Vagrant environment](vagrant.html).
Once logged onto the virtual machine, you have access to the Accio client.
Type `accio` and look at the built-in help.

## Launching an experiment

We will use examples provided with Accio to quickly create a first experiment.
Examples of workflow and run definitions ready to be launched can be found `etc/examples`.

First, push a workflow to the cluster:
```bash
accio push /vagrant/etc/examples/workflow_Geo-I.json
```

The output is similar to this:
```
[OK] Pushed workflow: workflow_Geo-I
[OK] Done in 2.54 s.
```

You can verify that the workflow has indeed been created:
```bash
accio get workflows
```

The output is similar to this:
```
Id                              Owner            Created          Name
workflow_Geo-I                  John Doe         moments ago      Geo-I nominal workflow
```

Then, create a first run for this workflow:
```bash
accio submit workflow_Geo-I url=/vagrant/etc/examples/geolife epsilon=0.01
```

The output is similar to this:
```
[OK] Created run: 97db38ca34d248a8b9357e1fdd0ccb89
[OK] Done in 1.808 s.
```

You can now check that the run has indeed been created and is being executed:
```bash
accio get runs
```

The output is similar to this:
```
Run id                            Workflow id      Created          Run name         Status
97db38ca34d248a8b9357e1fdd0ccb89  workflow_Geo-I   moments ago      <no name>        Running
```

If the run does not appear, it may be because it is already completed.
By default completed runs are hidden, but they may be retrieved by adding a flag:
```bash
accio get runs -all
```

## Web interface

Accio also comes with a Web interface giving access to information about workflows and runs.
If you are using the local Vagrant environment, the Web interface should be accessible at [http://192.168.50.4](http://192.168.50.4).

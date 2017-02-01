---
layout: docs
nav: docs
title: Getting started
---

This page will get you started with a local Accio cluster running inside a virtual machine.
Just after, you will be able to quickly test your first commands.

* TOC
{:toc}

## Spin up a local cluster
First, you need to install [Virtualbox](https://www.virtualbox.org/) and [Vagrant](https://www.vagrantup.com/) on your computer.
Vagrant is a very helpful tool to create portable and reproducible development environments.
We configure it to use Virtualbox to manage virtual machines.

The, clone the Git repository of Accio.
Then you can use Vagrant to create a virtual machine running Accio, which will be built from the source.

```
git clone git@github.com:privamov/accio.git
cd accio/
vagrant up
```

When first launching the virtual machine, the process takes some time.
Indeed, Vagrant downloads a base image and provisions it, which includes downloading dependencies needed to build Accio, building various Accio components and finally starting them.

Once the machine has booted, you may be able to access to following web interfaces:

  * Gateway: [http://192.168.50.4](http://192.168.50.4)
  * Gateway admin: [http://192.168.50.4:8880](http://192.168.50.4:8880)
  * Agent admin: [http://192.168.50.4:9990](http://192.168.50.4:9990)

When using Vagrant, you may notice a warning about guest additions not being installed.
This is not crucial, but if you want to get a rid of this error, you can install a plugin that will handle this:

```
vagrant plugin install vagrant-vbguest
```

## Login to the cluster
You can start an SSH session on the previously created virtual machine with the `vagrant ssh` command.
You will be logged in as the `ubuntu` user.
Local sources of Accio as mounted under the `/vagrant` in the virtual machine.

From there, you have access to the Accio client.
Type `accio` and look at the built-in help of available commands.

## Start your first experiment
We will use examples provided with Accio to quickly create a first experiment.
Under `etc/examples` you will find examples of workflow and run definitions ready to be launched.

First, push a workflow to the cluster:

```
$ accio push /vagrant/etc/examples/workflow_Geo-I.json
[OK] Pushed workflow: workflow_Geo-I
[OK] Done in 2.54 s.
$ accio workflows
Id                              Owner            Created          Name
workflow_Geo-I                  John Doe         moments ago      Geo-I nominal workflow
```

Then, create a first run for this workflow and check it is running:

```
$ accio submit workflow_Geo-I url=/vagrant/etc/examples/geolife epsilon=0.01
[OK] Created run: 97db38ca34d248a8b9357e1fdd0ccb89
[OK] Done in 1.808 s.
$ accio ps
Run id                            Workflow id      Created          Run name         Status
97db38ca34d248a8b9357e1fdd0ccb89  workflow_Geo-I   moments ago      <no name>        Running
```

You can also view your run in the Web interface at [http://192.168.50.4/#/runs](http://192.168.50.4/#/runs).

Once your run is completed, you may be able to visualize its outputs.

## Shut down the local cluster
When you are done working with Accio, you can stop the virtual machine with `vagrant halt`.
You can then start it again in the same state with `vagrant up`.

If you want to irreversibly destroy your local cluster, you can user the `vagrant destroy` command which will delete the virtual machine.
However, it should be of no impact on your work, since Accio source remains stored in your personal directory.

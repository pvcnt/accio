---
layout: docs
nav: docs
title: Getting started
---

This page will get you started with a local Accio cluster running inside a virtual machine.
Just after, you will be able to quickly test your first commands.

* TOC
{:toc}

## Install requirements
First, you need to install [Virtualbox](https://www.virtualbox.org/) and [Vagrant](https://www.vagrantup.com/) on your computer.
Vagrant is a very helpful tool to create portable and reproducible development environments.
We configure it to use Virtualbox to manage virtual machines.

## Spin up a local cluster
To create a local cluster, you need to clone the Git repository of Accio.
Then you can use Vagrant to create a virtual machine running Accio.

```bash
git clone git@github.com:privamov/accio.git
cd accio/
vagrant up
```

Please note that when first launching the virtual machine, the process will take some time.
Indeed, Vagrant downloads a base image and then provision it, which includes downloading dependencies needed to build Accio, building various Accio components and finally starting them.

When using Vagrant, you may notice a warning about guest additions not being installed.
This is not crucial, but if you want to get a rid of this error, you can install a plugin that will handle this:

```bash
vagrant plugin install vagrant-vbguest
```
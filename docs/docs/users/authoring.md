---
layout: docs
nav: docs
section: users
title: Authoring experiments
---

## Validating files

Workflow and run files can be validated thanks to the [`accio validate`](../client/validate-command.html) command, which takes one or several files as arguments.
This command will not execute files, but only check there are correct and could be executed without any problem.
In case of an error, it will do its best to provide meaningful information to help you fix errors.
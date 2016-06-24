---
layout: documentation
nav: docs
title: Accio concepts
---

## Introduction

Accio is framework whose main purpose is to study location privacy.
It can also be used as a generic mobility data manipulation tool.
It provides several elements:

  * a data model specifically designed to deal with mobility data;
  * a data processing and evaluation framework;
  * a workflow description format, describing pipelines;
  * an experiment description format, describing how to launch several workflows at once;
  * an execution engine, scheduling and executing the various workflows an experiment is composed of.

Accio comes with two different interfaces: a command line tool and a REST API.

## Data model

Accio supports a simple data model, strictly oriented around mobility data.

**Events.**
An event is the smallest piece of information in our model.
It is a triplet `(user, location, timestamp)` representing the location of a user at a given time.
All elements of this triplet must be defined.
Additionally, named scalar properties can be attached to triplets.

  * `user` is a string uniquely identifying a user inside the studied system.
  * `location` is a precise location, e.g. latitude/longitude or cartesian coordinates.
  * `timestamp` is a precise instant (millisecond precision), without timezone information.
  * `props` is a dictionary with arbitrary strings as keys and scalars as values.

**Traces.**
Events are organised into traces.
A trace is a consistent sequence of chronologically ordered events belonging to the same user.
A trace contains no duplicate event.
Most of the time, it is more practical to manipulate a trace as a whole instead of individual events.
There is often many traces per user, e.g., representing different batches of data being anonymized and/or sent to a server as a whole.
Empty traces may exist.

**Datasets.**
Datasets are collections of elements.
For now, Accio only support datasets of traces.
Elements inside a dataset are ordered, though this meaning may not always be known.
Traces datasets are the elements being manipulated by our data pipelines.

## Operators and pipelines

## Workflows and experiments

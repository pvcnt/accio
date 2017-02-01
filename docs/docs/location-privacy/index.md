---
layout: docs
nav: docs
title: Location privacy
---

Although the Accio framework is agnostic to the underlying operators, the way they are implemented and data they manipulate, Accio has been historically built to study location privacy.
It is visible when we look at the data types supported by the [Accio workflow DSL](workflow-dsl/index.html), which contains several spatio-temporal-related types.
Moreover, Accio is for now only packaged with a operators aiming at studying location privacy.

The standard set of operators encompasses many state-of-the-art algorithms about location privacy, such as Location Privacy Protection Mechanisms (LPPMs), evaluation metrics, as well as generic spatio-temporal data manipulation tools.
Accio has been used and is being used by privacy researchers to help them launch experiments and analyze their results.

Please make sure to take a look at [the library of operators](../operators/index.html) built-in into Accio.

## Data model

We propose a simple data model suited for studying location privacy.

### Events
An event is the smallest piece of information in our model.
It is a triplet `(user, location, timestamp)` representing the location of a user at a given time.
All elements of this triplet must be defined.
Additionally, named scalar properties can be attached to triplets.

  * `user` is a string uniquely identifying a user inside the studied system.
  * `location` is a precise location, e.g. latitude/longitude or cartesian coordinates.
  * `timestamp` is a precise instant (millisecond precision), without timezone information.
  * `props` is a dictionary with arbitrary strings as keys and scalars as values.

### Traces
Events are organised into traces.
A trace is a consistent sequence of chronologically ordered events belonging to the same user.
A trace contains no duplicate event.
Most of the time, it is more practical to manipulate a trace as a whole instead of individual events.
There is often many traces per user, e.g., representing different batches of data being anonymized and/or sent to a server as a whole.
Empty traces may exist.

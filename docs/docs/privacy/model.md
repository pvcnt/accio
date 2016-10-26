---
layout: documentation
nav: docs
title: Location privacy data model
---

Accio supports a simple data model for studying location privacy.

## Events
An event is the smallest piece of information in our model.
It is a triplet `(user, location, timestamp)` representing the location of a user at a given time.
All elements of this triplet must be defined.
Additionally, named scalar properties can be attached to triplets.

  * `user` is a string uniquely identifying a user inside the studied system.
  * `location` is a precise location, e.g. latitude/longitude or cartesian coordinates.
  * `timestamp` is a precise instant (millisecond precision), without timezone information.
  * `props` is a dictionary with arbitrary strings as keys and scalars as values.

## Traces
Events are organised into traces.
A trace is a consistent sequence of chronologically ordered events belonging to the same user.
A trace contains no duplicate event.
Most of the time, it is more practical to manipulate a trace as a whole instead of individual events.
There is often many traces per user, e.g., representing different batches of data being anonymized and/or sent to a server as a whole.
Empty traces may exist.
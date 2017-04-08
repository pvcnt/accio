---
layout: docs
weight: 10
title: Scopes
---

Workflow DSL:

```json
{
  "scopes": [
    {
      "name": "time",
      "column": "time",
      "granularity": "3.hours",
      "history": "4.days"
    }
  ],
  "graph": [
    {
      "op": "EventSource"
    },
    {
      "op": "TemporalSampling",
      "inputs": {
        "data": {
          "reference": "EventSource/data"
        }
      }
    },
    {
      "op": "GeoIndistinguishability",
      "inputs": {
        "data": {
          "reference": "TemporalSampling/data"
        }
      }
    },
    {
      "op": "PoisRetrieval",
      "inputs": {
        "train": {
          "reference": "TemporalSampling/data",
          "enter": ["time"]
        },
        "test": {
          "reference": "GeoIndistinguishability/data",
          "enter": ["time"]
        }
      }
    }
  ]
}
```

Scheduling:

  * Sequential (e.g., temporal) or parallel (e.g., attribute-based)

Export:


---
layout: deploy
title: Monitoring
---

The Accio agent exposes a rich administrative interface, accessible at the address and port specified by the `-admin.port` flag.
It is especially useful for monitoring and debugging purposes.

* TOC
{:toc}

## Metrics

A human-readable metrics endpoint is available under `/admin/metrics`, while a machine-readable metrics endpoint is exposed under `/admin/metrics.json`:
```bash
curl -s localhost:9990/admin/metrics.json | python -mjson.tool | head
```

The output should look similar to this:
```

    "finagle/aperture/coordinate": -1.0,
    "finagle/aperture/peerset_size": -1.0,
    "finagle/build/revision": 580932930000.0,
    "finagle/clientregistry/initialresolution_ms": 1,
    "finagle/clientregistry/size": 0.0,
    "finagle/future_pool/active_tasks": 0.0,
    "finagle/future_pool/completed_tasks": 0.0,
    "finagle/future_pool/pool_size": 0.0,
    "jvm/application_time_millis": 33896.75,
```

## Learn more

More information about the administrative interface and all its features is available [on this page](https://twitter.github.io/twitter-server/Admin.html).

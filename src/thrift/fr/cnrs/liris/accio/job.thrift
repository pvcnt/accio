namespace java fr.cnrs.liris.accio.thrift

include "fr/cnrs/liris/accio/common.thrift"

struct HeatbeatRequest {
  1: required common.JobId jobId;
}

struct JobStreamRequest {
  1: required common.JobId jobId;
  2: required string kind;
  3: required list<string> lines;
}

struct JobProgressRequest {
  1: required common.JobId jobId;
  2: optional string state;
  3: optional double progress;
}

struct JobStartedRequest {
  1: required common.JobId jobId;
}

struct JobCompletedRequest {
  1: required common.JobId jobId;
  2: required bool successful;
  3: required i32 exitCode;
  4: optional common.ExceptionDatum error;
  5: required list<common.ArtifactDatum> artifacts;
  6: required list<common.MetricDatum> metrics;
}

service JobService {
  void heartbeat(1: HeatbeatRequest req);

  void started(1: JobStartedRequest req);

  void completed(1: JobCompletedRequest req);

  void stream(1: JobStreamRequest req);

  void progress(1: JobProgressRequest req);
}
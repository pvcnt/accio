namespace java fr.cnrs.liris.accio.thrift

include "fr/cnrs/liris/accio/common.thrift"

struct ExecuteJobRequest {
  1: required common.JobId jobId;
  2: required string nodeName;
  3: required string op;
  4: required i64 seed;
  5: required map<string, common.ArtifactDatum> inputs;
}

struct KillJobRequest {
  1: optional common.JobId jobId;
  2: optional common.RunId runId;
}

struct KillJobResponse {
  1: required list<common.JobId> jobIds;
}

service ExecutorService {
  void health();

  void execute(1: ExecuteJobRequest req);

  KillJobResponse kill(1: KillJobRequest req);
}
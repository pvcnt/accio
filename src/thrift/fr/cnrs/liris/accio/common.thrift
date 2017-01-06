namespace java fr.cnrs.liris.accio.thrift

typedef string JobId
typedef string RunId

struct ExceptionDatum {
  1: required string kind;
  2: required string message;
  3: required list<string> stacktrace;
}

struct MetricDatum {
  1: required string name;
  2: required double value;
}

struct ArtifactDatum {
  1: required string name;
  2: required string kind;
  3: required string value;
}
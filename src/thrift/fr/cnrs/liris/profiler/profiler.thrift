namespace java fr.cnrs.liris.profiler.thrift

struct AggregatedStats {
  1: required i32 id;
  2: required i32 count;
  3: required i64 duration;
}

struct TaskProfile {
  1: required i64 thread_id;
  2: required i32 task_id;
  3: optional i32 parent_id;
  4: required i64 start_time;
  5: required i64 duration;
  6: required i32 description_index;
  7: required i32 type_index;
  8: required list<AggregatedStats> children;
}

struct StartProfile {
  1: required i64 start_time;
  2: optional string comment;
}

struct EndProfile {
  1: required list<string> descriptions;
}
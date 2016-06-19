namespace java fr.cnrs.liris.accio.core.thrift

/*include "op.thrift"

struct NodeDef {
  // The name given to this operator. Used for naming inputs, logging, visualization, etc.
  // Unique within a single GraphDef. Must match the regexp "[A-Za-z0-9.][A-Za-z0-9_./]*".
  1: required string name;

  // The operation name. There may be custom parameters in attrs.
  2: required string op;

  // Operation-specific graph-construction-time configuration. Note that this should include all
  // attrs defined in the corresponding OpDef, including those with a value matching the default
  // -- this allows the default to change and makes NodeDefs easier to interpret on their own.
  // However, if an attr with a default is not specified in this list, the default will be used.
  // The "names" (keys) must match the regexp "[a-z][a-z0-9_]+" (and one of the names from the
  // corresponding OpDef's attr field). The values must have a type matching the corresponding
  // OpDef attr's type field.
  3: required map<string, op.AttrValue> attrs;

  // Specifies if this node should be executed multiple times, and results aggregated.
  4: required i32 repeat = 1;
}

struct LayerDef {
  1: required list<string> nodes;
  2: optional string name;
}

// Represents the experiment definition.
struct ExperimentDef {
  1: optional string name;
  2: required list<NodeDef> nodes;
  3: optional LayerDef train_source;
  4: required LayerDef test_source;
  5: required set<LayerDef> treatments;
  6: required set<string> metrics;
}*/
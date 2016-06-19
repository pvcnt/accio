namespace java fr.cnrs.liris.accio.core.thrift

struct Value {
  1: optional string string_value;
  2: optional i64 long_value;
  3: optional i32 int_value;
  4: optional double double_value;
  5: optional bool bool_value;
}

struct ParamValue {
  1: required string name;
  2: required string parent;
  3: required Value value;
}

// A parametrized transformation.
struct Transformation {
  1: required list<string> ops;
  2: required list<string> uids;
  3: required list<ParamValue> params;
}

// For describing the attributes.
/*struct AttrDef {
  // A descriptive name for the attribute. Should match the regexp "[a-z][a-z0-9_]+".
  1: required string name;

  // Type of the attribute. We do not support arbitrary types.
  2: required AttrType type;

  // A reasonable default for this attribute if the user does not supply a value.
  3: optional AttrValue default_value;

  // Whether this attribute is mandatory or optional.
  4: required bool mandatory = true;

  // Human-readable description.
  5: optional string description;

  // --- Constraints ---
  // These constraints are only in effect if specified. Default is no constraints.

  // For type == "int", this is a minimum value.  For "list(___)"
  // types, this is the minimum length.
  //5: optional i64 minimum;

  // The set of allowed values.  Has type that is the "list" version
  // of the "type" field above (uses the ..._list, fields of AttrValue).
  // If type == "type" or "list(type)" above, then the type_list field
  // of allowed_values has the set of allowed DataTypes.
  // If type == "string" or "list(string)", then the s_list field has
  // the set of allowed strings.
  //6: optional list<AttrValue> allowed_values;
}

// Defines an operation. A NodeDef in a GraphDef specifies an Op by using the "op" field which
// should match the name of a OpDef.
struct OpDef {
  // Op names starting with an underscore are reserved for internal use.
  // Names should be CamelCase and match the regexp "[A-Z][a-zA-Z0-9_]*".
  1: required string name;

  // Whether this operator has to be trained.
  2: required bool trainable;

  // Number of inputs.
  3: required i32 num_inputs;

  // Number of outputs.
  4: required i32 num_outputs;

  // Description of the graph-construction-time configuration of this Op.
  5: list<AttrDef> attrs;

  // One-line human-readable description of what the Op does.
  6: optional string short_description;

  // Additional, longer human-readable description of what the Op does.
  7: optional string description;

  // Whether this operator is unstable, i.e., non-deterministic.
  8: required bool unstable;
}*/
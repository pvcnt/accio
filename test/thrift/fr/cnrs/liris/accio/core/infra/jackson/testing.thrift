namespace java fr.cnrs.liris.accio.core.infra.jackson

enum TestEnum {
  FOO,
  BAR,
  FOOBAR,
}

struct NestedStruct {
  1: required string s;
  2: optional i32 i;
}

union TestUnion {
  1: string s;
  2: NestedStruct n;
  3: TestEnum e;
}

struct BigStruct {
  1: required string s;
  4: optional i32 i2;
  5: required i64 l;
  7: required double d;
  8: required TestEnum en;
  10: optional bool b2;
  11: optional NestedStruct n;
  12: required list<NestedStruct> nl;
}

struct StructWithStrings {
  1: required string one;
  2: optional string two;
}

struct StructWithBooleans {
  1: required bool one;
  2: optional bool two;
}

struct StructWithBytes {
  1: required byte one;
  2: optional byte two;
}

struct StructWithShorts {
  1: required i16 one;
  2: optional i16 two;
}
struct StructWithInts {
  1: required i32 one;
  2: optional i32 two;
}

struct StructWithLongs {
  1: required i64 one;
  2: optional i64 two;
}

struct StructWithDoubles {
  1: required double one;
  2: optional double two;
}

struct StructWithStructs {
  1: required NestedStruct one;
  2: optional NestedStruct two;
}

struct StructWithLists {
  1: required list<i32> one;
  2: optional list<i32> two;
}

struct StructWithSets {
  1: required set<i32> one;
  2: optional set<i32> two;
}
struct StructWithMaps {
  1: required map<string, i32> one_string;
  2: optional map<string, i32> two_string;
  3: required map<byte, i32> one_byte;
  4: optional map<byte, i32> two_byte;
  5: required map<i16, i32> one_short;
  6: optional map<i16, i32> two_short;
  7: required map<i32, i32> one_int;
  8: optional map<i32, i32> two_int;
  9: required map<i64, i32> one_long;
  10: optional map<i64, i32> two_long;
  11: required map<double, i32> one_double;
  12: optional map<double, i32> two_double;
  13: required map<bool, i32> one_bool;
  14: optional map<bool, i32> two_bool;
}
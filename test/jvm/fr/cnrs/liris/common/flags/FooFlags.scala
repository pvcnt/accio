package fr.cnrs.liris.common.flags

case class FooFlags(
  @Flag(name = "foo", category = "one")
  foo: String = "defaultFoo",
  @Flag(name = "bar", category = "two")
  bar: Int = 42,
  @Flag(name = "nodoc", category = "undocumented")
  nodoc: String = "")

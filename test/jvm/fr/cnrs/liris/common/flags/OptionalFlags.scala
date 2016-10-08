package fr.cnrs.liris.common.flags

case class OptionalFlags(
  @Flag(name = "foo") foo: Option[Int],
  @Flag(name = "bar") bar: Option[String],
  @Flag(name = "baz") baz: Option[String] = Some("bazbaz"))

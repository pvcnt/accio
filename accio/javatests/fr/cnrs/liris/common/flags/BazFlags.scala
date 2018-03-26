package fr.cnrs.liris.common.flags

case class BazFlags(
  @Flag(name = "baz", category = "one")
  baz: String = "defaultBaz")

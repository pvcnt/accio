package fr.cnrs.liris.accio.core.framework

import fr.cnrs.liris.common.reflect.ScalaType
import fr.cnrs.liris.common.util.Distance
import org.joda.time.{Duration, Instant}

sealed trait ParamType

object ParamType {

  case object Integer extends ParamType

  case object Long extends ParamType

  case object Double extends ParamType

  case object String extends ParamType

  case object StringList extends ParamType

  case object Boolean extends ParamType

  case object Duration extends ParamType

  case object Distance extends ParamType

  case object Timestamp extends ParamType

  def apply(tpe: ScalaType): ParamType = tpe match {
    case t if t.runtimeClass == classOf[Int] => Integer
    case t if t.runtimeClass == classOf[Long] => Long
    case t if t.runtimeClass == classOf[Double] => Double
    case t if t.runtimeClass == classOf[String] => String
    case t if t.runtimeClass == classOf[Boolean] => Boolean
    case t if t.runtimeClass == classOf[Duration] => Duration
    case t if t.runtimeClass == classOf[Instant] => Timestamp
    case t if t.runtimeClass == classOf[Distance] => Distance
    case t if classOf[Seq[_]].isAssignableFrom(t.runtimeClass) && t.typeArguments.head.runtimeClass == classOf[String] => StringList
    case _ => throw new IllegalArgumentException(s"Invalid parameter type $tpe")
  }
}

case class ParamDef(name: String, typ: ParamType, help: Option[String], defaultValue: Option[Any], optional: Boolean)

case class InputDef(name: String, help: Option[String])

case class OutputDef(name: String, `type`: String, help: Option[String])

case class OperatorDef(
  name: String,
  params: Seq[ParamDef],
  inputs: Seq[InputDef],
  outputs: Seq[OutputDef],
  help: Option[String],
  description: Option[String],
  category: String,
  ephemeral: Boolean,
  unstable: Boolean)

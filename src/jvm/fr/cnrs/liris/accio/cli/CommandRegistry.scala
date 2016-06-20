package fr.cnrs.liris.accio.cli

import com.google.inject.Singleton
import fr.cnrs.liris.accio.cli.commands.AccioCommand
import fr.cnrs.liris.common.reflect.ReflectUtils

import scala.collection.mutable
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

case class CommandDef(name: String, help: Option[String], description: Option[String], allowResidue: Boolean, hidden: Boolean)

case class CommandMeta(defn: CommandDef, clazz: Class[AccioCommand[_]], flagsTypes: Seq[Type])

@Singleton
class CommandRegistry {
  private[this] val _commands = mutable.Map.empty[String, CommandMeta]

  def register[T <: AccioCommand[_] : TypeTag : ClassTag]: CommandMeta = {
    val tpe = implicitly[TypeTag[T]].tpe
    val clazz = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[AccioCommand[_]]]
    val annotations = clazz.getAnnotations
    require(ReflectUtils.isAnnotated[Command](annotations), "Commands must be annotated with @Command")
    val command = ReflectUtils.annotation[Command](annotations)
    val defn = CommandDef(command.name, maybe(command.help), maybe(command.description), command.allowResidue, command.hidden)
    require(!_commands.contains(defn.name), s"Duplicate command name '${defn.name}'")

    val rawFlagsType = tpe.baseType(typeOf[AccioCommand[_]].typeSymbol).typeArgs.head
    val flagsTypes = if (rawFlagsType =:= typeOf[Unit]) {
      // No flag defined.
      Seq.empty[Type]
    } else if (rawFlagsType <:< typeOf[Product] && rawFlagsType.typeArgs.nonEmpty) {
      // It should be an N-tuple (and not a case class).
      rawFlagsType.typeArgs
    } else {
      Seq(rawFlagsType)
    }

    val meta = CommandMeta(defn, clazz, flagsTypes)
    _commands(defn.name) = meta
    meta
  }

  def commands: Seq[CommandMeta] = _commands.values.toSeq.sortBy(_.defn.name)

  def get(name: String): Option[CommandMeta] = _commands.get(name)

  def contains(name: String): Boolean = _commands.contains(name)

  def apply(name: String): CommandMeta = _commands(name)

  private def maybe(str: String) = if (str.isEmpty) None else Some(str)
}
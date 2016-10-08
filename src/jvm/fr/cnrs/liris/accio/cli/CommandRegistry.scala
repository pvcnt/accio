package fr.cnrs.liris.accio.cli

import com.google.inject.Singleton
import fr.cnrs.liris.common.reflect.Annotations

import scala.collection.mutable
import scala.reflect.ClassTag

case class CommandMeta(defn: Command, clazz: Class[_ <: AccioCommand])

@Singleton
class CommandRegistry {
  private[this] val _commands = mutable.Map.empty[String, CommandMeta]

  def register[T <: AccioCommand : ClassTag]: CommandMeta =
    register(implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]])

  def register[T <: AccioCommand](clazz: Class[T]): CommandMeta = {
    val annotations = clazz.getAnnotations
    require(Annotations.exists[Command](annotations), "Commands must be annotated with @Command")
    val defn = Annotations.find[Command](annotations).get
    val meta = CommandMeta(defn, clazz)
    require(!_commands.contains(defn.name), s"Duplicate command name '${defn.name}'")
    _commands(defn.name) = meta
    meta
  }

  def commands: Seq[CommandMeta] = _commands.values.toSeq.sortBy(_.defn.name)

  def get(name: String): Option[CommandMeta] = _commands.get(name)

  def contains(name: String): Boolean = _commands.contains(name)

  def apply(name: String): CommandMeta = _commands(name)
}
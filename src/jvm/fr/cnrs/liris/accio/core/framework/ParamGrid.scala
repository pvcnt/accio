package fr.cnrs.liris.accio.core.framework

import com.google.common.base.MoreObjects
import fr.cnrs.liris.common.random.RandomUtils

import scala.reflect.ClassTag

class ParamGrid(grid: Map[String, Array[Any]]) {
  def contains(name: String): Boolean = grid.contains(name)

  def explore[T: ClassTag](name: String, domain: TraversableOnce[T]): ParamGrid =
    new ParamGrid(grid + (name -> (grid.getOrElse(name, Array.empty[T]) ++ domain.toArray[T])))

  def apply[T: ClassTag](name: String): Array[T] =
    grid.get(name).map(_.asInstanceOf[Array[T]]).getOrElse(Array.empty[T])

  def baseOn(paramMap: ParamMap): ParamGrid = baseOn(paramMap.toSeq: _*)

  def baseOn(params: (String, Any)*): ParamGrid =
    new ParamGrid(grid ++ params.map { case (name, value) => name -> Array(value) })

  def filter(prefix: String): ParamGrid = {
    val fullPrefix = s"${prefix.stripSuffix("/")}/"
    val newGrid = grid
        .filter { case (name, _) => name.startsWith(fullPrefix) }
        .map { case (name, domain) => name.drop(fullPrefix.length) -> domain }
    new ParamGrid(newGrid)
  }

  def filter(keys: Set[String]): ParamGrid =
    new ParamGrid(grid.filter { case (key, _) => keys.contains(key) })

  /**
   * Draw a random parameters map from this parameters space.
   *
   * @return A new random parameters map
   */
  def random(): ParamMap = {
    val params = grid.map { case (param, domain) =>
      param -> RandomUtils.randomElement(domain)
    }
    new ParamMap(params)
  }

  def keys: Set[String] = grid.keySet

  def toSeq: Seq[ParamMap] = {
    var paramMaps = Seq(new ParamMap(Map.empty))
    grid.foreach { case (param, domain) =>
      paramMaps = domain.flatMap(v => paramMaps.map(_.set(param -> v)))
    }
    paramMaps
  }

  override def toString: String = {
    MoreObjects.toStringHelper(this)
        .addValue(grid.map { case (name, values) => s"$name(${values.length})" }.mkString(", "))
        .toString
  }
}

object ParamGrid {
  def empty: ParamGrid = new ParamGrid(Map.empty)
}
package fr.cnrs.liris.accio.core.framework

class ParamMap(protected val map: Map[String, Any]) {
  /**
   * Return the number of parameters for which we have a value.
   */
  def size: Int = map.size

  /**
   * Check whether this map is empty.
   */
  def isEmpty: Boolean = map.isEmpty

  def keys: Set[String] = map.keySet

  /**
   * Check whether this map is not empty.
   */
  def nonEmpty: Boolean = map.nonEmpty

  /**
   * Check whether a value has been set for a given parameter.
   *
   * @param name A parameter name
   * @return True if there is a value for this parameter, false otherwise
   */
  def contains(name: String): Boolean = map.contains(name)

  /**
   * Return the value of a given parameter if it has been defined.
   *
   * @param name A parameter name
   * @tparam T Parameter type
   */
  def get[T](name: String): Option[T] = map.get(name).map(_.asInstanceOf[T])

  /**
   * Return the value of a given parameter if it has been defined, or a default value.
   *
   * @param name    A parameter name
   * @param default A default value
   * @tparam T Parameter type
   */
  def getOrElse[T](name: String, default: T): T = get[T](name).getOrElse(default)

  /**
   * Return the value of a given parameter.
   *
   * @param name A parameter name
   * @throws NoSuchElementException If no parameter with this name has been defined
   */
  @throws[NoSuchElementException]
  def apply(name: String): Any = map(name)

  def set(values: (String, Any)*): ParamMap = new ParamMap(this.map ++ values)

  def ++(other: ParamMap): ParamMap = new ParamMap(this.map ++ other.map)

  def filter(prefix: String): ParamMap = {
    val fullPrefix = s"${prefix.stripSuffix("/")}/"
    val newValues = map
        .filter { case (name, _) => name.startsWith(fullPrefix) }
        .map { case (name, value) => name.drop(fullPrefix.length) -> value }
    new ParamMap(newValues)
  }

  def filter(keys: Set[String]): ParamMap =
    new ParamMap(map.filter { case (key, _) => keys.contains(key) })

  /**
   * Return this parameters map viewed as a sequence of parameters and values.
   */
  def toSeq: Seq[(String, Any)] = map.toSeq.sortBy(_._1)

  /**
   * Return this parameters map viewed as a Scala map between parameters and values.
   */
  def toMap: Map[String, Any] = map

  override def toString: String = map.map { case (name, value) => s"$name=$value" }.mkString(", ")
}

/**
 * Factory for [[ParamMap]].
 */
object ParamMap {
  /**
   * Create an empty parameters map.
   */
  def empty: ParamMap = new ParamMap(Map.empty)
}
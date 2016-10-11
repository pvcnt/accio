package fr.cnrs.liris.accio.core.param

/**
 * Encapsulate features for objects parameterizable through their construct arguments.
 *
 * Implementations must be case classes, where constructor arguments can be annotated with
 * [[Param]] to indicate parameters. Non-annotated arguments are allowed but will be ignored.
 */
trait Parameterizable {
  /**
   * Return the parameters and their values for this operator instance. You don't have access to
   * other constructor arguments this way, only parameters.
   *
   * @return A parameters map
   */
  /*lazy val params: ParamMap = {
    println(typeOf[this.type])
    val refl = ReflectCaseClass.of(typeOf[this.type])
    val values = refl.fields.filter(_.isAnnotated[Param]).map(field => field.name -> field.get(this))
    new ParamMap(values.toMap)
  }*/

  /**
   * Return a new operator of the same type with new values for parameters. Not all parameters
   * have to be defined, current values will be used otherwise. Other constructor arguments cannot
   * be set this way, only parameters can.
   *
   * @param paramMap A parameters map
   */
  /*def set(paramMap: ParamMap): this.type = {
    if (paramMap.isEmpty) {
      this
    } else {
      val refl = ReflectCaseClass.of(typeOf[this.type])
      val ctorArgs = refl.fields.map { field =>
        if (field.isAnnotated[Param]) {
          paramMap.get(field.name).getOrElse(field.get(this))
        } else {
          field.get(this)
        }
      }
      refl.newInstance(ctorArgs).asInstanceOf[this.type]
    }
  }*/

  //override def toString: String = s"${getClass.getSimpleName}($params)"
}
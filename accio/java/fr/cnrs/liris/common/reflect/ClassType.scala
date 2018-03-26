package fr.cnrs.liris.common.reflect

import scala.tools.scalap.scalax.rules.scalasig.{ClassInfoType, Symbol, Type, TypeRefType}

class ClassType(symbol: Symbol, typeRefs: Seq[Type]) {
  val runtimeClass: Class[_] = CaseClassSigParser.loadClass(symbol.path)

  def typeArgs: Seq[ScalaType] = typeRefs.flatMap {
    case typeRefType: TypeRefType => Some(ScalaType(typeRefType))
    case _ => None
  }

  def baseClass(clazz: Class[_]): ClassType = {
    val baseType = typeRefs.find {
      case TypeRefType(_, s, typeArgs) => s.path == clazz.getName
      case _ => false
    }.get
    baseType match {
      case TypeRefType(_, s, typeArgs) => new ClassType(s, typeArgs)
    }
  }
}

private[reflect] object ClassType {
  def parse(clazz: Class[_]): ClassType = {
    CaseClassSigParser.findSym(clazz).infoType match {
      case ClassInfoType(symbol, typeRefs) => new ClassType(symbol, typeRefs)
    }
  }
}
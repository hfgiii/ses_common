package org.hfgiii.ses.common.macros

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

trait Mappable[T] {
  def toMap(t: T): Map[String, Any]
  def fromMap(map: Map[String, Any]): T
}

object Mappable {
  implicit def materializeMappable[T]: Mappable[T] = macro materializeMappableImpl[T]

  def materializeMappableImpl[T: c.WeakTypeTag](c: Context): c.Expr[Mappable[T]] = {
    import c.universe._
    val tpe = weakTypeOf[T]
    val tsym = tpe.typeSymbol
    val companion  = tsym.companion

    val fields = tpe.decls.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor ⇒ m
    }.get.paramLists.head


    val (toMapParams, fromMapParams) = fields.map { field ⇒
      val name = field.name.toTermName
      val decoded = name.decodedName.toString
      val returnType = tpe.decl(name).typeSignature
      val retStr     = returnType.toString  //Hack to force the load case class children

      if(returnType.typeSymbol.asClass.isCaseClass)
        (q"$decoded → implicitly[Mappable[$returnType]].toMap(t.$name)", q"implicitly[Mappable[$returnType]].fromMap(map($decoded).asInstanceOf[Map[String,Any]])")
      else
        (q"$decoded → t.$name", q"map($decoded).asInstanceOf[$returnType]")
    }.unzip

    c.Expr[Mappable[T]] { q"""
      new Mappable[$tpe] {
        def toMap(t: $tpe): Map[String, Any] = Map(..$toMapParams)
        def fromMap(map: Map[String, Any]): $tpe = $companion(..$fromMapParams)
      }
    """ }
  }
}


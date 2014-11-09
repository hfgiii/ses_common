package org.hfgiii.ses.common.macros

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

object SesMacros {


  trait Mappable[T] {
    def toMap(t: T, keyMapper: String => String): Map[String, Any]
    def fromMap(map: Map[String, Any],keyMapper: String => String): T
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
        val retStr     = returnType.toString  //Hack to force to load case class children

        if(returnType.typeSymbol.asClass.isCaseClass)
          (q"$decoded → implicitly[Mappable[$returnType]].toMap(t.$name, keyMapper)", q"implicitly[Mappable[$returnType]].fromMap(map($decoded).asInstanceOf[Map[String,Any]],keyMapper)")
        else
          (q"keyMapper($decoded) → t.$name", q"map(keyMapper($decoded)).asInstanceOf[$returnType]")
      }.unzip

      c.Expr[Mappable[T]] { q"""
      new Mappable[$tpe] {
        def toMap(t: $tpe, keyMapper: String => String): Map[String, Any] = Map(..$toMapParams)
        def fromMap(map: Map[String, Any],keyMapper: String => String): $tpe = $companion(..$fromMapParams)
      }
    """ }
    }
  }
}

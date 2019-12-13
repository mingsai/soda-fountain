package com.socrata.soda.server.macros

import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context

import com.rojoma.json.v3.util.SimpleHierarchyCodecBuilder

object BranchCodec {
  // $COVERAGE-OFF$ Disabling coverage because macros are not supported.
  def apply[T <: AnyRef](codecBuilder: SimpleHierarchyCodecBuilder[T]): SimpleHierarchyCodecBuilder[T] =
    macro impl[T]
  // $COVERAGE-ON$

  def impl[T <: AnyRef : c.WeakTypeTag](c: Context)(codecBuilder: c.Expr[SimpleHierarchyCodecBuilder[T]]): c.Expr[SimpleHierarchyCodecBuilder[T]] = {
    import c.universe._

    def isType(t: Type, w: Type) =
      // There HAS to be a better way to do this.
      // t MAY be <error>.  w must not be!
      // since <error> =:= any type, reject if it looks "impossible".
      t =:= w && !(t =:= typeOf[String] && t =:= typeOf[Map[_,_]])

    def tag(thing: Symbol): String = {
      thing.annotations.reverse.find { ann => isType(ann.tree.tpe, typeOf[Tag]) } match {
        case Some(ann) =>
          ann.tree.children.tail.collect {
            case AssignOrNamedArg(Ident(n), Literal(Constant(arg: String))) if n.toString == "value" => arg
          }.headOption.getOrElse {
            c.abort(thing.pos, "No value for " + thing.name)
          }
        case None =>
          c.abort(thing.pos, "No tag annotation for " + thing.name)
      }
    }

    val classes = weakTypeOf[T].typeSymbol.asClass.knownDirectSubclasses
    if(classes.isEmpty) c.abort(c.enclosingPosition, "No known subclasses of " + weakTypeOf[T] + "; did you forget \"sealed\"?")
    val codecBuilderBuilt = classes.foldLeft(codecBuilder.tree) { (expr, cls) =>
      q"$expr.branch[$cls](${tag(cls)})(_root_.com.rojoma.json.v3.util.AutomaticJsonEncodeBuilder[$cls], _root_.com.rojoma.json.v3.util.AutomaticJsonDecodeBuilder[$cls], _root_.scala.Predef.implicitly)"
    }

    c.Expr[SimpleHierarchyCodecBuilder[T]](codecBuilderBuilt)
  }
}

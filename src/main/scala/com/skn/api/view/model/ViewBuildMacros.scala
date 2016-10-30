package com.skn.api.view.model

import com.skn.api.view.jsonapi.JsonApiPlayModel.ObjectKey
import org.slf4j.{Logger, LoggerFactory}

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
  *
  * Created by Sergey on 04.10.2016.
  */
case class TestLinked(name: String, id: Option[Long]) extends ViewItem { val key = ObjectKey("testLinked", id) }

case class Test(stV: String, numV: BigDecimal, another: TestLinked, id: Option[Long]) extends ViewItem { val key = ObjectKey("test", id)}

object ViewBuildMacros
{
  val logger: Logger = LoggerFactory.getLogger(ViewBuildMacros.getClass)

    def build(view: Test) = macro buildMacros[Test]
  def buildMacros[T <: ViewItem](context: blackbox.Context)(view: context.Expr[T]): context.Expr[String] =
  {
    import context.universe._
    def isAssignableFrom(baseClasses: List[context.universe.Symbol], typeSymbol: Symbol) = baseClasses.exists(symbol => symbol.equals(typeSymbol))

    val values = view.actualType.members.filter(member => member.isTerm).map(_.asTerm).filter(_.isVal)
    values.filter(member => isAssignableFrom(member.typeSignature.baseClasses, typeOf[ViewItem].typeSymbol))
      .map { member =>  }

    var str = view.mirror.RootClass+" : \n"
    for(value <- values) yield
      str+="Val: name=" + value.name.toString + ", type=" + value.info.resultType + ", test=" +
        value.info.baseClasses.headOption.map(sym => sym.toString).getOrElse("") + ", exists("+typeOf[ViewItem].typeSymbol.toString+")=" +
        value.typeSignature.baseClasses.map(bc => bc.asType).find(bc => bc.equals(typeOf[ViewItem].typeSymbol)) +
        value.typeSignature.baseClasses.map(m => m.asType.toString).reduceOption((s1, s2) => s1+":"+s2) + //info.find(t => t.equals(typeOf[ViewItem])).map(_.toString) +
        ", eq(ViewItem)="+value.info.resultType.equals(typeOf[ViewItem].typeSymbol) + "\n"

    val res = view.actualType.paramLists.map(list => list.map(field => field.name.toString).reduceOption((field1, field2) => field1+", "+field2))

    for(elt <- res) yield str = str+elt.getOrElse("")
    str+=view.actualType.members.filter(m => m.isTerm && m.asTerm.isVal).map(m => m.fullName + "-" + m.typeSignature.toString).reduceOption((n1, n2) => n1 + ", " + n2)
    context.Expr(Literal(Constant(str)))
  }
}

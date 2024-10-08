package spectacular

import scala.quoted.*

import anticipation.*

object Spectacular:
  def enumerable[EnumType <: reflect.Enum: Type](using Quotes): Expr[EnumType is Enumerable] =
    import quotes.reflect.*

    val companion = Ref(TypeRepr.of[EnumType].typeSymbol.companionModule).asExpr

    '{
        new Enumerable:
          type Self = EnumType
          val name: Text = ${Expr(TypeRepr.of[EnumType].show)}.tt
          val values: IArray[EnumType] =
            ${  (companion: @unchecked) match
                  case '{ $companion: companionType } =>
                    val ref = TypeRepr.of[companionType].typeSymbol.declaredMethod("values").head
                    companion.asTerm.select(ref).asExprOf[Array[EnumType]]
            }.asInstanceOf[IArray[EnumType]]
    }

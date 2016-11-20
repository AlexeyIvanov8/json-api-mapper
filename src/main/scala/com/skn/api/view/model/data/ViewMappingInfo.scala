package com.skn.api.view.model.data

import com.skn.api.view.jsonapi.JsonApiPlayModel.ObjectKey
import com.skn.api.view.model.{ViewLink, ViewValue}

import scala.reflect.runtime.{universe => ru}

/**
  * Common used tools for read and write view
  * Created by Sergey on 30.10.2016.
  */
object ViewMappingInfo {
  val OptionType = ru.typeOf[Option[_]]
  val ViewLinkType = ru.typeOf[ViewLink[_]]
  val ViewValueType = ru.typeOf[ViewValue]
  val SeqType = ru.typeOf[Seq[_]]
  val ObjectKeyType = ru.typeOf[ObjectKey]

  private def getSymbolDesc(mirror: ru.Mirror, symbol: ru.Symbol, tpe: ru.Type): FieldDesc = {
    var resType = tpe
    val isOption = resType <:< ViewMappingInfo.OptionType
    if (isOption)
      resType = resType.typeArgs.head
    val isSeq = resType <:< ViewMappingInfo.SeqType
    if (isSeq)
      resType = resType.typeArgs.head
    resType match {
      case t if t <:< ViewMappingInfo.ViewLinkType => LinkFieldDesc(
        isOption = isOption, isSeq = isSeq, symbol, resType)
      case t if t <:< ViewMappingInfo.ViewValueType => ValueFieldDesc(
        isOption = isOption, isSeq = isSeq, symbol, resType)
      case t if t =:= ObjectKeyType => KeyFieldDesc(
        isOption = isOption, isSeq = false, symbol, resType)
      case _ => AttributeFieldDesc(isOption, isSeq, symbol, resType, mirror.runtimeClass(resType))
    }
  }

  def getTypeSymbolDesc(mirror: ru.Mirror, symbol: ru.TypeSymbol): FieldDesc = {
    getSymbolDesc(mirror, symbol, symbol.toType)
  }

  def getTermSymbolDesc(mirror: ru.Mirror, symbol: ru.TermSymbol): FieldDesc = {
    getSymbolDesc(mirror, symbol, symbol.typeSignature)
  }
}

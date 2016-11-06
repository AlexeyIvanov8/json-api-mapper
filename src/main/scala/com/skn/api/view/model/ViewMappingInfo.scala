package com.skn.api.view.model

import com.skn.api.view.jsonapi.JsonApiPlayModel.ObjectKey
import com.skn.api.view.model.data.{AttributeFieldDesc, FieldDesc, LinkFieldDesc}

import scala.reflect.runtime.{universe => ru}

/**
  * Created by Sergey on 30.10.2016.
  */
object ViewMappingInfo {
  val OptionType = ru.typeOf[Option[_]]
  val ViewLinkType = ru.typeOf[ViewLink[_]]
  val ViewValueType = ru.typeOf[ViewValue]
  val SeqType = ru.typeOf[Seq[_]]
  val ObjectKeyType = ru.typeOf[ObjectKey]

  def getFieldDesc(field: ru.TermSymbol, fieldMirror: ru.FieldMirror): FieldDesc = {
    var aType = field.typeSignature
    val isOption = aType <:< ViewMappingInfo.OptionType
    if (isOption)
      aType = aType.typeArgs.head
    val isSeq = aType <:< ViewMappingInfo.SeqType
    if (isSeq)
      aType = aType.typeArgs.head
    aType match {
      case t if t <:< ViewMappingInfo.ViewLinkType => LinkFieldDesc(
        isOption = isOption, isSeq = isSeq, fieldMirror, field.typeSignature)
      case _ => AttributeFieldDesc(isOption, isSeq, fieldMirror, field.typeSignature)
    }
  }
}

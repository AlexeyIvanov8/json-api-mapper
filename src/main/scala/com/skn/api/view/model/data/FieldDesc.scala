package com.skn.api.view.model.data

import scala.reflect.runtime.{universe => ru}

/**
  * Created by Sergey on 06.11.2016.
  */
sealed trait FieldDesc {
  val isOption: Boolean
  val isSeq: Boolean
  val fieldSymbol: ru.Symbol
  val unpackType: ru.Type
}

case class LinkFieldDesc(isOption: Boolean, isSeq: Boolean, fieldSymbol: ru.Symbol, unpackType: ru.Type) extends FieldDesc
case class AttributeFieldDesc(isOption: Boolean, isSeq: Boolean, fieldSymbol: ru.Symbol, unpackType: ru.Type, unpackClass: Class[_]) extends FieldDesc
case class ValueFieldDesc(isOption: Boolean, isSeq: Boolean, fieldSymbol: ru.Symbol, unpackType: ru.Type) extends FieldDesc

case class MirrorFieldDesc(desc: FieldDesc, mirror: ru.FieldMirror)

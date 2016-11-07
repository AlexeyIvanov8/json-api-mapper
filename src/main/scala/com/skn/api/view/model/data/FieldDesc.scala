package com.skn.api.view.model.data

import scala.reflect.runtime.{universe => ru}

/**
  * Created by Sergey on 06.11.2016.
  */
sealed trait FieldDesc {
  val isOption: Boolean
  val isSeq: Boolean
  val fieldSymbol: ru.TermSymbol
}

case class LinkFieldDesc(isOption: Boolean, isSeq: Boolean, fieldSymbol: ru.TermSymbol) extends FieldDesc
case class AttributeFieldDesc(isOption: Boolean, isSeq: Boolean, fieldSymbol: ru.TermSymbol) extends FieldDesc

case class MirrorFieldDesc(desc: FieldDesc, mirror: ru.FieldMirror)

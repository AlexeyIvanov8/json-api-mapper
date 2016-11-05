package com.skn.api.view.model

import com.skn.api.view.jsonapi.JsonApiPlayModel.ObjectKey

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
}

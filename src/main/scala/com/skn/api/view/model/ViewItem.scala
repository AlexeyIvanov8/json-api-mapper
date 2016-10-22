package com.skn.api.view.model

import com.skn.api.view.jsonapi.JsonApiPalyModel.ObjectKey

/**
  *
  * Created by Sergey on 03.10.2016.
  */
trait ViewItem
{
  val key: ObjectKey
}

/**
  * Trait, that mark object present JsonObject = Map[String, JsValue]
  */
trait ViewObject

/**
  * Trait for create values who can be serialize/deserialize in String
  */
trait ViewValue
{
  override def toString: String = super.toString
  def fromString[T <: ViewValue](str: String): T
}
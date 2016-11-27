package com.skn.api.view.model.mapper

import com.skn.api.view.jsonapi.JsonApiModel.{Link, ObjectKey}

import scala.reflect.runtime.{universe => ru}
/**
  * Created by Sergey on 31.10.2016.
  */
trait LinkDefiner {
  def getCommonLink(key: ObjectKey): Link
  def getLink(key: ObjectKey): Link
}

trait TypeDefiner {
  def getType(reflectionType: ru.Type): String
}

class SimpleLinkDefiner extends LinkDefiner {
  override def getCommonLink(key: ObjectKey) = Link("http://" + key.`type`)
  override def getLink(key: ObjectKey): Link = Link("http://" + key.`type` + "/")
}

class SimpleTypeDefiner extends TypeDefiner {
  override def getType(reflectionType: ru.Type): String = reflectionType.typeSymbol.name.toString
}

package com.skn.api.view.model.mapper

import com.skn.api.view.jsonapi.JsonApiPlayModel.Link

import scala.reflect.runtime.{universe => ru}
/**
  * Created by Sergey on 31.10.2016.
  */
trait LinkDefiner {
  def getLink(itemType: ru.Type): Link
}

class SimpleLinkDefiner extends LinkDefiner {
  override def getLink(itemType: ru.Type): Link = Link("http://" + itemType.typeSymbol.name.toString)
}

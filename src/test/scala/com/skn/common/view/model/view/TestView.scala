package com.skn.common.view.model.view

import java.time.LocalDateTime

import com.skn.api.view.jsonapi.JsonApiPlayModel.ObjectKey
import com.skn.api.view.model.{ViewItem, ViewLink, ViewValue, ViewValueFactory}

/**
  *
  * Created by Sergey on 11.10.2016.
  */
class Home(val name: String) extends ViewValue
{
  override def toString: String = "1"+name
}

object Home extends ViewValueFactory[Home]
{
  override def fromString(str: String): Home = new Home(str.substring(1))
}

case class TestLink(key: ObjectKey, date: Option[LocalDateTime]) extends ViewItem

case class TestView(str: String,
                    num: BigDecimal,
                    home: Home,
                    id: Option[Long],
                    link: Option[ViewLink[TestLink]],
                    custom: Option[CustomObject]) extends ViewItem { val key = ObjectKey("testType", id) }

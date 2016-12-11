package com.skn.common.view

import java.time.LocalDateTime

import com.skn.api.view.jsonapi.JsonApiModel.ObjectKey
import com.skn.api.view.jsonapi.JsonApiValueModel.JsonApiNumber
import com.skn.api.view.model.{ViewItem, ViewLink, ViewValue, ViewValueFactory}

/**
  *
  * Created by Sergey on 11.10.2016.
  */
case class Home(val name: String) extends ViewValue {
  override def toString: String = "1"+name
}

object Home extends ViewValueFactory[Home] {
  override def fromString(str: String): Home = new Home(str.substring(1))
}

case class TestSeq(id: Long,
                   simpleSeq: Seq[TestSimple],
                   optionSeq: Option[Seq[Long]]) extends ViewItem { val key = ObjectKey("test_seq", id) }

case class TestLink(id: Long, date: Option[LocalDateTime]) extends ViewItem { val key = ObjectKey("testLink", id) }

case class TestSimple(key: ObjectKey, name: String, order: Int) extends ViewItem

case class TestView(str: String,
                    num: BigDecimal,
                    home: Home,
                    id: Option[Long],
                    link: Option[ViewLink[TestLink]],
                    custom: Option[CustomObject]) extends ViewItem { val key = ObjectKey("testType", id.map(JsonApiNumber(_))) }

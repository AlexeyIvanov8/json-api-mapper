package com.skn.common.view

import java.time.LocalDateTime

import com.fasterxml.jackson.databind.SerializationFeature
import com.skn.api.view.jsonapi.{JsonApiJacksonFormat, JsonApiMapper}
import com.skn.api.view.jsonapi.JsonApiModel.ObjectKey
import com.skn.api.view.model.ViewLink
import com.skn.api.view.model.mapper.ReadFeatures.AbsentValueAsNull
import com.skn.api.view.model.mapper._
import com.typesafe.scalalogging._
import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.LoggerFactory

class BaseUnitTest extends FlatSpec with Matchers
{
  protected val logger = Logger(LoggerFactory.getLogger(classOf[BaseUnitTest]))
  val mapper = new JsonApiMapper

  def mappers = new {
    val jacksonMapper = JsonApiJacksonFormat.createMapper()
    jacksonMapper.enable(SerializationFeature.INDENT_OUTPUT)
    val viewWriter = new DefaultViewWriter(new SimpleLinkDefiner)
    val viewReader = new DefaultViewReader(Map[ReadFeatures, Boolean](AbsentValueAsNull() -> true))
    val jsonViewWriter = new JsonApiViewWriter(viewWriter, root => jacksonMapper.writeValueAsString(root))
  }

  def data = new {
    val itemName = "Js string value"

    val item = TestView(itemName,
      5, new Home("TH"), Some(0L),
      Some(new ViewLink(TestLink(1L, Some(LocalDateTime.now())))),
      Some(CustomObject(Some("customName"), 34423, Some(List(3.4, 4.5)))))
    val itemData = mappers.viewWriter.write(item)
    val itemDataStr = mappers.jsonViewWriter.write(item)
    val itemWithNull = TestSimple(ObjectKey("fg", 0), null, 1)
  }
}
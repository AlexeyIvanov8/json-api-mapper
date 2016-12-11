package com.skn.common.view

import java.time.LocalDateTime

import com.fasterxml.jackson.databind.SerializationFeature
import com.skn.api.view.jsonapi.{JsonApiJacksonFormat, JsonApiMapper}
import com.skn.api.view.jsonapi.JsonApiModel.{ObjectKey, RootObject}
import com.skn.api.view.model.ViewLink
import com.skn.api.view.model.mapper.ReadFeatures.AbsentValueAsNull
import com.skn.api.view.model.mapper._
import com.typesafe.scalalogging._
import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.LoggerFactory

class BaseUnitTest extends FlatSpec with Matchers
{
  protected val logger = Logger(LoggerFactory.getLogger(classOf[BaseUnitTest]))
  protected val mapper = new JsonApiMapper

  def mappers = new {
    val jacksonMapper = JsonApiJacksonFormat.createMapper()
    jacksonMapper.enable(SerializationFeature.INDENT_OUTPUT)
    val viewWriter = new DefaultViewWriter(new SimpleLinkDefiner)
    val viewReader = new DefaultViewReader(Map[ReadFeatures, Boolean](AbsentValueAsNull() -> true))
    val jsonViewWriter = new JsonApiViewWriter(viewWriter, root => jacksonMapper.writeValueAsString(root))
    val jsonViewReader = new JsonApiViewReader(viewReader, json => jacksonMapper.readValue(json, classOf[RootObject]))
  }

  def data = new {
    val itemName = "Js string value"

    val item = createNewItem()
    val itemData = mappers.viewWriter.write(item)
    val itemDataStr = mappers.jsonViewWriter.write(item)
    val itemWithNull = TestSimple(ObjectKey("fg", 0), null, 1)

    def createNewItem() = TestView(itemName,
      5, new Home("TH"), Some(0L),
      Some(new ViewLink(TestLink(1L, Some(LocalDateTime.now())))),
      Some(CustomObject(Some("customName"), 34423, Some(List(3.4, 4.5)))))
  }
}
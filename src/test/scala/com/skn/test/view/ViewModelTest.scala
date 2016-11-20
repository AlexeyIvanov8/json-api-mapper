package com.skn.test.view

import java.time.LocalDateTime
import java.util.concurrent._

import com.skn.api.view.exception.ParsingException
import com.skn.api.view.model.ViewLink
import com.skn.common.view._
import play.api.libs.json.Json
import com.skn.api.view.jsonapi.JsonApiPlayFormat.dataFormat
import com.skn.api.view.jsonapi.JsonApiModel.{ObjectKey, RootObject}
import com.skn.api.view.model.mapper._

import scala.collection.immutable.Stream.Empty

class ViewModelTest extends BaseUnitTest
{

  "A ViewItem" should "be serialized automatically" in {
    val executorService: ExecutorService = new ThreadPoolExecutor(4, 4,
      1000, TimeUnit.MILLISECONDS,
      new LinkedBlockingQueue[Runnable]())

    val viewMapper = new DefaultViewWriter(new SimpleLinkDefiner)
    val newViewMapper = mappers.viewReader//new DefaultViewReader

    val view = data.item

    val serialized = viewMapper.write(view)
    val deserialized = newViewMapper.read[TestView](serialized)

    logger.info("Before = " + view.toString)
    logger.info("Serialized = " + Json.toJson(serialized)(dataFormat))
    logger.info("Deserialized = " + deserialized)

    serialized.attributes.get("str").as[String] should be (view.str)
    serialized.attributes.get("num").as[BigDecimal] should be (view.num)

    deserialized.str should be (view.str)
    val deCustom = deserialized.custom.get
    deCustom.name should be (view.custom.get.name)
    deCustom.prices shouldEqual view.custom.get.prices
    deserialized.link shouldBe defined
    val deLink = deserialized.link.get
    deLink.key.`type` should be (view.link.get.key.`type`)
    deLink.key.id.get should be (view.link.get.key.id.get)
    deserialized.key should be (view.key)
    logger.info("deser key = " + deserialized.key)
  }

  "A JsonapiViewWriter" should "serialize ViewItem to String" in {
    val str = mappers.jsonViewWriter.write(data.item)
    logger.info("Current str: "+str)
    val root = mappers.jacksonMapper.readValue(str, classOf[RootObject])
    root.data shouldBe defined
    val dataSeq = root.data.get
    dataSeq should have size 1
    dataSeq.head.key shouldEqual data.item.key
    logger.info("data attrs = " + dataSeq.head.attributes)
    dataSeq.head.attributes.get("str").as[String] shouldEqual data.itemName
  }

  "Absent values" should "be deserialized to null if absent feature enabled" in {
    val res = mappers.viewWriter.write(data.itemWithNull)
    val testAfter = mappers.viewReader.read[TestSimple](res)
    testAfter.name shouldBe null
  }

  "Absent values" should "produce error, if absent feature disabled" in {
    val viewReaderAbsent = new DefaultViewReader
    val res = mappers.viewWriter.write(data.itemWithNull)
    an [ParsingException] should be thrownBy viewReaderAbsent.read[TestSimple](res)
  }
}

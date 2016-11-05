package com.skn.test.view

import java.time.LocalDateTime
import java.util.concurrent._

import com.skn.api.view.model.{SimpleLinkDefiner, ViewLink, ViewReader, ViewWriter}
import com.skn.common.view.BaseUnitTest
import com.skn.common.view.model.view.{CustomObject, Home, TestLink, TestView}
import play.api.libs.json.Json
import com.skn.api.view.jsonapi.JsonApiPlayFormat.dataFormat
import com.skn.api.view.jsonapi.JsonApiPlayModel.ObjectKey

class ViewModelTest extends BaseUnitTest
{

  "A ViewItem" should "be serialized automatically" in
  {
    val executorService: ExecutorService = new ThreadPoolExecutor(4, 4,
      1000, TimeUnit.MILLISECONDS,
      new LinkedBlockingQueue[Runnable]())

    val viewMapper = new ViewWriter(new SimpleLinkDefiner)
    val newViewMapper = new ViewReader

    val str = "Js string value"
    val view = TestView(str, 998,
      new Home("TH"),
      Some(0),
      Some(new ViewLink(TestLink(ObjectKey("testLink", 1L), Some(LocalDateTime.now())))),
      Some(CustomObject(Some("customName"), 94, Some(3.4 :: 4.5 :: Nil))))

    val serialized = viewMapper.write(view)
    val deserialized = newViewMapper.read[TestView](serialized)

    logger.info("Serialized = " + Json.toJson(serialized)(dataFormat))
    logger.info("Deserialized = " + deserialized)

    serialized.attributes.get("str").as[String] should be (view.str)
    serialized.attributes.get("num").as[BigDecimal] should be (view.num)

    deserialized.str should be (view.str)
    val deCustom = deserialized.custom.get
    deCustom.name should be (view.custom.get.name)
    deCustom.prices shouldEqual view.custom.get.prices
  }
}

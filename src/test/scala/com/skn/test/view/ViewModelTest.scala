package com.skn.test.view

import java.util.concurrent._

import com.skn.api.view.model.DefaultViewMapper
import com.skn.common.view.BaseUnitTest
import com.skn.common.view.model.view.{CustomObject, TestView}




class ViewModelTest extends BaseUnitTest
{
  def bench(executorService: ExecutorService, threads: Int, iterations: Int, batch: Int): Unit =
  {
    for(k <- 0 to iterations) yield {
      val bTime = System.nanoTime()
      val futures = for (i <- 0 until threads) yield executorService.submit(new Callable[Long] {
        override def call(): Long = {
          var count = 0L
          val viewMapper = new DefaultViewMapper
          for (j <- 0 to batch) yield {
            val item = TestView("Js string value", 998, Some(1),
              Some(CustomObject(Some("customName"), 94, Some(3.4 :: 4.5 :: Nil))))
            val data = viewMapper.toData(item)
            count += data.key.id.getOrElse(0L)
          }
          count
        }
      })
      val res = futures.map(fut => fut.get()).sum
      val timeMs = (System.nanoTime() - bTime) / 1000000
      logger.info(res + " items converted to data in " + (timeMs) + "ms, " + (res.toDouble / timeMs.toDouble) + "ops in ms")
    }
  }

  "A ViewItem" should "be serialized automatically" in
  {
    val executorService: ExecutorService = new ThreadPoolExecutor(4, 4,
      1000, TimeUnit.MILLISECONDS,
      new LinkedBlockingQueue[Runnable]())

    val viewMapper = new DefaultViewMapper

    val str = "Js string value"
    val view = TestView(str, 998, Some(0),
        Some(CustomObject(Some("customName"), 94, Some(3.4 :: 4.5 :: Nil))))

    val serialized = viewMapper.toData(view)
    val deserialized = viewMapper.fromData[TestView](serialized)

    serialized.attributes.get("str").as[String] should be (view.str)
    serialized.attributes.get("num").as[BigDecimal] should be (view.num)

    deserialized.str should be (view.str)
    val deCustom = serialized.attributes.get("custom").asInstanceOf[Option[CustomObject]].get
    deCustom.name should be (view.custom.get.name)
    deCustom.prices shouldEqual view.custom.get.prices
  }
}

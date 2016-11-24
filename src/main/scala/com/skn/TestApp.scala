package com.skn

import java.time.LocalDateTime
import java.util.concurrent._

import com.skn.api.view.jsonapi.JsonApiJacksonFormat
import com.skn.api.view.jsonapi.JsonApiModel.{ObjectKey, RootObject}
import com.skn.api.view.model._
import com.skn.api.view.model.mapper._
import com.skn.common.view.{CustomObject, Home, TestLink, TestView}
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import com.skn.api.view.jsonapi.JsonApiValueModel._

/**
  *
  * Created by Sergey on 18.10.2016.
  */
object TestApp extends App {

  protected val logger = Logger(LoggerFactory.getLogger("App logger"))

  def bench(executorService: ExecutorService, threads: Int, iterations: Int, batch: Int): Unit =
  {
    for(k <- 0 to iterations) yield {
      val bTime = System.nanoTime()
      val futures = for (i <- 0 until threads) yield executorService.submit(new Callable[Long] {
        override def call(): Long = {
          var count = 0L
          val viewMapper = new DefaultViewWriter(new SimpleLinkDefiner)

          val item = TestView("t", 998,
            new Home("TH"),
            Some(1),
            Some(new ViewLink(TestLink(1L, Some(LocalDateTime.now())))),
            Some(CustomObject(Some("customName"), 94, Some(3.4 :: 4.5 :: Nil))))
          val testData = viewMapper.write(item)

          val viewReader = new DefaultViewReader(Map[ReadFeatures, Boolean]())
          val jsonViewReader = new JsonApiViewReader(viewReader, str => JsonApiJacksonFormat.jacksonMapper.readValue(str, classOf[RootObject]))
          val jsonViewWriter = new JsonApiViewWriter(viewMapper, root => JsonApiJacksonFormat.jacksonMapper.writeValueAsString(root))

          val testStr = jsonViewWriter.write(item)
          for (j <- 0 to batch) yield {

            /*val item = TestView("Js string value", 998, Some(1),
              Some(CustomObject(Some("customName"), 94, Some(3.4 :: 4.5 :: Nil))))*/
            val data = jsonViewReader.read[TestView](testStr)
            //val data = jsonViewWriter.write(item)
            count += /*1+data.length*0 // */ data.get.key.id.map(_.as[Long]).getOrElse(0L)
          }
          count
        }
      })
      val res = futures.map(fut => fut.get()).sum
      val timeMs = (System.nanoTime() - bTime) / 1000000
      System.out.println("res = "+res + " time = " + timeMs)
      System.out.println(res + " items converted to data in " + timeMs + "ms, " + (res.toDouble / timeMs.toDouble) + "ops in ms")
    }
  }

  override def main(args: Array[String]): Unit =
  {
    val executorService = new ThreadPoolExecutor(8, 8, 1000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue[Runnable]())
    //bench(executorService, 1, 16, 100000)
    System.out.println("\n\n")
    bench(executorService, 4, 16, 10000000)
  }
}

package com.skn.measurement.view

import java.time.LocalDateTime

import org.openjdk.jmh.annotations._
import java.util.concurrent.TimeUnit

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.skn.api.view.jsonapi.JsonApiMapper
import com.skn.api.view.jsonapi.JsonApiPlayModel.{ObjectKey, RootObject}
import com.skn.api.view.model.{SimpleLinkDefiner, ViewLink, ViewWriter}
import com.skn.common.view.model._
import com.skn.common.view.model.view.{CustomObject, Home, TestLink, TestView}
import com.skn.measurement.view.JsonApiFormatMeasurement.BenchmarkState
import play.api.libs.json.Json

import scala.reflect.runtime.{universe => ru}
import scala.util.Random

/**
  *
  * Created by Sergey on 01.10.2016.
  */
@Fork(value = 1, warmups = 0)
@Warmup(iterations = 4)
@Measurement(iterations = 4, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Array(Mode.Throughput))
class JsonApiFormatMeasurement //extends BaseUnitTest
{
  //
  //@State(Scope.Benchmark) val random = new Random(System.nanoTime())

  @Threads(1)
  //@Benchmark
  def create(state: BenchmarkState): TestView = {
    TestView("Js string value" + state.random.nextLong(), state.random.nextLong(),
      new Home("TH"), Some(0),
      Some(new ViewLink(TestLink(ObjectKey("link", 1L), Some(LocalDateTime.now())))),
      Some(CustomObject(Some("customName"), state.random.nextInt(), Some(3.4 :: 4.5 :: Nil))))
  }

  def reflect(state: BenchmarkState): Unit = {
    //val test = Some(List() ++ 3.4 ++ 4.5 ++ 23.6)//Person("test", 23, None)

    val item = TestView("Js string value" + state.random.nextLong().toString,
      state.random.nextLong(), new Home("TH"), Some(0L),
      Some(new ViewLink(TestLink(ObjectKey("link", 1L), Some(LocalDateTime.now())))),
      Some(CustomObject(Some("customName"), 34423, Some(List(3.4, 4.5)))))
    state.viewMapper.toData(item)
    /*val itemClass = reflectItem.symbol.asClass
    vamembers = itemClass.info.members
    members.size*/
  }

  @Threads(1)
  @Benchmark
  def reflect1(state: BenchmarkState): Unit = {
    reflect(state)
  }

  @Threads(3)
  @Benchmark
  def reflect4(state: BenchmarkState): Unit = {
    reflect(state)
    //val vars = itemClass.info.members.filter(_.isTerm).map(_.asTerm).filter(m => m.isVal || m.isVar)
  }

  @Threads(20)
  //@Benchmark
  def simpleTest(state: BenchmarkState): Unit = {
    Thread.sleep(100L)
    /*val one = 10L
    val two = 30L
    var res = 1L
    for(i <- 0L to 1000000L)
      res *= one * two * i
    res*/
  }

  @GroupThreads(4)
  //@Benchmark
  def writeBenchmark(state: BenchmarkState): Unit = {
    state.mapper.write(randomHouse(state.random))(state.houseFormat)
  }

  @GroupThreads(4)
  //@Benchmark
  def readPlayBenchmark(state: BenchmarkState): Unit = {
    state.mapper.read(Json.parse(jsonHouse))(state.houseFormat)
  }

  @GroupThreads(4)
  //@Benchmark
  def readJacksonBenchmark(state: BenchmarkState): Unit = {
    val root = state.jacksonMapper.readValue(jsonHouse, classOf[RootObject])
    state.houseFormat.fromRootObject(root)
  }

  @GroupThreads(1)
  //@Benchmark
  def writeBenchmarkOne(state: BenchmarkState): Unit = {
    state.mapper.write(randomHouse(state.random))(state.houseFormat)
  }

  @GroupThreads(1)
  //@Benchmark
  def writeWithNewMapperBenchmark(state: BenchmarkState) = {
    val mapper = new JsonApiMapper
    mapper.write(randomHouse(state.random))(state.houseFormat)
  }

  def randomPerson(random: Random) = Person("test", 34, Some(1)) //Person(random.nextString(3), random.nextInt(100), Some(random.nextLong()))

  def randomHouse(random: Random) = House(20, Address("test", "testb", Some(2)), Some(4))

  def jsonHouse = "{\"data\":[{\"type\":\"house\",\"attributes\":{\"price\":10},\"links\":{\"self\":\"http://skn.com/v1/house/1\"},\"relationships\":{\"address\":{\"links\":{\"self\":\"http://skm.com/v1/house/1/address\"},\"data\":[{\"type\":\"address\",\"id\":3}]},\"address_v2\":{\"links\":{\"related\":{\"href\":\"http://skm.com/v2/house/1/address\"}},\"data\":[{\"type\":\"address\",\"id\":3}]}}}]}"
}

object JsonApiFormatMeasurement {

  @State(Scope.Thread)
  class BenchmarkState {
    val mapper = new JsonApiMapper
    val viewMapper = new ViewWriter(new SimpleLinkDefiner)
    val personFormat = PersonFormat.format
    val houseFormat = HouseFormat.format
    val random = new Random(System.nanoTime())
    val jacksonMapper = new ObjectMapper()
    //val mirror = ru.runtimeMirror(TestView.getClass.getClassLoader)
    jacksonMapper.registerModule(DefaultScalaModule)
  }

}
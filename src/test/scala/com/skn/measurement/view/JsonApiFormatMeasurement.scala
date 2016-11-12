package com.skn.measurement.view

import java.time.LocalDateTime

import org.openjdk.jmh.annotations._
import java.util.concurrent.{ConcurrentHashMap, TimeUnit}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.skn.api.view.jsonapi.JsonApiMapper
import com.skn.api.view.jsonapi.JsonApiPlayModel.{Data, ObjectKey, RootObject}
import com.skn.api.view.model._
import com.skn.common.view.{CustomObject, Home, TestLink, TestView}
import com.skn.common.view.model._
import com.skn.measurement.view.JsonApiFormatMeasurement.{BenchmarkState, SharedState}
import play.api.libs.json.{JsValue, Json}

import scala.reflect.runtime.{universe => ru}
import scala.util.Random
import com.skn.api.view.jsonapi.JsonApiPlayFormat._
import com.skn.api.view.model.mapper.{SimpleLinkDefiner, ViewReader, ViewWriter}

import scala.collection.convert.Wrappers.ConcurrentMapWrapper
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
    TestView("Js string value" + state.random.nextLong().toString,
      state.random.nextLong(), new Home("TH"), Some(0L),
      Some(new ViewLink(TestLink(ObjectKey("link", 1L), Some(LocalDateTime.now())))),
      Some(CustomObject(Some("customName"), 34423, Some(List(3.4, 4.5)))))
  }

  def reflect(state: BenchmarkState): Data = {
    val item = create(state)
    state.viewWriter.write(item)
  }

  @Threads(1)
  //@Benchmark
  def writeTest1(state: BenchmarkState): Data = {
    reflect(state)
  }

  @Threads(3)
  //@Benchmark
  def writeTest3(state: BenchmarkState): Data = {
    reflect(state)
  }

  @Threads(1)
  //@Benchmark
  def readTest1(state: BenchmarkState): ViewItem =
    state.viewReader.read[TestView](state.testData)

  @Threads(3)
  //@Benchmark
  def readTest3(state: BenchmarkState): ViewItem =
    state.viewReader.read[TestView](state.testData)

  @Threads(1)
  @Benchmark
  def putTest(state: BenchmarkState, shared: SharedState): Unit = {
    shared.map.put(state.random.nextInt(), state.random.nextLong().toString)
  }

  /*@Threads(1)
  @Benchmark
  def jsView1(state: BenchmarkState): JsValue =
    state.playJson.toJson(state.viewWriter.write(create(state)))(dataFormat)

  @Threads(3)
  @Benchmark
  def jsView3(state: BenchmarkState): JsValue =
    state.playJson.toJson(state.viewWriter.write(create(state)))(dataFormat)

  @Threads(3)
  @Benchmark
  def jackson(state: BenchmarkState): String =
    state.jacksonMapper.writeValueAsString(state.viewWriter.write(create(state)))*/

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
    val viewWriter = new ViewWriter(new SimpleLinkDefiner)
    val viewReader = new ViewReader
    val personFormat = PersonFormat.format
    val houseFormat = HouseFormat.format
    val random = new Random(System.nanoTime())
    val jacksonMapper = new ObjectMapper()
    val playJson = Json
    //val mirror = ru.runtimeMirror(TestView.getClass.getClassLoader)
    jacksonMapper.registerModule(DefaultScalaModule)

    val testData = viewWriter.write(TestView("Js string value" + random.nextLong().toString,
      random.nextLong(), new Home("TH"), Some(0L),
      Some(new ViewLink(TestLink(ObjectKey("link", 1L), Some(LocalDateTime.now())))),
      Some(CustomObject(Some("customName"), 34423, Some(List(3.4, 4.5))))))
  }

  @State(Scope.Benchmark)
  class SharedState {
    val map = new ConcurrentHashMap[Int, String](10000000)
  }
}
package com.skn.measurement.view

import org.openjdk.jmh.annotations._
import java.util.concurrent.TimeUnit

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.skn.api.view.jsonapi.JsonApiMapper
import com.skn.api.view.jsonapi.JsonApiPalyModel.RootObject
import com.skn.common.view.model._
import com.skn.measurement.view.JsonApiFormatMeasurement.BenchmarkState
import play.api.libs.json.Json

import scala.util.Random
/**
  *
  * Created by Sergey on 01.10.2016.
  */
@Fork(value = 1, warmups = 0)
@Warmup(iterations = 5)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Array(Mode.AverageTime))
class JsonApiFormatMeasurement //extends BaseUnitTest
{
  //
  //@State(Scope.Benchmark) val random = new Random(System.nanoTime())

  //@Group("JsonapiSerializeOneThread")
  @GroupThreads(4)
  @Benchmark
  def writeBenchmark(state: BenchmarkState): Unit = { state.mapper.write(randomHouse(state.random))(state.houseFormat) }

  @GroupThreads(4)
  @Benchmark
  def writeWithJackson(state: BenchmarkState): Unit =
  {
    val root = state.houseFormat.toRootObject(randomHouse(state.random))
    state.jacksonMapper.writeValueAsString(root)
  }

  @GroupThreads(4)
  @Benchmark
  def readPlayBenchmark(state: BenchmarkState): Unit = { state.mapper.read(Json.parse(jsonHouse))(state.houseFormat) }

  @GroupThreads(4)
  @Benchmark
  def readJacksonBenchmark(state: BenchmarkState): Unit =
  {
    val root = state.jacksonMapper.readValue(jsonHouse, classOf[RootObject])
    state.houseFormat.fromRootObject(root)
  }

  @GroupThreads(1)
  //@Benchmark
  def writeBenchmarkOne(state: BenchmarkState): Unit = { state.mapper.write(randomHouse(state.random))(state.houseFormat) }

  @GroupThreads(1)
  //@Benchmark
  def writeWithNewMapperBenchmark(state: BenchmarkState) =
  {
    val mapper = new JsonApiMapper
    mapper.write(randomHouse(state.random))(state.houseFormat)
  }

  def randomPerson(random: Random) = Person("test", 34, Some(1))//Person(random.nextString(3), random.nextInt(100), Some(random.nextLong()))

  def randomHouse(random: Random) = House(20, Address("test", "testb", Some(2)), Some(4))

  def jsonHouse = "{\"data\":[{\"type\":\"house\",\"attributes\":{\"price\":10},\"links\":{\"self\":\"http://skn.com/v1/house/1\"},\"relationships\":{\"address\":{\"links\":{\"self\":\"http://skm.com/v1/house/1/address\"},\"data\":[{\"type\":\"address\",\"id\":3}]},\"address_v2\":{\"links\":{\"related\":{\"href\":\"http://skm.com/v2/house/1/address\"}},\"data\":[{\"type\":\"address\",\"id\":3}]}}}]}"
}

object JsonApiFormatMeasurement
{
  @State(Scope.Benchmark)
  class BenchmarkState
  {
    val mapper = new JsonApiMapper
    val personFormat = PersonFormat.format
    val houseFormat = HouseFormat.format
    val random = new Random(System.nanoTime())
    val jacksonMapper = new ObjectMapper()
    jacksonMapper.registerModule(DefaultScalaModule)
  }
}
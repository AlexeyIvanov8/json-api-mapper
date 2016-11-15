package com.skn.api.view.jsonapi

import com.skn.api.view.exception.ParsingException

/**
  *
  * Created by Sergey on 03.10.2016.
  */
object JsonApiValueModel
{
  trait JsonApiValueReader[T]
  {
    def read(jsonApi: JsonApiValue): T
  }

  sealed trait JsonApiValue { def as[T](implicit reader: JsonApiValueReader[T]): T = reader.read(this) }

  sealed trait JsonApiOneValue[T] extends JsonApiValue { val value: T }
  case class JsonApiString(value: String) extends JsonApiOneValue[String]
  case class JsonApiNumber(value: BigDecimal) extends JsonApiOneValue[BigDecimal]
  case class JsonApiFloat(value: Float) extends JsonApiOneValue[Float]
  case class JsonApiBoolean(value: Boolean) extends JsonApiOneValue[Boolean]
  case class JsonApiArray(seq: Seq[JsonApiValue]) extends JsonApiValue
  case class JsonApiObject(map: Map[String, JsonApiValue]) extends JsonApiValue

  class JsonApiStringReader extends JsonApiValueReader[String]
  {
    def read(jsonApi: JsonApiValue) = jsonApi match {
      case JsonApiString(value) => value
      case _ => throw ParsingException("Expected "+JsonApiString.getClass.getName+" value instead "+jsonApi.getClass.getName)
    }
  }

  class JsonApiNumberReader extends JsonApiValueReader[BigDecimal]
  {
    def read(jsonApi: JsonApiValue) = jsonApi match {
      case JsonApiNumber(value) => value
      case _ => throw ParsingException("Expected "+JsonApiNumber.getClass.getName+" value instead "+jsonApi.getClass.getName)
    }
  }

  /*class JsonApiFloatReader extends JsonApiValueReader[Float]
  {
    def read(jsonApi: JsonApiValue) = jsonApi match {
      case JsonApiFloat(value) => value
      case _ => throw ParsingException("Expected " + JsonApiFloat.getClass.getName + "value instead " + jsonApi.getClass.getName)
    }
  }*/

  class JsonApiBooleanReader extends JsonApiValueReader[Boolean]
  {
    def read(jsonApi: JsonApiValue) = jsonApi match {
      case JsonApiBoolean(value) => value
      case _ => throw ParsingException("Expected "+JsonApiBoolean.getClass.getName+" value instead "+jsonApi.getClass.getName)
    }
  }

  class JsonApiArrayReader extends JsonApiValueReader[Seq[JsonApiValue]]
  {
    def read(jsonApi: JsonApiValue) = jsonApi match {
      case JsonApiArray(value) => value
      case _ => throw ParsingException("Expected "+JsonApiArray.getClass.getName+" value instead "+jsonApi.getClass.getName)
    }
  }

  class JsonApiObjectReader extends JsonApiValueReader[Map[String, JsonApiValue]]
  {
    def read(jsonApi: JsonApiValue) = jsonApi match {
      case JsonApiObject(value) => value
      case _ => throw ParsingException("Expected "+JsonApiObject.getClass.getName+" value instead of "+jsonApi.getClass.getName)
    }
  }

  implicit val stringReader = new JsonApiStringReader
  implicit val numberReader = new JsonApiNumberReader
  //implicit val floatReader = new JsonApiFloatReader
  implicit val booleanReader = new JsonApiBooleanReader
  implicit val arrayReader = new JsonApiArrayReader
  implicit val objectReader = new JsonApiObjectReader
}

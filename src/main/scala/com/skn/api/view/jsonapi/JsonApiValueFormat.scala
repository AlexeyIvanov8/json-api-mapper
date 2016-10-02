package com.skn.api.view.jsonapi

import com.skn.api.view.exception.ParsingException
import com.skn.api.view.jsonapi.Model.{JsonApiArray, JsonApiBoolean, JsonApiNumber, JsonApiObject, JsonApiString, JsonApiValue}
import play.api.libs.json._
import play.api.libs.json.Format._

/**
  *
  * Created by Sergey on 02.10.2016.
  */
object JsonApiValueFormat
{
  /*implicit def jsonApiStringFormat = Json.format[JsonApiString]
  implicit def jsonApiIntFormat = Json.format[JsonApiInt]
  implicit def jsonApiDoubleFormat = Json.format[JsonApiDouble]
  implicit def jsonApiNumberFormat = Json.format[JsonApiNumber]
  implicit def jsonApiBooleanFormat = Json.format[JsonApiBoolean]

  implicit def jsonApiArrayFormat: Format[JsonApiArray] = Json.format[JsonApiArray]

  implicit def jsonApiObjectFormat: Format[JsonApiObject] = Json.format[JsonApiObject]*/

  implicit def jsonApiValueFormat = new Format[JsonApiValue]
  {
    override def reads(json: JsValue): JsResult[JsonApiValue] = JsSuccess(readJsonApiValue(json))

    def readJsonApiValue(json: JsValue): JsonApiValue = json match {
      case JsString(value) => JsonApiString(value)
      case JsNumber(value) => JsonApiNumber(value)
      case JsBoolean(value) => JsonApiBoolean(value)
      case JsArray(value) => JsonApiArray(value.map { element => readJsonApiValue(element) }) //value.map { element => elementlazyReadmap(_.read(jsonApiValueFormat) })
      case JsObject(obj) => JsonApiObject(obj.map { case (key: String, value: JsValue) => (key, readJsonApiValue(value)) }.toMap)
      case _ => throw ParsingException("Unhandled json api value type")
    }

    override def writes(jsonApi: JsonApiValue): JsValue = writeJsonApiValue(jsonApi)

    def writeJsonApiValue(jsonApi: JsonApiValue): JsValue = jsonApi match {
      case JsonApiString(value) => JsString(value)
      case JsonApiNumber(value) => JsNumber(value)
      case JsonApiBoolean(value) => JsBoolean(value)
      case JsonApiArray(value) => JsArray(value.map { element => writeJsonApiValue(element) })
      case JsonApiObject(value) => JsObject(value.map { case (key, element) => (key, writeJsonApiValue(element))})
      case _ => throw ParsingException("Unhandled json api value type")
    }
  }
  /*val jsonApiAnyVal = new Format[JsonApiAnyVal]
  {
    override def writes(value: JsonApiAnyVal): JsValue = Json.toJson(value)

    override def reads(json: JsValue): JsResult[JsonApiAnyVal] =
  }*/
}

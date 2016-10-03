package com.skn.api.view.jsonapi

import com.skn.api.view.exception.ParsingException
import com.skn.api.view.jsonapi.JsonApiPalyModel.RootObject
import com.skn.api.{Fail, Result, Success}
import play.api.libs.json.{JsValue, Json}
import com.skn.api.view.jsonapi.JsonApiPlayFormat._

/**
  * This class provide methods for mapping RootObject to and from json via @RootObjectMapper
  * Created by Sergey on 02.10.2016.
  */
class JsonApiMapper
{
	def write[T](value: T)(implicit format: RootObjectMapper[T]) =
	{
		val result = format.toRootObject(value) match
		{
			case result: Success[RootObject] => result.value
			case fail: Fail[RootObject] => RootObject()
			case _ => throw ParsingException("Undefined result type")
		}

		Json.toJson(result)(rootFormat)
	}

	def read[T](json: JsValue)(implicit format: RootObjectMapper[T]): Result[T] = format.fromRootObject(json.as[RootObject](rootFormat))
}

package com.skn.test.view

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import play.api.libs.json.{JsArray, Json}
import com.skn.common.view.model._
import com.skn.api.Success
import com.skn.api.view.jsonapi.JsonApiModel.RootObject
import com.skn.api.view.jsonapi.JsonApiPlayFormat.rootFormat
import com.skn.api.view.jsonapi.FieldNames
import com.skn.api.view.jsonapi.JsonApiValueModel.{JsonApiArray, JsonApiNumber, JsonApiObject, JsonApiString, JsonApiValue}
import com.skn.test.JsonApiTest
import com.skn.common.view.BaseUnitTest
import com.skn.api.view.jsonapi.JsonApiValueFormat._
import com.skn.api.view.jsonapi.JsonApiValueModel._

class JsonApiPlayFormatTest extends BaseUnitTest
{
	"A Person" should "serialize to json name age and id" taggedAs JsonApiTest in
	{
			val person = Person("John", 30, Some(1))
			val jsRoot = mapper.write(person)(PersonFormat.format)
			System.out.println("jsRoot = " + jsRoot.toString)
			val root = jsRoot.as[RootObject](rootFormat)
			root.data.size should be (1)
			val dataHead = root.data.get.head
			dataHead.key.id.get.as[Long] should be (1)
			dataHead.attributes.map { attrs =>
				attrs.contains("name") should be (true)
				attrs.contains("age") should be (true)
			}
			
			val personResult = mapper.read(jsRoot)(PersonFormat.format)
			personResult shouldBe a [Success[_]]
			personResult match {
				case success: Success[Person] =>
				  success.value.age should be (30 + 999)
          success.value.name should be ("John")
      }
      logger.debug("Person after deserialize = {}", personResult.asInstanceOf[Success[Person]].value)
	}
	
	"A Person" should "deserialize from Json api representation" taggedAs JsonApiTest in
	{
		val name = "D.D."
		val age = 19
		val jsPersonString = s"""
		{
			"data": [{"type": "person", "attributes": {"name": "$name", "age": $age}}]
		}	"""
		logger.info("js person string = {}", jsPersonString)
		
		val jsPerson = Json.parse(jsPersonString)
		val personResult = mapper.read(jsPerson)(PersonFormat.format)
		personResult shouldBe a [Success[_]]
		val person = personResult.asInstanceOf[Success[Person]].value
		person.id shouldBe empty
		person.name should be (name)
		person.age should be (age)
	}

	"A House" should "be have a price in data attributes and link to address" taggedAs JsonApiTest in
	{
		val address = Address("Line 2", "30/F", Some(3))
		val house = House(10, address)
		val jsHouseRoot = mapper.write(house)(HouseFormat.format)
		logger.info("Hose json = {}", jsHouseRoot)
    val houseRootResult = mapper.read(jsHouseRoot)(HouseFormat.format)
    houseRootResult shouldBe a [Success[_]]
    val houseRoot = houseRootResult match { case success: Success[House] => success.value }
    houseRoot.address.id shouldBe defined
    houseRoot.address.id.get should be (3)
    houseRoot.price shouldBe a [BigDecimal]
    houseRoot.price should be (BigDecimal.valueOf(10))
	}

  "A one elt data" should "be serialized in js field without array" taggedAs JsonApiTest in
  {
    val person = Person("Test name", 44, Some(0))
    val personRoot = mapper.write(person)(PersonFormat.format)
    logger.info("Person root = {}", personRoot)
    personRoot \ FieldNames.data should not be an [JsArray]
  }

  "A BigDecimal" should "be serialized without escaping" taggedAs JsonApiTest in
  {
    val bds = BigDecimal(10) :: BigDecimal(35544.978) :: Nil
    val jacksonMapper = new ObjectMapper()
    jacksonMapper.registerModule(DefaultScalaModule)
    val json = jacksonMapper.writeValueAsString(bds)
    logger.debug("big decimal json = "+json)
  }

  "A JsonApiValue" should "support composite objects" taggedAs JsonApiTest in
  {
    val objectTop = JsonApiObject(Map(
      "field" -> JsonApiString("field value"),
      "numTest" -> JsonApiNumber(96),
      "arrayF" -> JsonApiArray(JsonApiNumber(4) :: JsonApiObject(Map("inner" -> JsonApiNumber(3))) :: Nil),
      "objF" -> JsonApiObject(Map("in2" -> JsonApiString("in2val")))
    ))
    val json = Json.toJson(objectTop)
    val parsedObject = json.as[JsonApiValue].as[Map[String, JsonApiValue]]
    parsedObject.size should be (objectTop.map.size)
    parsedObject("field").as[String] shouldEqual "field value"
    parsedObject("numTest").as[BigDecimal] shouldEqual BigDecimal(96)
    parsedObject("arrayF") shouldBe a [JsonApiArray]
    parsedObject("arrayF").as[Seq[JsonApiValue]].head.as[BigDecimal] shouldEqual BigDecimal(4)
  }
}
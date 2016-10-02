package com.skn.common.view.model

import play.api.libs.json.{JsNumber, JsString, JsValue}
import play.api.libs.json.Format._
import com.skn.api.view.jsonapi.Model._
import com.skn.api.view.jsonapi.Model.{Data, ObjectKey, RootObject}
import com.skn.api.Success
import com.skn.api.view.jsonapi.RootObjectMapper


case class Person(name: String, age: Int, id: Option[Long])

object PersonFormat 
{
	val format = instance

  def instance = new RootObjectMapper[Person]
  {
    def fromRootObject(rootObject: RootObject) =
    {
      val data = rootObject.data.get.head
      val add = rootObject.meta match {
        case Some(value) => value("test").asInstanceOf[JsonApiNumber].value.intValue()
        case None => 0 }

      val attributes = data.attributes.get
      Success(Person(
        attributes("name").as[String],
        attributes("age").as[Int] + add,
        data.key.id ))
    }

    def toRootObject(person: Person) =
    {
      Success(
        RootObject(
          Some(Data(ObjectKey("person", person.id),
            Some(Attributes(
              "name" -> JsString(person.name),
              "age" -> JsNumber(person.age) ))
          )::Nil),
          None,
          Some(Meta(("test", JsonApiNumber(999))))
        )
      )
    }
  }
}
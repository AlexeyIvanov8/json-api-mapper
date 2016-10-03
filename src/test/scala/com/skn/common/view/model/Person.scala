package com.skn.common.view.model

import com.skn.api.view.jsonapi.JsonApiPalyModel._
import com.skn.api.view.jsonapi.JsonApiPalyModel.{Data, ObjectKey, RootObject}
import com.skn.api.Success
import com.skn.api.view.jsonapi.JsonApiValueModel.{JsonApiNumber, JsonApiString}
import com.skn.api.view.jsonapi.JsonApiValueModel._
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
        case Some(value) => value("test").as[BigDecimal].intValue()
        case None => 0 }

      val attributes = data.attributes.get
      Success(Person(
        attributes("name").as[String],
        attributes("age").as[BigDecimal].intValue() + add,
        data.key.id ))
    }

    def toRootObject(person: Person) =
    {
      Success(
        RootObject(
          Some(Data(ObjectKey("person", person.id),
            Some(Attributes(
              "name" -> JsonApiString(person.name),
              "age" -> JsonApiNumber(person.age) ))
          )::Nil),
          None,
          Some(Meta(("test", JsonApiNumber(999))))
        )
      )
    }
  }
}
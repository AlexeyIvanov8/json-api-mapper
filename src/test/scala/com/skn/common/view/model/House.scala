package com.skn.common.view.model

import play.api.libs.json.Json
import com.skn.api.view.jsonapi.Model.{Data, Link, ObjectKey, Relationship, Relationships, RootObject}
import com.skn.api.view.jsonapi.Model._
import com.skn.api.{Result, Success}
import com.skn.api.view.jsonapi.Model.Relationship
import com.skn.api.view.jsonapi.RootObjectMapper

case class House(price: BigDecimal, address: Address, id: Option[Long] = None)

object HouseFormat
{
	val format = new RootObjectMapper[House]
	{
		override def toRootObject(house: House) = Success(RootObject(
					Data(
							ObjectKey("house", house.id),
							Some(Attributes(
									("price", Json.toJson(house.price))	
								)),
						  Some(Link("http://skn.com/v1/house/1")),
							Some(Relationships(
									("address", Relationship(Link("http://skm.com/v1/house/1/address"), Some(ObjectKey("address", house.address.id) :: Nil))),
									("address_v2", Relationship(Link(Related("http://skm.com/v2/house/1/address")), (ObjectKey("address", house.address.id) :: Nil) ))
								))
					) :: Nil)
				)

		override def fromRootObject(rootObject: RootObject): Result[House] =
		{
			val dataHead = rootObject.data.get.head
			val attributes = dataHead.attributes.get
			val relationships = dataHead.relationships.get
			val addressRel = relationships.get("address").get
			val addressKey = addressRel.data.get.head
			val address = Address("MockStreet", "MockBuilding", addressKey.id)

			Success(House(
					attributes.get("price").get.as[BigDecimal],
					address,
          dataHead.key.id
				))
		}
	}
}
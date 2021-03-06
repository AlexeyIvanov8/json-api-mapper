package com.skn.common.view.model

import com.skn.api.view.jsonapi.JsonApiValueModel.JsonApiNumber
import com.skn.api.view.jsonapi.JsonApiModel.{Data, Link, ObjectKey, Relationships, RootObject}
import com.skn.api.view.jsonapi.JsonApiModel._
import com.skn.api.{Result, Success}
import com.skn.api.view.jsonapi.JsonApiModel.Relationship
import com.skn.api.view.jsonapi.RootObjectMapper
import com.skn.api.view.model.ViewItem

import com.skn.api.view.jsonapi.JsonApiValueModel._

case class House(price: BigDecimal, address: Address, id: Option[Long] = None)
	extends ViewItem { val key = ObjectKey("house", id.map(JsonApiNumber(_))) }

object HouseFormat
{
	val format = new RootObjectMapper[House]
	{
		override def toRootObject(house: House) = Success(RootObject(
					Data(
							ObjectKey("house", house.id.map(JsonApiNumber(_))),
							Some(Attributes(
									("price", JsonApiNumber(house.price))
								)),
						  Some(Link("http://skn.com/v1/house/1")),
							Some(Relationships(
									("address", Relationship(Link("http://skm.com/v1/house/1/address"),
										Some(ObjectKey("address", house.address.id.map(JsonApiNumber(_))) :: Nil))),
									("address_v2", Relationship(Link(Related("http://skm.com/v2/house/1/address")),
										ObjectKey("address", house.address.id.map(JsonApiNumber(_))) :: Nil ))
								))
					) :: Nil)
				)

		override def fromRootObject(rootObject: RootObject): Result[House] =
		{
			val dataHead = rootObject.data.get.head
			val attributes = dataHead.attributes.get
			val relationships = dataHead.relationships.get
			val addressRel = relationships("address")
			val addressKey = addressRel.data.get.head
			val address = Address("MockStreet", "MockBuilding", addressKey.id.map(_.as[Long]))

			Success(House(
					attributes("price").asInstanceOf[JsonApiNumber].value,
					address,
          dataHead.key.id.map(_.as[Long])
				))
		}
	}
}
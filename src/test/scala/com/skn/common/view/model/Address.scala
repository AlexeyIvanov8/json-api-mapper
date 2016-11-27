package com.skn.common.view.model

import com.skn.api.view.jsonapi.JsonApiModel.{Attributes, Data, ObjectKey, RootObject}
import com.skn.api.Success
import com.skn.api.view.jsonapi.JsonApiValueModel.{JsonApiNumber, JsonApiString}
import com.skn.api.view.jsonapi.RootObjectMapper

import com.skn.api.view.jsonapi.JsonApiValueModel._

case class Address(street: String, building: String, id: Option[Long])

object AddressFormat
{
	val format = new RootObjectMapper[Address]
	{
		def toRootObject(address: Address) =
			Success(
					RootObject(
							Some(Data(
									ObjectKey("address", address.id.map(JsonApiNumber(_))),
									Some(Attributes(
											("street", JsonApiString(address.street)),
											("building", JsonApiString(address.building))
										))
								) :: Nil)
						)
				)
		
		def fromRootObject(root: RootObject) = 
		{
			val dataHead = root.data.get.head
			val attrs = dataHead.attributes.get
			Success(Address(
				attrs("street").asInstanceOf[JsonApiString].value,
				attrs("building").asInstanceOf[JsonApiString].value, dataHead.key.id.map(_.as[Long])))
		}
	}
}
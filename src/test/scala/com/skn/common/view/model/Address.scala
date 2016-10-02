package com.skn.common.view.model

import com.skn.api.view.jsonapi.Model.{Attributes, Data, ObjectKey, RootObject}
import com.skn.api.Success
import com.skn.api.view.jsonapi.RootObjectMapper
import play.api.libs.json.JsString

case class Address(street: String, building: String, id: Option[Long])

object AddressFormat
{
	val format = new RootObjectMapper[Address]
	{
		def toRootObject(address: Address) =
			Success(
					RootObject(
							Some(Data(
									ObjectKey("address", address.id),
									Some(Attributes(
											("street", JsString(address.street)),
											("building", JsString(address.building))
										))
								) :: Nil)
						)
				)
		
		def fromRootObject(root: RootObject) = 
		{
			val dataHead = root.data.get.head
			val attrs = dataHead.attributes.get
			Success(Address(attrs.get("street").get.as[String], attrs.get("building").get.as[String], dataHead.key.id))
		}
	}
}
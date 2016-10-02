package com.skn.api

package version
{
	sealed abstract class ApiVersion(val order: Int, val name: String) extends Ordered[ApiVersion]
	{
		def compare(other: ApiVersion) = this.order - other.order
	}
	
	case object BadVersion extends ApiVersion(0, "BadVersion")
	case object V1 extends ApiVersion(1, "v1")
	case object Latest extends ApiVersion(-1, "Latest")
	
	
	object ApiVersion
	{
		def valueOf(name: String): ApiVersion = name match 
		{
			case V1.name => V1
			case Latest.name => Latest
			case _ => BadVersion
		}
	
		def apply(name: String) = valueOf(name)
		def unapply(apiVersion: ApiVersion) = (apiVersion.name)
	}
}
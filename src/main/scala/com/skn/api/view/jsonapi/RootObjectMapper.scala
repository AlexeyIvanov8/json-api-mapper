package com.skn.api.view.jsonapi

import com.skn.api.view.jsonapi.JsonApiModel.{Data, Meta, Relationship, RootObject}
import com.skn.api.{Result, Success}

/**
  * The trait provide low level mapping any object to RootObject of json api.
  * Implement this trait for class, that you want map to Json api and use @JsonApiMapper
  * Created by Sergey on 02.10.2016.
  */
trait RootObjectMapper[T]
{
  def toRootObject(t: T): Result[RootObject]
  def fromRootObject(rootObject: RootObject): Result[T]
}

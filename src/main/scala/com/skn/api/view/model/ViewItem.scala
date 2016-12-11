package com.skn.api.view

import com.skn.api.view.jsonapi.JsonApiModel.ObjectKey

/**
  * Model for present view structure
  * Created by Sergey on 03.10.2016.
  */
package model {

  import com.skn.api.view.jsonapi.JsonApiValueModel.JsonApiValue

  /**
    * Trait for classes that contains data of view
    */
  trait ViewItem { val key: ObjectKey }

  /**
    * Container for view's object like Option
    * @tparam V - type of view data
    */
  class ViewLink[V <: ViewItem](val key: ObjectKey)
  {
    def this(view: V) = this(view.key)

    override def equals(obj: scala.Any): Boolean = {
      obj match {
        case null => false
        case link: ViewLink[V] => key.equals(link.key)
        case _ => false
      }
    }

    override def hashCode(): Int = key.hashCode()
    /*override def == (obj: Any): Boolean = {
      this.equals(obj)

    }*/
  }

  /**
    * Trait, that mark object present JsonObject = Map[String, JsValue]
    */
  trait ViewObject

  /**
    * Trait for create values who can be serialize/deserialize in String
    * Also, for that must be defined companion object with ViewValueFactory trait
    */
  trait ViewValue {
    override def toString: String = super.toString
  }

  /**
    * When we discover class during deserialization process we have only types without instance.
    * So we need factory for creation instance from String
    * @tparam V - type of instance for creation
    */
  trait ViewValueFactory[V <: ViewValue] {
    def fromString(str: String): V
  }

}
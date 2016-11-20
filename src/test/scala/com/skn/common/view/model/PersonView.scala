package com.skn.common.view.model

import com.skn.api.view.jsonapi.JsonApiModel.ObjectKey
import com.skn.api.view.model.ViewItem

import scala.language.experimental.macros
/**
  *
  * Created by Sergey on 04.10.2016.
  */
case class PersonView(name: String, test: Number, id: Option[Long] = None) extends ViewItem { val key = ObjectKey("person", id) }

package com.skn.common.view.model

import com.skn.api.view.jsonapi.JsonApiPalyModel.ObjectKey
import com.skn.api.view.model.{Test, ViewItem}
import com.skn.api.view.model.ViewBuildMacros._

import scala.language.experimental.macros
/**
  *
  * Created by Sergey on 04.10.2016.
  */
case class PersonView(name: String, test: Number, id: Option[Long] = None) extends ViewItem { val key = ObjectKey("person", id) }

object PersonView
{
  def buildView(view: Test) = build(view)
}

package com.skn.common.view.model

import com.skn.api.view.jsonapi.JsonApiModel.ObjectKey
import com.skn.api.view.jsonapi.JsonApiValueModel.JsonApiString
import com.skn.api.view.model.ViewItem

/**
  * Created by Sergey on 25.11.2016.
  */
case class WithStringId(id: String, value: Long) extends ViewItem {
  val key = ObjectKey("with_string_id", JsonApiString(id))
}

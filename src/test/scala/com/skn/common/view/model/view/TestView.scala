package com.skn.common.view.model.view

import com.skn.api.view.jsonapi.JsonApiPalyModel.ObjectKey
import com.skn.api.view.model.ViewItem

/**
  *
  * Created by Sergey on 11.10.2016.
  */
case class TestView(str: String, num: BigDecimal, id: Option[Long], custom: Option[CustomObject]) extends ViewItem { val key = ObjectKey("testType", id) }

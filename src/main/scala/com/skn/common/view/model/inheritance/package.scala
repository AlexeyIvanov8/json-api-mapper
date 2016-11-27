package com.skn.common.view.model

import com.skn.api.view.jsonapi.JsonApiModel.ObjectKey
import com.skn.api.view.model.ViewItem

/**
  * Created by Sergey on 25.11.2016.
  */
package object inheritance {

  class LowParent(val id: Long,
                  val firstName: String,
                  protected val `type`: String = "low_parent") extends ViewItem { val key = ObjectKey(`type`, id) }

  class WideChild(id: Long, override val firstName: String, val lastName: String)
    extends LowParent(id = id, firstName = firstName, `type` = "wide_child")
}

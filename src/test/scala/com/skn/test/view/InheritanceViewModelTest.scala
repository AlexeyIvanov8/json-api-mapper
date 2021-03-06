package com.skn.test.view

import com.skn.common.view.BaseUnitTest
import com.skn.common.view.model.inheritance.{LowParent, WideChild}
import org.slf4j.LoggerFactory

/**
  * Created by Sergey on 25.11.2016.
  */
class InheritanceViewModelTest extends BaseUnitTest {

  "A child serialization" should "be supported" in {
    val parent = new LowParent(5L, "Low")
    val child = new WideChild(3L, "Wide", "TC")
    val jsonChild = mappers.jsonViewWriter.write(child)
    val jsonParent = mappers.jsonViewWriter.write(parent)
    logger.info("Parent = " + jsonParent)
    logger.info("Child = " + jsonChild)

    val parentAfter = mappers.jsonViewReader.read[LowParent](jsonParent).get.head
    val childAfter = mappers.jsonViewReader.read[WideChild](jsonChild).get.head

    parentAfter.key should equal (parent.key)
    childAfter.key.`type` should equal (child.key.`type`)
    childAfter.lastName should equal (child.lastName)
    childAfter.firstName should equal (child.firstName)
  }
}

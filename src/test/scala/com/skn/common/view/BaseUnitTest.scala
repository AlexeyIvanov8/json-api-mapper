package com.skn.common.view

import com.skn.api.view.jsonapi.JsonApiMapper
import com.typesafe.scalalogging._
import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.LoggerFactory

class BaseUnitTest extends FlatSpec with Matchers
{
  protected val logger = Logger(LoggerFactory.getLogger(classOf[BaseUnitTest]))
  val mapper = new JsonApiMapper
}
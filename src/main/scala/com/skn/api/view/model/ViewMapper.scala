package com.skn.api.view.model

import java.time.{LocalDate, LocalDateTime}
import java.time.temporal.Temporal
import java.util.concurrent.atomic.AtomicLong

import com.skn.api.view.exception.ParsingException
import com.skn.api.view.jsonapi.JsonApiPlayModel.{Data, Link, Relationship}
import com.skn.api.view.jsonapi.JsonApiPlayModel._
import com.skn.api.view.jsonapi.JsonApiValueModel.{JsonApiArray, JsonApiBoolean, JsonApiNumber, JsonApiObject, JsonApiString, JsonApiValue, JsonApiValueReader}
import com.sun.org.apache.bcel.internal.generic.ObjectType
import org.slf4j.LoggerFactory

import scala.reflect.ClassTag
import scala.reflect.runtime.{universe => ru}
/**
  *
  * Created by Sergey on 03.10.2016.
  */
trait ViewMapper
{
  def toData[T <: ViewItem](item: T)(implicit classTag: ClassTag[T]): Data
  //def fromData[T <: ViewItem](data: Data)(implicit classTag: ClassTag[T]): T
  //def toRelationship[T <: ViewData](item: T): Relationship
  //def fromRelationship[T <: ViewData](relation: Relationship): T
}





object ViewMapper
{

  def write(view: ViewItem)(implicit unapply: ViewItem => Option[Seq[_]]) =
  {
    unapply(view).map { seq =>
      seq.map {
        member => member match {
          case other: ViewItem => Relationship(Link("test"), view.key :: Nil)
          case value: String => JsonApiString(value)
          case value: BigDecimal => JsonApiNumber(value)
          case value: Int => JsonApiNumber(value)
          case value: Double => JsonApiNumber(value)
          case value: Long => JsonApiNumber(value)
          case value: Boolean => JsonApiBoolean(value)
        }
      }
    }
  }
}

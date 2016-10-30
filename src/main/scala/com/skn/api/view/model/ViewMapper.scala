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



class DefaultViewMapper extends ViewMapper {
  private val logger = LoggerFactory.getLogger(classOf[DefaultViewMapper])

  // cases definitions
  case class ReflectData(mirror: ru.Mirror, vars: Map[String, ru.FieldMirror])

  var reflectCache = Map[Class[_], ReflectData]()

  private def cacheReflectData(item: Any): ReflectData = {
    val itemClass = item.getClass
    if (!reflectCache.contains(itemClass)) {
      val mirror = ru.runtimeMirror(itemClass.getClassLoader)

      val reflectItem = mirror.reflect(item)
      val reflectItemClass = reflectItem.symbol.asClass

      val test = reflectItemClass.info.members
        .filter(_.isTerm)
        .map(_.asTerm)
        .filter(m => m.isVal || m.isVar).toSeq
      for(f <- test) {
        reflectItem.reflectField(f)
      }
      val vars = reflectItemClass.info.members
        .filter(_.isTerm)
        .map(_.asTerm)
        .filter(m => m.isVal || m.isVar)
        .map(field => field.getter.name.toString -> reflectItem.reflectField(field))
        .toMap

      reflectCache += (itemClass -> ReflectData(mirror, vars))
    }
    reflectCache(itemClass)
  }

  private def toJsonValue(field: Any): JsonApiValue = {
    field match {
      case value: String => JsonApiString(value)
      case value: BigDecimal => JsonApiNumber(value)
      case value: Boolean => JsonApiBoolean(value)
      case value: Int => JsonApiNumber(value)
      case value: Long => JsonApiNumber(value)
      case value: Double => JsonApiNumber(value)
      case value: Temporal => JsonApiString(value.toString)
      case value: ViewValue => JsonApiString(value.toString)
      case values: Seq[_] => JsonApiArray(values.map(value => toJsonValue(value)))
      case value: AnyRef => JsonApiObject(toJsonObject(value))
      case _ => throw ParsingException("Unhandled value " + field.toString + " while convert to view item")
    }
  }

  val assignmentCount = new AtomicLong(0L)

  protected def toJsonObject(item: Any): Map[String, JsonApiValue] = {
    val reflectData = cacheReflectData(item)
    reflectData.vars.flatMap { case (name, field) =>
      val fieldValue = field.bind(item).get
      fieldValue match {
        // skip system values
        case value: ObjectKey => None
        case value: Option[_] => value match {
          case Some(r) => Some(name -> toJsonValue(r))
          case None => None
        }
        case value: Any => Some(name -> toJsonValue(value))
      }
    }
  }

  override def toData[T <: ViewItem](item: T)(implicit classTag: ClassTag[T]): Data = {
    Data(item.key, Some(toJsonObject(item)))
  }

  //override def fromData[T <: ViewItem](data: Data)(implicit classTag: ClassTag[T]): T = ???
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

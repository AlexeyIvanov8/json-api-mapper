package com.skn.api.view.model

import java.lang.reflect.Method
import java.time.temporal.Temporal
import java.util.concurrent.atomic.AtomicLong

import com.skn.api.view.exception.ParsingException
import com.skn.api.view.jsonapi.JsonApiPalyModel.{Data, Link, Relationship}
import com.skn.api.view.jsonapi.JsonApiPalyModel._
import com.skn.api.view.jsonapi.JsonApiValueModel.{JsonApiArray, JsonApiBoolean, JsonApiNumber, JsonApiObject, JsonApiString, JsonApiValue}
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
  def fromData[T <: ViewItem](data: Data)(implicit classTag: ClassTag[T]): T
  def toRelationship[T <: ViewItem](item: T): Relationship
  def fromRelationship[T <: ViewItem](relation: Relationship): T
}

class DefaultViewMapper extends ViewMapper
{
  private val logger = LoggerFactory.getLogger(classOf[DefaultViewMapper])

  protected def toJsonValue(field: Any): JsonApiValue = {
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
      case _ => throw ParsingException("Unhandled value")
    }
  }

  val assignmentCount = new AtomicLong(0L)
  protected def toJsonObject(item: Any): Map[String, JsonApiValue] = {
    val reflectData = cacheReflectData(item)
    reflectData.vars.flatMap { field =>
      val fieldValue = field.bind(item).get
      //fieldValue match { case _ => None }
      fieldValue match {
        // skip system values
        case value: ObjectKey => None
        case value: Option[_] => value match {
          case Some(r) => Some(field.symbol.name.toString -> toJsonValue(r))
          case None => None
        }
        case value: Any => Some(field.symbol.name.toString -> toJsonValue(value))
      }
    }.toMap
  }

  var fieldsCache = Map[String, ru.FieldMirror]()

  case class ReflectData(mirror: ru.Mirror, vars: List[ru.FieldMirror])

  var reflectCache = Map[Class[_], ReflectData]()

  private def cacheReflectData(item: Any): ReflectData = {
    val itemClass = item.getClass
    if(!reflectCache.contains(itemClass)) {
      val mirror = ru.runtimeMirror(item.getClass.getClassLoader)

      val reflectItem = mirror.reflect(item)
      val reflectItemClass = reflectItem.symbol.asClass
      val vars = reflectItemClass.info.members
        .filter(_.isTerm)
        .map(_.asTerm)
        .filter(m => m.isVal || m.isVar)
        .map(field => reflectItem.reflectField(field))
        .toList

      reflectCache += (itemClass -> ReflectData(mirror, vars))
    }
    reflectCache(itemClass)
  }

  override def toData[T <: ViewItem](item: T)(implicit classTag: ClassTag[T]): Data = {
    Data(item.key, Some(toJsonObject(item)))
  }

  override def fromData[T <: ViewItem](data: Data)(implicit classTag: ClassTag[T]): T =
  {
    val mirror = ru.runtimeMirror(classTag.runtimeClass.getClassLoader)

    logger.info("Fields of " + classTag.runtimeClass.getName + " = " + classTag.runtimeClass.getDeclaredFields.map(field => field.getName)
      .reduceOption((l, r) => l + ", " + r))
    classTag.runtimeClass.getFields.map(field => field)
    classTag.runtimeClass.newInstance().asInstanceOf[T]
  }

  override def toRelationship[T <: ViewItem](item: T): Relationship = ???

  override def fromRelationship[T <: ViewItem](relation: Relationship): T = ???
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

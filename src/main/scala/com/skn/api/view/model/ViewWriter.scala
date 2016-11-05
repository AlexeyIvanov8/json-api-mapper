package com.skn.api.view.model

import java.time.temporal.Temporal

import com.skn.api.view.exception.ParsingException
import com.skn.api.view.jsonapi.JsonApiPlayModel.{Data, Link, ObjectKey, Relationship}
import com.skn.api.view.jsonapi.JsonApiValueModel.{JsonApiArray, JsonApiBoolean, JsonApiNumber, JsonApiObject, JsonApiString, JsonApiValue}
import org.slf4j.LoggerFactory

import scala.reflect.ClassTag
import scala.reflect.runtime.{universe => ru}
/**
  * Created by Sergey on 30.10.2016.
  */
class ViewWriter(val linkDefiner: LinkDefiner) extends ViewMapper {
  private val logger = LoggerFactory.getLogger(classOf[ViewWriter])

  // cases definitions
  case class ReflectData(mirror: ru.Mirror, itemType: ru.Type, vars: Map[String, ru.FieldMirror])

  var reflectCache = Map[Class[_], ReflectData]()

  private def cacheReflectData(item: Any): ReflectData = {
    val itemClass = item.getClass
    if (!reflectCache.contains(itemClass)) {
      val mirror = ru.runtimeMirror(itemClass.getClassLoader)

      val reflectItem = mirror.reflect(item)
      val reflectItemClass = reflectItem.symbol.asClass

      val vars = reflectItemClass.info.members
        .filter(_.isTerm)
        .map(_.asTerm)
        .filter(m => m.isVal || m.isVar)
        .map(field => field.getter.name.toString -> reflectItem.reflectField(field))
        .toMap

      reflectCache += (itemClass -> ReflectData(mirror, reflectItemClass.typeSignature, vars))
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

  case class FieldDesc(isOption: Boolean, isSeq: Boolean, fieldMirror: ru.FieldMirror)
  case class DataContainer(attributes: scala.collection.mutable.Map[String, JsonApiValue] = scala.collection.mutable.Map[String, JsonApiValue](),
                           relationships: scala.collection.mutable.Map[String, Relationship] = scala.collection.mutable.Map[String, Relationship]())

  def write[T <: ViewItem](item: T): Data =
  {
    val container = DataContainer()
    val reflectData = cacheReflectData(item)
    reflectData.vars.map { case (name, desc) =>
      val fieldType = desc.symbol.typeSignature
      val value = desc.bind(item).get
      value match {
        // skip system values
        case d if fieldType <:< ViewMappingInfo.ObjectKeyType => None
        case d if fieldType <:< ViewMappingInfo.OptionType => value match {
          case Some(r) => Some(writeField(fieldType, name, r, container))
          case None => None
        }
        case value: Any => writeField(fieldType, name, value, container)
      }
    }
    Data(item.key,
      Some(container.attributes.toMap),
      Some(linkDefiner.getLink(reflectData.itemType)),
      Some(container.relationships.toMap))
  }

  def writeField(fieldType: ru.Type, fieldName: String, value: Any, container: DataContainer): Any = {
    fieldType match {
      case f if f <:< ViewMappingInfo.SeqType && f.typeArgs.head <:< ViewMappingInfo.ViewLinkType =>
        container.relationships.put(fieldName, writeSeqRelationship(f.typeArgs.head, value.asInstanceOf[Seq[ViewLink[_ <: ViewItem]]]))
      case f if f <:< ViewMappingInfo.ViewLinkType =>
        container.relationships.put(fieldName, writeOneRelationship(fieldType, value.asInstanceOf[ViewLink[_ <: ViewItem]]))
      case _ =>
        container.attributes.put(fieldName, toJsonValue(value))
    }
  }

  def writeSeqRelationship(linkedItemType: ru.Type, relationships: Seq[ViewLink[_ <: ViewItem]]): Relationship = {
    Relationship(linkDefiner.getLink(linkedItemType), relationships.map(_.key))
  }

  def writeOneRelationship(linkedItemType: ru.Type, relationship: ViewLink[_ <: ViewItem]): Relationship = {
    Relationship(linkDefiner.getLink(linkedItemType), relationship.key :: Nil)
  }

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

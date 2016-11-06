package com.skn.api.view.model

import java.time.temporal.Temporal

import com.skn.api.view.exception.ParsingException
import com.skn.api.view.jsonapi.JsonApiPlayModel.{Data, Link, ObjectKey, Relationship}
import com.skn.api.view.jsonapi.JsonApiValueModel.{JsonApiArray, JsonApiBoolean, JsonApiNumber, JsonApiObject, JsonApiString, JsonApiValue}
import org.slf4j.LoggerFactory

import scala.reflect.runtime.{universe => ru}
/**
  * Created by Sergey on 30.10.2016.
  */
class ViewWriter(val linkDefiner: LinkDefiner) {
  private val logger = LoggerFactory.getLogger(classOf[ViewWriter])

  // cases definitions
  private trait FieldDesc {
    val isOption: Boolean
    val isSeq: Boolean
    val fieldMirror: ru.FieldMirror
    val fieldType: ru.Type
  }

  private case class LinkFieldDesc(isOption: Boolean, isSeq: Boolean, fieldMirror: ru.FieldMirror, fieldType: ru.Type) extends FieldDesc
  private case class AttributeFieldDesc(isOption: Boolean, isSeq: Boolean, fieldMirror: ru.FieldMirror, fieldType: ru.Type) extends FieldDesc

  private case class ReflectData(mirror: ru.Mirror, itemType: ru.Type, vars: Map[String, FieldDesc])
  private var reflectCache = Map[Class[_], ReflectData]()

  private class DataContainer {
    var attributes: Map[String, JsonApiValue] = Map[String, JsonApiValue]()
    var relationships: Map[String, Relationship] = Map[String, Relationship]()
  }

  def write[T <: ViewItem](item: T): Data =
  {
    val container = new DataContainer()
    val reflectData = cacheReflectData(item)
    reflectData.vars.map { case (name, desc) =>
      val value = desc.fieldMirror.bind(item).get
      value match {
        // skip system values
        case ObjectKey => None
        case d if desc.isOption => value match {
          case Some(r) => Some(writeField(desc, desc.fieldType.typeArgs.head, name, r, container))
          case None => None
        }
        case value: Any => writeField(desc, desc.fieldType, name, value, container)
      }
    }
    Data(item.key,
      Some(container.attributes),
      Some(linkDefiner.getLink(reflectData.itemType)),
      Some(container.relationships))
  }

  private def writeField(desc: FieldDesc, fieldType: ru.Type, fieldName: String, value: Any, container: DataContainer): Unit = {
    desc match {
      case d: LinkFieldDesc if d.isSeq =>
        container.relationships += (fieldName -> writeSeqRelationship(fieldType.typeArgs.head, value.asInstanceOf[Seq[ViewLink[_ <: ViewItem]]]))
      case d: LinkFieldDesc =>
        container.relationships += (fieldName -> writeOneRelationship(fieldType, value.asInstanceOf[ViewLink[_ <: ViewItem]]))
      case d: AttributeFieldDesc =>
        container.attributes += (fieldName -> toJsonValue(value))
    }
  }

  private def writeSeqRelationship(linkedItemType: ru.Type, relationships: Seq[ViewLink[_ <: ViewItem]]): Relationship = {
    Relationship(linkDefiner.getLink(linkedItemType), relationships.map(_.key))
  }

  private def writeOneRelationship(linkedItemType: ru.Type, relationship: ViewLink[_ <: ViewItem]): Relationship = {
    Relationship(linkDefiner.getLink(linkedItemType), relationship.key :: Nil)
  }

  private def toJsonObject(item: Any): Map[String, JsonApiValue] = {
    val reflectData = cacheReflectData(item)
    reflectData.vars.flatMap { case (name, field) =>
      val fieldValue = field.fieldMirror.bind(item).get
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
        .map { field => field.getter.name.toString -> {
          var aType = field.typeSignature
          val isOption = aType <:< ViewMappingInfo.OptionType
          if (isOption)
            aType = aType.typeArgs.head
          val isSeq = aType <:< ViewMappingInfo.SeqType
          if (isSeq)
            aType = aType.typeArgs.head
          aType match {
            case t if t <:< ViewMappingInfo.ViewLinkType => LinkFieldDesc(
              isOption = isOption, isSeq = isSeq, reflectItem.reflectField(field), field.typeSignature)
            case _ => AttributeFieldDesc(isOption, isSeq, reflectItem.reflectField(field), field.typeSignature)
          }
        }}
        .toMap

      reflectCache += (itemClass -> ReflectData(mirror, reflectItemClass.typeSignature, vars))
    }
    reflectCache(itemClass)
  }
}

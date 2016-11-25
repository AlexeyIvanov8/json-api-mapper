package com.skn.api.view.model.mapper

import java.time.temporal.Temporal

import com.skn.api.view.exception.ParsingException
import com.skn.api.view.jsonapi.JsonApiModel.{Data, ObjectKey, Relationship}
import com.skn.api.view.jsonapi.JsonApiValueModel.{JsonApiArray, JsonApiBoolean, JsonApiNumber, JsonApiObject, JsonApiString, JsonApiValue}
import com.skn.api.view.model._
import com.skn.api.view.model.data._
import org.slf4j.LoggerFactory

import scala.reflect.runtime.{ universe => ru }
/**
  * Default implementation
  * @param linkDefiner - for creating links to other view items
  */
class DefaultViewWriter(val linkDefiner: LinkDefiner) extends ViewWriter {
  
  private val logger = LoggerFactory.getLogger(classOf[ViewWriter])

  // cases definitions
  private case class ReflectData(mirror: ru.Mirror, itemType: ru.Type, vars: Map[String, MirrorFieldDesc])
  private var reflectCache = Map[Class[_], ReflectData]()

  private class DataContainer {
    var attributes: Map[String, JsonApiValue] = Map[String, JsonApiValue]()
    var relationships: Map[String, Relationship] = Map[String, Relationship]()
  }

  /**
    * Entry point for ViewItem write
    * @param item - what you want write
    * @tparam T - concrete item type
    * @return json api Data, that represent send item
    */
  def write[T <: ViewItem](item: T): Data =
  {
    val container = new DataContainer()
    val reflectData = cacheReflectData(item)
    reflectData.vars.map { case (name, field) =>
      val value = field.mirror.bind(item).get
      value match {
        // skip system values
        case v: ObjectKey => None
        case null => null//throw ParsingException("null is not support in root fields(" + name + "). Use explicit None instead.")
        case d if field.desc.isOption => value match {
          case Some(r) => Some(writeField(field.desc, name, r, container))
          case None => None
        }
        case value: Any => writeField(field.desc, name, value, container)
      }
    }
    Data(item.key,
      container.attributes match {
        case a if a.isEmpty => None
        case _ => Some(container.attributes) },
      Some(linkDefiner.getLink(item.key)),
      container.relationships match {
        case r if r.isEmpty => None
        case _ => Some(container.relationships) })
  }

  /**
    * Aggregate Option unboxed value to container
    */
  private def writeField(desc: FieldDesc, fieldName: String, value: Any, container: DataContainer): Unit = {
    desc match {
      case _ @ (_: AttributeFieldDesc | _: ValueFieldDesc) =>
        container.attributes += fieldName -> toJsonValue(value)
      case d: LinkFieldDesc if d.isSeq =>
        val relationshipsSeq = value.asInstanceOf[Seq[ViewLink[_ <: ViewItem]]]
        if(!relationshipsSeq.isEmpty)
           container.relationships += fieldName -> writeSeqRelationship(desc.unpackType, relationshipsSeq)
      case _: LinkFieldDesc =>
        container.relationships +=
          fieldName -> writeOneRelationship(desc.unpackType, value.asInstanceOf[ViewLink[_ <: ViewItem]])
    }
  }

  private def writeSeqRelationship(linkedItemType: ru.Type, relationships: Seq[ViewLink[_ <: ViewItem]]): Relationship = {
    Relationship(linkDefiner.getCommonLink(relationships.head.key), relationships.map(_.key))
  }

  private def writeOneRelationship(linkedItemType: ru.Type, relationship: ViewLink[_ <: ViewItem]): Relationship = {
    Relationship(linkDefiner.getLink(relationship.key), relationship.key :: Nil)
  }

  private def toJsonObject(item: Any): Map[String, JsonApiValue] = {
    val reflectData = cacheReflectData(item)
    reflectData.vars.flatMap { case (name, field) =>
      val fieldValue = field.mirror.bind(item).get
      fieldValue match {
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
      case null => null
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
        .map { field => field.getter.name.toString -> MirrorFieldDesc(ViewMappingInfo.getTermSymbolDesc(mirror, field), reflectItem.reflectField(field)) }
        .toMap

      reflectCache += (itemClass -> ReflectData(mirror, reflectItemClass.typeSignature, vars))
    }
    reflectCache(itemClass)
  }
}

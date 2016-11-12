package com.skn.api.view.model.mapper

import java.time.{LocalDate, LocalDateTime}

import com.skn.api.view.exception.ParsingException
import com.skn.api.view.jsonapi.JsonApiPlayModel.{Data, ObjectKey}
import com.skn.api.view.jsonapi.JsonApiValueModel.{JsonApiBoolean, JsonApiNumber, JsonApiString, JsonApiValue}
import com.skn.api.view.model._
import com.skn.api.view.model.data._
import org.slf4j.LoggerFactory

import scala.reflect.ClassTag
import scala.reflect.runtime.{ universe => ru }

/**
  * Created by Sergey on 12.11.2016.
  */
class DefaultViewReader extends ViewReader {
  val logger = LoggerFactory.getLogger(classOf[ViewReader])

  private var fromStringMethodsCache = Map[ru.Type, ViewValueFactory[_]]()

  /**
    * Contains values, that need pass to constructor and that may be fill after creation
    * @param params - variables, that may be fill after
    * @param constructorParams - constructor arguments
    * @param objectType - type of described object
    * setters - mirrors for settings, is var because caches during invocations
    */
  private case class CreateObjectDescription(mirror: ru.Mirror,
                                             objectType: ru.Type,
                                             constructorMirror: ru.MethodMirror,
                                             constructorParams: Seq[FieldDesc],
                                             params: Seq[FieldDesc]) {
    var setters = Map[ru.MethodSymbol, ru.MethodMirror]()
  }

  /** descriptions for non-simple objects, that must be create via constructor and setters */
  private var createDescriptionsCache = Map[ru.Type, CreateObjectDescription]()
  private var tagTypeCache = Map[ClassTag[_], ru.Type]()
  /** Cache for random types descriptions, like String, Seq[String], etc */
  private var typesDescCache = Map[ru.Type, FieldDesc]()

  /**
    * Entry point for reading, convert data to specified type V. V is required and must be set
    * @param data - data of object
    * @param classTag - V type description
    * @tparam V - result type
    * @return ViewItem created by data
    */
  def read[V <: ViewItem](data: Data)(implicit classTag: ClassTag[V]): V = {
    val createObjectDescription = cacheCreateDescription(classTag)
    val constructorArgs = readFields(createObjectDescription.mirror, createObjectDescription.constructorParams, data)
    val args = readFields(createObjectDescription.mirror, createObjectDescription.params, data)
    constructObject(createObjectDescription, createObjectDescription.objectType, constructorArgs.map { case (k, v) => v }, args.toMap).asInstanceOf[V]
  }

  private def readFields(mirror: ru.Mirror, fieldsDesc: Seq[FieldDesc], data: Data): Seq[(ru.TermSymbol, Any)] = {
    fieldsDesc.map { desc =>
      desc.fieldSymbol.asTerm -> (desc match {
        case d if d.isOption => readField(mirror, desc, data) match {
          case None => None
          case r: Any => Some(r)
        }
        case _ => readField(mirror, desc, data)
      })
    }
  }

  private def readField(mirror: ru.Mirror, fieldDesc: FieldDesc, data: Data): Any = {
    fieldDesc match {
      case f: LinkFieldDesc if f.isSeq => readRelationship(fieldDesc, data, readSeqRelationship)
      case f: LinkFieldDesc => readRelationship(fieldDesc, data, readOneRelationship)
      case _ => readAttribute(mirror, fieldDesc, data)
    }
  }

  private def readRelationship(desc: FieldDesc, data: Data, reader: Seq[ObjectKey] => Any): Any = {
    val relationshipName = getFieldName(desc)
    val res = data.relationships match {
      case Some(relationships) => relationships.get(relationshipName) match {
        case Some(relationship) => relationship.data
        case None => None
      }
      case None => None
    }
    res match {
      case Some(relationship) => reader(relationship)
      case None if !desc.isOption => throw ParsingException("Not found relationship field " + relationshipName)
      case _ => None
    }
  }

  private def readAttribute(mirror: ru.Mirror, desc: FieldDesc, data: Data): Any = {
    val fieldName = getFieldName(desc)
    val res = data.attributes match {
      case Some(attributes) => attributes.get(fieldName)
      case None => None
    }
    res match {
      case Some(attribute) => fromJsValue(mirror, cacheFieldDesc(mirror, desc.unpackType), attribute)
      case None if !desc.isOption => throw ParsingException("Not found attribute with name " + fieldName)
      case _ => None
    }
  }

  private def readOneRelationship[V <: ViewItem](keys: Seq[ObjectKey]): ViewLink[V] ={
    if (keys.tail.nonEmpty) throw ParsingException("Expected one elt data in " + keys)
    new ViewLink[V](keys.head)
  }

  private def readSeqRelationship[V <: ViewItem](keys: Seq[ObjectKey]): Seq[ViewLink[V]] =
    keys.map(key => new ViewLink[V](key))

  private def getFieldName(desc: FieldDesc) = desc.fieldSymbol.name.toString

  private abstract class FieldTypeMapping[T]() {
    def toJsValue(t: T): JsonApiValue
    def fromJsValue(jsValue: JsonApiValue): T
  }

  private val fieldsMapping = Map[Class[_], FieldTypeMapping[_]](
    classOf[String] -> new FieldTypeMapping[String]() {
      override def fromJsValue(jsValue: JsonApiValue): String = jsValue.as[String]
      override def toJsValue(t: String): JsonApiValue = JsonApiString(t)},
    classOf[BigDecimal] -> new FieldTypeMapping[BigDecimal]() {
      override def fromJsValue(jsValue: JsonApiValue): BigDecimal = jsValue.as[BigDecimal]
      override def toJsValue(t: BigDecimal): JsonApiValue = JsonApiNumber(t)
    },
    classOf[Int] -> new FieldTypeMapping[Int]() {
      override def fromJsValue(jsValue: JsonApiValue): Int = jsValue.as[BigDecimal].intValue()
      override def toJsValue(t: Int): JsonApiValue = JsonApiNumber(t)
    },
    classOf[Long] -> new FieldTypeMapping[Long] {
      override def fromJsValue(jsValue: JsonApiValue): Long = jsValue.as[BigDecimal].longValue()
      override def toJsValue(t: Long): JsonApiValue = JsonApiNumber(t)
    },
    classOf[Double] -> new FieldTypeMapping[Double]() {
      override def fromJsValue(jsValue: JsonApiValue): Double = jsValue.as[BigDecimal].doubleValue()
      override def toJsValue(t: Double): JsonApiValue = JsonApiNumber(t)
    },
    classOf[Boolean] -> new FieldTypeMapping[Boolean]() {
      override def fromJsValue(jsValue: JsonApiValue): Boolean = jsValue.as[Boolean]
      override def toJsValue(t: Boolean): JsonApiValue = JsonApiBoolean(t)
    },
    // some date types, for more types define ViewValue wrappers
    classOf[LocalDate] -> new FieldTypeMapping[LocalDate]() {
      override def fromJsValue(jsValue: JsonApiValue): LocalDate = LocalDate.parse(jsValue.as[String])
      override def toJsValue(t: LocalDate): JsonApiValue = JsonApiString(t.toString)
    },
    classOf[LocalDateTime] -> new FieldTypeMapping[LocalDateTime]() {
      override def fromJsValue(jsValue: JsonApiValue): LocalDateTime = LocalDateTime.parse(jsValue.as[String])
      override def toJsValue(t: LocalDateTime): JsonApiValue = JsonApiString(t.toString)
    }
  )

  private def fromJsValue(mirror: ru.Mirror, fieldDesc: FieldDesc, jsValue: JsonApiValue): Any = {
    val result = fieldDesc match {
      case d: ValueFieldDesc =>
        cacheFromStringMethod(mirror, fieldDesc.unpackType).fromString(jsValue.as[String])

      case d if d.isSeq =>
        val jsArray = jsValue.as[Seq[JsonApiValue]]
        val seqType = cacheFieldDesc(mirror, fieldDesc.unpackType)
        jsArray.map(value => fromJsValue(mirror, seqType, value))

      case d: AttributeFieldDesc =>
        val fieldMapper = fieldsMapping.get(d.unpackClass)
        fieldMapper match {
          case Some( mapper) => mapper.fromJsValue(jsValue)
          case None =>
            val jsObject = jsValue.as[Map[String, JsonApiValue]]
            createObject(mirror, fieldDesc.unpackType, jsObject)
        }
    }

    if (fieldDesc.isOption)
      Some(result)
    else
      result
  }

  private def constructObject(desc: CreateObjectDescription,
                              objectType: ru.Type,
                              constructorArgs: Seq[Any],
                              variables: Map[ru.TermSymbol, Any]): Any = {
    val result = desc.constructorMirror(constructorArgs: _*)
    variables.foreach { case (key, value) =>
      val setter = key.setter.asMethod
      if (!desc.setters.contains(setter))
        desc.setters += setter -> desc.mirror.reflect(result).reflectMethod(key.setter.asMethod)
      desc.setters(setter).apply(value)
    }
    result
  }

  private def createObject(mirror: ru.Mirror, objectType: ru.Type, jsObject: Map[String, JsonApiValue]): Any = {
    val desc = cacheCreateDescription(mirror, objectType)
    val constructorArgs = desc.constructorParams.map(arg =>
      fromJsValue(mirror, arg,
        jsObject(arg.fieldSymbol.name.toString)) )
    val variables = desc.params.map(variable =>
        variable.fieldSymbol.asTerm -> fromJsValue(mirror, variable, jsObject(getFieldName(variable))))
      .toMap
    constructObject(desc, objectType, constructorArgs, variables)
  }

  // caching methods
  private def cacheFromStringMethod(mirror: ru.Mirror, objectType: ru.Type): ViewValueFactory[_] =
  {
    if(!fromStringMethodsCache.contains(objectType)) {
      val moduleMirror = mirror.reflectModule(objectType.typeSymbol.companion.asModule)
      fromStringMethodsCache += objectType -> moduleMirror.instance.asInstanceOf[ViewValueFactory[_ <: ViewValue]]
    }
    fromStringMethodsCache(objectType)
  }

  private def cacheCreateDescription(classTag: ClassTag[_]): CreateObjectDescription = {
    val objectType = cacheTagType(classTag)
    if (!createDescriptionsCache.contains(objectType)) {
      val mirror = ru.runtimeMirror(classTag.runtimeClass.getClassLoader)
      cacheCreateDescription(mirror, objectType)
    }
    createDescriptionsCache(objectType)
  }

  private def cacheCreateDescription(mirror: ru.Mirror, objectType: ru.Type) = {
    if (!createDescriptionsCache.contains(objectType)) {
      val constructor = objectType.typeSymbol.asClass.primaryConstructor.asMethod
      var constructorParams = List[FieldDesc]()
      if (constructor.paramLists.nonEmpty)
        constructorParams = constructor.paramLists.head.map(param => ViewMappingInfo.getTermSymbolDesc(mirror, param.asTerm))
      val params = objectType.decls
        .filter(_.isTerm)
        .map(_.asTerm)
        .filter(_.isVar)
        .filter(variable => constructorParams.exists(arg => arg.equals(variable)))
        .map(param => ViewMappingInfo.getTermSymbolDesc(mirror, param))
        .toSeq
      val constructorMirror = mirror.reflectClass(objectType.typeSymbol.asClass)
        .reflectConstructor(constructor)
      createDescriptionsCache += objectType -> CreateObjectDescription(mirror, objectType, constructorMirror, constructorParams, params)
    }
    createDescriptionsCache(objectType)
  }

  private def cacheTagType(tag: ClassTag[_]): ru.Type = {
    if(!tagTypeCache.contains(tag)) {
      val mirror = ru.runtimeMirror(tag.runtimeClass.getClassLoader)
      val objectType = mirror.classSymbol(tag.runtimeClass).typeSignature
      tagTypeCache += tag -> objectType
    }
    tagTypeCache(tag)
  }

  private def cacheFieldDesc(mirror: ru.Mirror, fieldType: ru.Type): FieldDesc = {
    if(!typesDescCache.contains(fieldType))
      typesDescCache += fieldType -> ViewMappingInfo.getTypeSymbolDesc(mirror, fieldType.typeSymbol.asType)
    typesDescCache(fieldType)
  }
}

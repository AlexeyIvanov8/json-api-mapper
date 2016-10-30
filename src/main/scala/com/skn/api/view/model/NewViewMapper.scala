package com.skn.api.view.model

import java.time.{LocalDate, LocalDateTime}

import com.skn.api.view.exception.ParsingException
import com.skn.api.view.jsonapi.JsonApiPlayModel.{Data, ObjectKey, Relationship}
import com.skn.api.view.jsonapi.JsonApiValueModel.{JsonApiBoolean, JsonApiNumber, JsonApiString, JsonApiValue}
import org.slf4j.LoggerFactory

import scala.reflect.ClassTag
import scala.reflect.runtime.{universe => ru}
/**
  * Created by Sergey on 28.10.2016.
  */
class NewViewMapper
{
  val logger = LoggerFactory.getLogger(classOf[NewViewMapper])

  val OptionType = ru.typeOf[Option[_]]
  val ViewLinkType = ru.typeOf[ViewLink[_]]
  val ViewValueType = ru.typeOf[ViewValue]
  val SeqType = ru.typeOf[Seq[_]]


  def read[V <: ViewItem](data: Data)(implicit classTag: ClassTag[V]): V =
  {
    val mirror = ru.runtimeMirror(classTag.runtimeClass.getClassLoader)
    val objectType = mirror.classSymbol(classTag.runtimeClass).typeSignature
    val createObjectDescription = cacheCreateDescription(objectType, classTag)

    val constructorArgs = readFields(createObjectDescription.mirror, createObjectDescription.constructorParams, data)
    val args = readFields(createObjectDescription.mirror, createObjectDescription.params, data)
    constructObject(createObjectDescription.mirror, objectType, constructorArgs.map { case (k, v) => v }, args.toMap).asInstanceOf[V]
  }

  def readFields(mirror: ru.Mirror, fieldMirrors: Seq[ru.TermSymbol], data: Data): Seq[(ru.TermSymbol, Option[_])] =
  {
    fieldMirrors.map { fieldMirror =>
      val fieldName = getFieldName(fieldMirror)
      fieldMirror -> (fieldMirror.typeSignature match {
        case v if v <:< OptionType => readField(mirror, data, v.typeArgs.head, fieldName)
        case _  => readField(mirror, data, fieldMirror.typeSignature, fieldName)
      })
    }
  }

  def readField(mirror: ru.Mirror, data: Data, field: ru.Type, fieldName: String): Option[_] =
  {
    field match {
      case v if v <:< ViewLinkType => readRelationship(data, fieldName)
      case v if v <:< SeqType && v.typeArgs.head <:< ViewLinkType => readRelationships(data, fieldName)
      case _ => readAttribute(mirror, data, field, fieldName)
    }
  }

  def getRelationshipData(data: Data, relationshipName: String): Option[Seq[ObjectKey]] =
  {
    data.relationships match {
      case Some(relationships) => relationships.get(relationshipName) match {
          case Some(relationship) => relationship.data
          case None => None
        }
      case None => None
    }
  }

  def readAttribute(mirror: ru.Mirror, data: Data, mappedType: ru.Type, fieldName: String): Option[_] = {
    data.attributes.get.get(fieldName).map { v =>
      logger.info("begin process attr "+fieldName)
      fromJsValue(mappedType, v, mirror) }
  }

  def readRelationship[V <: ViewItem](data: Data, fieldName: String): Option[ViewLink[V]] =
    getRelationshipData(data, fieldName).map { keys =>
      if (keys.tail.nonEmpty) throw ParsingException("Expected one elt data in " + keys)
      new ViewLink[V](keys.head)
    }

  def readRelationships[V <: ViewItem](data: Data, fieldName: String): Option[Seq[ViewLink[V]]] = {
    getRelationshipData(data, fieldName).map { keys =>
      keys.map(key => new ViewLink[V](key))
    }
  }

  def getFieldName(field: ru.TermSymbol) = field.name.toString

  /**
    * Contains values, that need pass to constructor and that may be fill after creation
    * @param params - variables, that may be fill after
    * @param constructorParams - constructor arguments
    */
  case class CreateObjectDescription(mirror: ru.Mirror, constructorParams: Seq[ru.TermSymbol], params: Seq[ru.TermSymbol])
  var createDescriptionsCache = Map[ru.Type, CreateObjectDescription]()

  private def cacheCreateDescription(objectType: ru.Type, classTag: ClassTag[_]): CreateObjectDescription = {
    if (!createDescriptionsCache.contains(objectType)) {
      val mirror = ru.runtimeMirror(classTag.runtimeClass.getClassLoader)
      val constructor = objectType.typeSymbol.asClass.primaryConstructor.asMethod
      var constructorParams = List[ru.TermSymbol]()
      if (constructor.paramLists.nonEmpty)
        constructorParams = constructor.paramLists.head.map(param => param.asTerm)
      val params = objectType.decls
        .filter(_.isTerm)
        .map(_.asTerm)
        .filter(_.isVar).filter(variable => constructorParams.exists(arg => arg.equals(variable)))
        .toSeq
      createDescriptionsCache += objectType -> CreateObjectDescription(mirror, constructorParams, params)
    }
    createDescriptionsCache(objectType)
  }

  abstract class FieldTypeMapping[T]()
  {
    def toJsValue(t: T): JsonApiValue
    def fromJsValue(jsValue: JsonApiValue): T
  }

  val fieldsMapping = Map[ru.Type, FieldTypeMapping[_]](
    ru.typeOf[String] -> new FieldTypeMapping[String]() {
      override def fromJsValue(jsValue: JsonApiValue): String = jsValue.as[String]
      override def toJsValue(t: String): JsonApiValue = JsonApiString(t)},
    ru.typeOf[BigDecimal] -> new FieldTypeMapping[BigDecimal]() {
      override def fromJsValue(jsValue: JsonApiValue): BigDecimal = jsValue.as[BigDecimal]
      override def toJsValue(t: BigDecimal): JsonApiValue = JsonApiNumber(t)
    },
    ru.typeOf[Int] -> new FieldTypeMapping[Int]() {
      override def fromJsValue(jsValue: JsonApiValue): Int = jsValue.as[BigDecimal].intValue()
      override def toJsValue(t: Int): JsonApiValue = JsonApiNumber(t)
    },
    ru.typeOf[scala.Long] -> new FieldTypeMapping[Long] {
      override def fromJsValue(jsValue: JsonApiValue): Long = jsValue.as[BigDecimal].longValue()
      override def toJsValue(t: Long): JsonApiValue = JsonApiNumber(t)
    },
    ru.typeOf[Double] -> new FieldTypeMapping[Double]() {
      override def fromJsValue(jsValue: JsonApiValue): Double = jsValue.as[BigDecimal].doubleValue()
      override def toJsValue(t: Double): JsonApiValue = JsonApiNumber(t)
    },
    ru.typeOf[Boolean] -> new FieldTypeMapping[Boolean]() {
      override def fromJsValue(jsValue: JsonApiValue): Boolean = jsValue.as[Boolean]
      override def toJsValue(t: Boolean): JsonApiValue = JsonApiBoolean(t)
    },
    // some date types, for more types define ViewValue wrappers
    ru.typeOf[LocalDate] -> new FieldTypeMapping[LocalDate]() {
      override def fromJsValue(jsValue: JsonApiValue): LocalDate = LocalDate.parse(jsValue.as[String])
      override def toJsValue(t: LocalDate): JsonApiValue = JsonApiString(t.toString)
    },
    ru.typeOf[LocalDateTime] -> new FieldTypeMapping[LocalDateTime]() {
      override def fromJsValue(jsValue: JsonApiValue): LocalDateTime = LocalDateTime.parse(jsValue.as[String])
      override def toJsValue(t: LocalDateTime): JsonApiValue = JsonApiString(t.toString)
    }
  )

  private def fromJsValue(mappedType: ru.Type, jsValue: JsonApiValue, mirror: ru.Mirror): Any =
  {
    val isOption = mappedType <:< OptionType
    val fieldType = if(isOption) mappedType.typeArgs.head else mappedType
    val fieldMapper = fieldsMapping.find { case (t, mapper) => fieldType=:=t }

    val result = fieldMapper match {
      case Some((t, mapper)) => mapper.fromJsValue(jsValue)
      case None =>
        fieldType match {
          case t if t <:< ViewValueType =>
            cacheFromStringMethod(mirror, fieldType).fromString(jsValue.as[String])

          case t if t <:< SeqType =>
            val jsArray = jsValue.as[Seq[JsonApiValue]]
            val seqType = fieldType.typeArgs.head
            jsArray.map(value => fromJsValue(seqType, value, mirror))

          case _ =>
            logger.info("Field "+fieldType.typeSymbol.name+" detect as jsObject")
            val jsObject = jsValue.as[Map[String, JsonApiValue]]
            createObject(mirror, fieldType, jsObject)
        }
    }
    if(isOption)
      Some(result)
    else
      result
  }

  private def constructObject(mirror: ru.Mirror,
                              objectType: ru.Type,
                              constructorArgs: Seq[Option[_]],
                              variables: Map[ru.TermSymbol, Option[_]]): Any = {
    val constructorMirror =  mirror.reflectClass(objectType.typeSymbol.asClass)
      .reflectConstructor(objectType.typeSymbol.asClass.primaryConstructor.asMethod)
    val result = constructorMirror(constructorArgs.map(arg => arg.get): _*)
    variables.foreach { case (key, value) =>
      mirror.reflect(result).reflectMethod(key.setter.asMethod).apply(value.get) }
    result
  }

  private def createObject(mirror: ru.Mirror, objectType: ru.Type, jsObject: Map[String, JsonApiValue]): Any = {
    val desc = cacheCreateDescription(mirror, objectType)
    val constructorArgs = desc.constructorParams.map(arg =>
      Some(fromJsValue(arg.typeSignature, jsObject(arg.name.toString), mirror)) )
    val variables = desc.params.map(variable =>
      variable -> Some(fromJsValue(variable.typeSignature, jsObject(variable.name.toString), mirror)) )
      .toMap
    constructObject(mirror, objectType, constructorArgs, variables)
  }

  var fromStringMethodsCache = Map[ru.Type, ViewValueFactory[_]]()
  def cacheFromStringMethod(mirror: ru.Mirror, objectType: ru.Type): ViewValueFactory[_] =
  {
    if(!fromStringMethodsCache.contains(objectType)) {
      val moduleMirror = mirror.reflectModule(objectType.typeSymbol.companion.asModule)
      fromStringMethodsCache += objectType -> moduleMirror.instance.asInstanceOf[ViewValueFactory[_ <: ViewValue]]
    }
    fromStringMethodsCache(objectType)
  }

  private def cacheCreateDescription(classTag: ClassTag[_], objectType: ru.Type): CreateObjectDescription =
    cacheCreateDescription(ru.runtimeMirror(classTag.runtimeClass.getClassLoader), objectType)

  private def cacheCreateDescription(mirror: ru.Mirror, objectType: ru.Type) = {
    if (!createDescriptionsCache.contains(objectType)) {
      val constructor = objectType.typeSymbol.asClass.primaryConstructor.asMethod
      var constructorArgs = List[ru.TermSymbol]()
      if (constructor.paramLists.nonEmpty)
        constructorArgs = constructor.paramLists.head.map(param => param.asTerm)
      val vars = objectType.decls
        .filter(_.isTerm)
        .map(_.asTerm)
        .filter(_.isVar).filter(variable => constructorArgs.exists(arg => arg.equals(variable)))
        .toSeq
      createDescriptionsCache += objectType -> CreateObjectDescription(mirror, constructorArgs, vars)
    }
    createDescriptionsCache(objectType)
  }
}

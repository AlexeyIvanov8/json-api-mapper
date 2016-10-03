package com.skn.api.view.jsonapi

import com.skn.api.version.ApiVersion
import com.skn.api.view.jsonapi.JsonApiPalyModel.{Attributes, Data, Error, JsonApiInfo, JsonPointer, Link, Meta, ObjectKey, Related, Relationship, Relationships, RootObject, Source}
import play.api.libs.json._
import com.skn.api.view.jsonapi.JsonApiValueFormat._
import com.skn.api.view.jsonapi.JsonApiValueModel.JsonApiValue
/**
*
* Created by Sergey on 02.10.2016.
*/
object JsonApiPlayFormat
{

  def writeOption[T](option: Option[T])(implicit tWrites: Writes[T]) = Json.toJson( option.map { value => Json.toJson(value)(tWrites) } )

  val metaFormat = new Format[Meta]
  {
    def writes(meta: Meta) = JsObject( for((key, value) <- meta) yield key -> Json.toJson(value) )//convertedValueToJson(value)) )
    def reads(json: JsValue) = JsSuccess( json.as[Map[String, JsonApiValue]] )
  }

  val attributesFormat = new Format[Attributes]
  {
    def writes(attributes: Attributes) = JsObject( for( (key, value) <- attributes ) yield key -> Json.toJson(value) )
    def reads(json: JsValue) = JsSuccess( json.as[Map[String, JsonApiValue]] )
  }

  val relatedFormat = new Format[Related]
  {
    def writes(related: Related) = JsObject(
      (FieldNames.href -> JsString(related.href)) :: Nil ++
      related.meta.map { meta => FieldNames.meta -> Json.toJson(meta)(metaFormat) }
    )

    def reads(json: JsValue) = JsSuccess(Related(
      (json \ FieldNames.href).as[String],
      (json \ FieldNames.meta).asOpt[Meta](metaFormat)
    ))
  }

  val linksFormat = new Format[Link]
  {
    def writes(link: Link) = JsObject(Seq[(String, JsValue)]() ++
      link.self.map { FieldNames.self -> JsString(_) } ++
      link.related.map { FieldNames.related -> Json.toJson(_)(relatedFormat) }
    )

    def reads(json: JsValue) = JsSuccess(Link(
      (json \ FieldNames.self).asOpt[String],
      (json \ FieldNames.related).asOpt[Related](relatedFormat)
    ))
  }

  val objectKeyFormat = Json.format[ObjectKey]

  val apiVersionFormat = new Format[ApiVersion]
  {
    def writes(version: ApiVersion) = JsString(version.name)
    def reads(json: JsValue) = JsSuccess(ApiVersion(json.as[String]))
  }

  val jsonApiInfoFormat = new Format[JsonApiInfo]
  {
    def writes(info: JsonApiInfo) = JsObject(Seq(
        FieldNames.version -> writeOption(info.version.map { version => Json.toJson(version)(apiVersionFormat) }),
        FieldNames.meta -> writeOption(info.meta.map { meta => Json.toJson(meta)(metaFormat) })
      ))

    def reads(json: JsValue) = JsSuccess(JsonApiInfo(
        (json \ FieldNames.version).asOpt[ApiVersion](apiVersionFormat),
        (json \ FieldNames.meta).asOpt[Meta](metaFormat)
      ))
  }

  val relationshipFormat = new Format[Relationship]
  {
    def writes(relation: Relationship) = JsObject(
        ((FieldNames.links -> Json.toJson(relation.links)(linksFormat)) ::
        (FieldNames.data -> Json.toJson(relation.data.map{ d => d.map{ key => Json.toJson(key)(objectKeyFormat) } })) :: Nil) ++
        relation.meta.map { FieldNames.meta -> Json.toJson(_)(metaFormat)}.toSeq
      )

    def reads(json: JsValue) = JsSuccess(Relationship(
        (json \ FieldNames.links).as[Link](linksFormat),
        (json \ FieldNames.data).asOpt[Seq[JsValue]]
          .map { seqOpt =>
            seqOpt.map { jsValue => jsValue.as[ObjectKey](objectKeyFormat) }
          },
        (json \ FieldNames.meta).asOpt[Meta](metaFormat)
      ))
  }

  val relationshipsFormat = new Format[Relationships]
  {
    def writes(relationships: Relationships) = JsObject(
        for((key, value) <- relationships) yield key -> Json.toJson(value)(relationshipFormat)
      )

    def reads(json: JsValue) = JsSuccess(
        json.as[Map[String, JsValue]].map { case (key, value) => key -> value.as[Relationship](relationshipFormat) }
      )
  }

  val dataFormat = new Format[Data]
  {
    def writes(data: Data) = JsObject(
        FieldNames.`type` -> Json.toJson(data.key.`type`) :: Nil ++
        data.key.id.map { FieldNames.id -> Json.toJson(_) } ++
        data.attributes.map{ attributes => FieldNames.attributes -> Json.toJson(attributes)(attributesFormat) } ++
        data.links.map { links => FieldNames.links -> Json.toJson(links)(linksFormat) } ++
        data.relationships.map { rel => FieldNames.relationships -> Json.toJson(rel)(relationshipsFormat) } ++
        data.meta.map { meta => FieldNames.meta -> Json.toJson(meta)(metaFormat) } )

    def reads(json: JsValue) = JsSuccess(Data(
        ObjectKey((json \ FieldNames.`type`).as[String], (json \ FieldNames.id).asOpt[Long]),
        (json \ FieldNames.attributes).asOpt[Attributes](attributesFormat),
        (json \ FieldNames.links).asOpt[Link](linksFormat),
        (json \ FieldNames.relationships).asOpt[Relationships](relationshipsFormat),
        (json \ FieldNames.meta).asOpt[Meta](metaFormat)
      ))
  }

  val jsonPointerFormat: Format[JsonPointer] = Json.format[JsonPointer]

  val sourceFormat = new Format[Source]
  {
    def writes(source: Source) = JsObject(Seq(
        FieldNames.pointer -> writeOption(source.pointer.map { pointer => Json.toJson(pointer)(jsonPointerFormat) }),
        FieldNames.parameter -> Json.toJson(source.parameter)
      ))

    def reads(json: JsValue) = JsSuccess(Source(
        (json \ FieldNames.pointer).asOpt[JsonPointer](jsonPointerFormat),
        (json \ FieldNames.parameter).asOpt[String]
      ))
  }

  val errorFormat = new Format[Error]
  {
    def writes(error: Error) = JsObject(
        FieldNames.id -> Json.toJson(error.id) ::
        FieldNames.status -> Json.toJson(error.status) ::
        FieldNames.code -> Json.toJson(error.code) ::
        FieldNames.title -> Json.toJson(error.title) ::
        FieldNames.detail -> Json.toJson(error.detail) :: Nil ++
        error.about.map { about => FieldNames.about -> Json.toJson(about)(linksFormat) } ++
        error.source.map { source => FieldNames.source -> Json.toJson(source)(sourceFormat) } ++
        error.meta.map { meta => FieldNames.meta -> Json.toJson(meta)(metaFormat) })

    def reads(json: JsValue) = JsSuccess(Error(
        (json \ FieldNames.id).asOpt[Long],
        (json \ FieldNames.about).asOpt[Link](linksFormat),
        (json \ FieldNames.status).asOpt[Int],
        (json \ FieldNames.code).asOpt[String],
        (json \ FieldNames.title).asOpt[String],
        (json \ FieldNames.detail).asOpt[String],
        (json \ FieldNames.source).asOpt[Source](sourceFormat),
        (json \ FieldNames.meta).asOpt[Meta](metaFormat)
      ))
  }

def writeSeqToArrayOrOne[T](seq: Seq[T])(implicit tFormat: Format[T]) =
{
if(seq.nonEmpty)
seq.tail match {
case Nil => Json.toJson(seq.head)(tFormat)
case _ => Json.toJson(seq.map { value => Json.toJson(value)(tFormat) })
}
else
Json.arr()
}

def readOptSeqOrOne[T](json: JsValue, fieldName: String, format: Format[T]): Option[Seq[T]] =
(json \ fieldName).toOption.map {
case fieldValue: JsArray => fieldValue.as[Seq[JsValue]].map { element => element.as[T](format) }
case fieldValue: JsValue => fieldValue.as[T](format) :: Nil
}

val rootFormat = new Format[RootObject]
  {
    def writes(root: RootObject) = JsObject(Seq[(String, JsValue)]() ++
        root.data.map { data => FieldNames.data -> writeSeqToArrayOrOne(data)(dataFormat) } ++ //Json.toJson(data.map { d => Json.toJson(d)(dataFormat) }) } ++
        root.errors.map { errors => FieldNames.errors -> writeSeqToArrayOrOne(errors)(errorFormat) } ++ //Json.arr(errors.map { error => Json.toJson(error)(errorFormat) }) } ++
        root.meta.map{ meta => FieldNames.meta -> Json.toJson(meta)(metaFormat) } ++
        root.jsonApi.map { jsonapi => FieldNames.jsonApi -> Json.toJson(jsonapi)(jsonApiInfoFormat) } ++
        root.links.map { links =>  FieldNames.links -> writeSeqToArrayOrOne(links)(linksFormat) } ++ //Json.arr(links.map { link => Json.toJson(link)(linksFormat) }) } ++
        root.included.map { included => FieldNames.included -> writeSeqToArrayOrOne(included)(dataFormat) } //Json.arr(included.map { include => Json.toJson(include)(dataFormat) }) }
      )


    def reads(json: JsValue) = JsSuccess(RootObject(
        readOptSeqOrOne(json, FieldNames.data, dataFormat),
        readOptSeqOrOne(json, FieldNames.errors, errorFormat),
        (json \ FieldNames.meta).asOpt[Meta](metaFormat),
        (json \ FieldNames.jsonApi).asOpt[JsonApiInfo](jsonApiInfoFormat),
        readOptSeqOrOne(json, FieldNames.links, linksFormat),
        readOptSeqOrOne(json, FieldNames.included, dataFormat)
      ))
  }
}

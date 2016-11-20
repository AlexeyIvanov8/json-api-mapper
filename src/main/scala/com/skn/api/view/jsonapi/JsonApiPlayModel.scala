package com.skn.api.view.jsonapi

import com.skn.api.version.ApiVersion
import com.skn.api.view.jsonapi.JsonApiValueModel.JsonApiValue

/**
* Created by Sergey on 01.10.2016.
*/
object JsonApiPlayModel {

  type Attributes = Map[String, JsonApiValue]

  implicit def Attributes(values: (String, JsonApiValue)*): Attributes = Map(values: _*)

  type Meta = Map[String, JsonApiValue]

  implicit def Meta(values: (String, JsonApiValue)*): Meta = Map(values: _*)

  case class ObjectKey(`type`: String, id: Option[Long] = None)

  implicit object ObjectKey {
    def apply(`type`: String, id: Long): ObjectKey = ObjectKey(`type`, Some(id))
  }

  case class JsonApiInfo(version: Option[ApiVersion] = None, meta: Option[Meta] = None)

  case class Related(href: String, meta: Option[Meta] = None)

  case class Link(self: Option[String] = None, related: Option[Related] = None)

  implicit object Link {
    def apply(self: String): Link = Link(Some(self))

    def apply(related: Related): Link = Link(None, Some(related))
  }

  case class Relationship(links: Link, data: Option[Seq[ObjectKey]] = None, meta: Option[Meta] = None)

  implicit object Relationship {
    def apply(links: Link, data: Seq[ObjectKey]): Relationship = Relationship(links, Some(data))
  }

  type Relationships = Map[String, Relationship]

  implicit def Relationships(values: (String, Relationship)*): Relationships = Map(values: _*)

  case class Data(
                   key: ObjectKey,
                   attributes: Option[Attributes] = None,
                   links: Option[Link] = None,
                   relationships: Option[Relationships] = None,
                   meta: Option[Meta] = None)

  case class JsonPointer(values: Seq[String]) {
    override def toString = "/" + values.reduce((left, right) => left + "/" + right)
  }

  case class Source(pointer: Option[JsonPointer], parameter: Option[String])

  case class Error(
                    id: Option[Long] = None,
                    about: Option[Link] = None,
                    status: Option[Int] = None,
                    code: Option[String] = None,
                    title: Option[String] = None,
                    detail: Option[String] = None,
                    source: Option[Source] = None,
                    meta: Option[Meta] = None)

  case class DataSeq(seq: Seq[Data])

  implicit def dataSeqConvert(seq: Seq[Data]): DataSeq = DataSeq(seq)

  case class ErrorsSeq(seq: Seq[Error])

  implicit def errorsSeqConvert(seq: Seq[Error]): ErrorsSeq = ErrorsSeq(seq)

  case class RootObject(data: Option[Seq[Data]] = None,
                         errors: Option[Seq[Error]] = None,
                         meta: Option[Meta] = None,
                         jsonApi: Option[JsonApiInfo] = None,
                         links: Option[Seq[Link]] = None,
                         included: Option[Seq[Data]] = None)

  implicit object RootObject {
    def apply(data: DataSeq): RootObject = RootObject(Some(data.seq))

    def apply(errors: ErrorsSeq): RootObject = RootObject(None, Some(errors.seq))
  }

}

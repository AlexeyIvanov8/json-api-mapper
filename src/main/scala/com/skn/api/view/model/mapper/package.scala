package com.skn.api.view.model

import com.skn.api.view.jsonapi.JsonApiModel.{Data, RootObject}

import scala.reflect.ClassTag

/**
  * Created by Sergey on 12.11.2016.
  */
package object mapper {

  trait ViewReader {
    def read[V <: ViewItem](data: Data)(implicit classTag: ClassTag[V]): V
  }

  /**
    * Write View model to Json api representation
    */
  trait ViewWriter {
    val linkDefiner: LinkDefiner
    def write[T <: ViewItem](item: T): Data
  }

  /** Next classes combine View model mapping and Json api mapping */
  class JsonApiViewReader(val viewReader: ViewReader,
                          val jsonReader: String => RootObject) {
    def read[V <: ViewItem](json: String)(implicit classTag: ClassTag[V]): Option[V] = {
      val root = jsonReader(json)
      root.data.flatMap { dataSeq => dataSeq match {
        case Seq(data) => Some(viewReader.read(data))
        case _ => None
      }}
    }
  }

  class JsonApiViewWriter(val viewWriter: ViewWriter,
                          val jsonMapper: RootObject => String) {
    def write[V <: ViewItem](item: V): String = {
      jsonMapper(RootObject(viewWriter.write[V](item) :: Nil))
    }
  }
}

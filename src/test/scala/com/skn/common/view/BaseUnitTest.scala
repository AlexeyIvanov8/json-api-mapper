package com.skn.common.view

import java.io.{ByteArrayOutputStream, OutputStream}
import java.time.LocalDateTime

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.core.JsonParser.{Feature, NumberType}
import com.fasterxml.jackson.core._
import com.fasterxml.jackson.databind.Module.SetupContext
import com.fasterxml.jackson.databind.`type`.{TypeFactory, TypeModifier}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.module.{SimpleDeserializers, SimpleModule, SimpleSerializers}
import com.fasterxml.jackson.databind.node.{JsonNodeType, ObjectNode, TextNode}
import com.fasterxml.jackson.databind.ser.{BeanSerializerModifier, Serializers}
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JacksonModule, OptionModule, TupleModule}
import com.skn.api.view.jsonapi.JsonApiMapper
import com.skn.api.view.jsonapi.JsonApiPlayModel.{ObjectKey, RootObject}
import com.skn.api.view.jsonapi.JsonApiValueModel.{JsonApiArray, JsonApiNumber, JsonApiObject, JsonApiOneValue, JsonApiString, JsonApiValue}
import com.skn.api.view.model.ViewLink
import com.skn.api.view.model.mapper.{DefaultViewWriter, JsonapiViewWriter, SimpleLinkDefiner}
import com.typesafe.scalalogging._
import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

class BaseUnitTest extends FlatSpec with Matchers
{
  protected val logger = Logger(LoggerFactory.getLogger(classOf[BaseUnitTest]))
  val mapper = new JsonApiMapper

  def mappers = new {
    val jacksonMapper = new ObjectMapper()

    var module = new DefaultScalaModule {
      override def setupModule(context: SetupContext): Unit = {
        val serializers = new SimpleSerializers
        serializers.addSerializer(classOf[JsonApiValue], (value: JsonApiValue, gen: JsonGenerator, serializers: SerializerProvider) => {
          value match {
            case v: JsonApiOneValue[_] => gen.writeObject(v.value)
            case v: JsonApiArray =>
              gen.writeStartArray()
              for (elt <- v.seq)
                gen.writeObject(elt)
              gen.writeEndArray()
            case v: JsonApiObject =>
              gen.writeObject(v.map)
          }
        })

        val deserializers = new SimpleDeserializers
        deserializers.addDeserializer(classOf[JsonApiValue], (parser: JsonParser, context: DeserializationContext) => {
          val codec = parser.getCodec
          val node = codec.readTree(parser).asInstanceOf[JsonNode]
          node.getNodeType match {
            case JsonNodeType.STRING => JsonApiString(node.asText())
            case JsonNodeType.NUMBER => node.numberType() match {
              case NumberType.BIG_DECIMAL => JsonApiNumber(codec.treeToValue(node, classOf[BigDecimal]))
              case NumberType.DOUBLE => JsonApiNumber(node.asDouble())
              case NumberType.INT => JsonApiNumber(node.intValue())
              case NumberType.LONG => JsonApiNumber(node.longValue())
              case _ => JsonApiNumber(BigDecimal(node.asText()))
            }
            case JsonNodeType.ARRAY =>
              JsonApiArray(for(i: Int <- 0 until node.size()) yield
                codec.treeToValue(node.get(i), classOf[JsonApiValue]))
            case JsonNodeType.OBJECT => JsonApiObject(
              node.asInstanceOf[ObjectNode].fields()
                .asScala
                .map(elt => elt.getKey -> codec.treeToValue(elt.getValue.asInstanceOf[TreeNode], classOf[JsonApiValue]))
                .toMap)
            case _ => JsonApiString(node.asText())
          }
        })
        context.addDeserializers(deserializers)
        context.addSerializers(serializers)
        super.setupModule(context)
      }
    }

    jacksonMapper.setSerializationInclusion(Include.NON_NULL)
    jacksonMapper.setSerializationInclusion(Include.NON_EMPTY)
    jacksonMapper.registerModule(module) //DefaultScalaModule)
    val viewWriter = new DefaultViewWriter(new SimpleLinkDefiner)
    val jsonViewWriter = new JsonapiViewWriter(viewWriter, root => jacksonMapper.writeValueAsString(root))
  }

  def data = new {
    val itemName = "Js string value"

    val item = TestView(itemName,
      5, new Home("TH"), Some(0L),
      Some(new ViewLink(TestLink(ObjectKey("link", 1L), Some(LocalDateTime.now())))),
      Some(CustomObject(Some("customName"), 34423, Some(List(3.4, 4.5)))))
    val itemData = mappers.viewWriter.write(item)
    val itemDataStr = mappers.jsonViewWriter.write(item)
  }
}
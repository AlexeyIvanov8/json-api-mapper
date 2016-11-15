package com.skn.api.view.jsonapi

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.core.JsonParser.NumberType
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser, TreeNode}
import com.fasterxml.jackson.databind.Module.SetupContext
import com.fasterxml.jackson.databind.{DeserializationContext, JsonNode, ObjectMapper, SerializerProvider}
import com.fasterxml.jackson.databind.module.{SimpleDeserializers, SimpleSerializers}
import com.fasterxml.jackson.databind.node.{JsonNodeType, ObjectNode}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.skn.api.view.jsonapi.JsonApiValueModel.{JsonApiArray, JsonApiNumber, JsonApiObject, JsonApiOneValue, JsonApiString, JsonApiValue}

import scala.collection.JavaConverters._

/**
  * Created by Sergey on 15.11.2016.
  */
object JsonApiJacksonFormat {
  val jacksonMapper = init()

  private def init(): ObjectMapper = {
    val mapper = new ObjectMapper()
    val module = new DefaultScalaModule {
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
              JsonApiArray(for (i: Int <- 0 until node.size()) yield
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


    mapper.setSerializationInclusion(Include.NON_NULL)
    mapper.setSerializationInclusion(Include.NON_EMPTY)
    mapper.registerModule(module)

    mapper
  }
}

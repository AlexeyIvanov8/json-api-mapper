package com.skn.api.view.jsonapi

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.core.JsonParser.NumberType
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser, TreeNode}
import com.fasterxml.jackson.databind.Module.SetupContext
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.module.{SimpleDeserializers, SimpleSerializers}
import com.fasterxml.jackson.databind.node.{JsonNodeType, ObjectNode}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.skn.api.view.jsonapi.JsonApiValueModel.{JsonApiArray, JsonApiNumber, JsonApiObject, JsonApiOneValue, JsonApiString, JsonApiValue}

import scala.collection.JavaConverters._

/**
  * Created by Sergey on 15.11.2016.
  */
object JsonApiJacksonFormat {

  val jacksonMapper = createMapper()

  def createMapper(): ObjectMapper = {
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
            case JsonNodeType.NUMBER => node.numberType() match {
              case NumberType.BIG_DECIMAL => JsonApiNumber(BigDecimal(node.asText())) //codec.treeToValue(node, classOf[BigDecimal]))
              case NumberType.DOUBLE => JsonApiNumber(node.asDouble())
              case NumberType.INT => JsonApiNumber(node.intValue())
              case NumberType.LONG => JsonApiNumber(node.longValue())
              case _ => JsonApiNumber(BigDecimal(node.asText()))
            }
            case JsonNodeType.ARRAY =>
              val it = node.elements()
              var seq: List[JsonApiValue] = List[JsonApiValue]()
              while(it.hasNext)
                seq = codec.treeToValue(it.next(), classOf[JsonApiValue]) :: seq
              JsonApiArray(seq.reverse)
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
    mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    mapper.enable(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)

    mapper.registerModule(module)

    mapper
  }
}

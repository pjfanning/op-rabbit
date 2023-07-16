package com.github.pjfanning.op_rabbit

import io.circe.{Decoder, Encoder}
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import java.nio.charset.Charset

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

case class CirceThing(a: Int)
class CirceSupportSpec extends AnyFunSpec with Matchers {
  import CirceSupport._
  import io.circe.generic.auto._
  val u = implicitly[RabbitUnmarshaller[CirceThing]]
  val m = implicitly[RabbitMarshaller[CirceThing]]

  describe("CirceSupport") {
    it("deserializes the provided content") {
      u.unmarshall("""{"a": 5}""".getBytes, Some("application/json"), Some("UTF-8")) should be (CirceThing(5))
    }

    it("interprets no encoding / no contentType as json / UTF8") {
      u.unmarshall("""{"a": 5}""".getBytes, None, None) should be (CirceThing(5))
    }

    it("rejects wrong encoding") {
      a [MismatchedContentType] should be thrownBy {
        u.unmarshall("""{"a": 5}""".getBytes, Some("text"), Some("UTF-8"))
      }
    }

    it("throws an InvalidFormat exception when unmarshalling is unpossible") {
      a [InvalidFormat] should be thrownBy {
        u.unmarshall("""{"a": }""".getBytes, Some("application/json"), Some("UTF-8"))
      }
    }

    it("serializes the provided content") {
      val body = m.marshall(CirceThing(5))
    }

    it("provides the appropriate content headers") {
      val properties = m.setProperties().build
      properties.getContentType should be ("application/json")
      properties.getContentEncoding should be ("UTF-8")
    }
  }
}

package com.github.pjfanning.op_rabbit

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class PublisherSpec extends AnyFunSpec with Matchers {
  describe("Publisher.exchange") {
    it("receives a Concrete exchange definition, and topic key") {
      val exchange = Exchange.direct("hi")
      val publisher = Publisher.exchange(exchange, "hi")
      publisher.exchangeName shouldBe "hi"
      publisher.routingKey shouldBe "hi"
    }
  }
}

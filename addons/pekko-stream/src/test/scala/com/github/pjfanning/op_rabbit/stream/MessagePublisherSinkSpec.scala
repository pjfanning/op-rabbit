package com.github.pjfanning.op_rabbit.stream

import com.github.pjfanning.op_rabbit.{Message, Publisher, Queue, RecoveryStrategy}
import com.github.pjfanning.op_rabbit.helpers.RabbitTestHelpers
import org.apache.pekko.actor._
import org.apache.pekko.stream.scaladsl.{Keep, Sink}
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.Envelope
import com.github.pjfanning.op_rabbit.Directives._
import com.github.pjfanning.op_rabbit.helpers.RabbitTestHelpers
import com.timcharper.acked.AckedSource
import com.spingo.scoped_fixtures.ScopedFixtures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Try}

class MessagePublisherSinkSpec extends AnyFunSpec with Matchers with ScopedFixtures with RabbitTestHelpers {
  implicit val executionContext = ExecutionContext.global

  val _queueName = ScopedFixture[String] { setter =>
    val name = s"test-queue-rabbit-control-${Math.random()}"
    deleteQueue(name)
    val r = setter(name)
    deleteQueue(name)
    r
  }

  trait RabbitFixtures {
    val exceptionReported = Promise[Boolean]
    implicit val errorReporting = new RabbitErrorLogging {
      def apply(name: String, message: String, exception: Throwable, consumerTag: String, envelope: Envelope, properties: BasicProperties, body: Array[Byte]): Unit = {
        exceptionReported.success(true)
      }
    }
    val queueName = _queueName()
    val rabbitControl = actorSystem.actorOf(Props(new RabbitControl))
    val range = (0 to 16)
    val qos = 8
  }

  describe("PublisherSink") {
    it("publishes all messages consumed, and acknowledges the promises") {
      new RabbitFixtures {
        implicit val recoveryStrategy = RecoveryStrategy.limitedRedeliver()

        val (subscription, consumed) = RabbitSource(
          rabbitControl,
          channel(qos),
          consume(queue(queueName, durable = true, exclusive = false, autoDelete = false)),
          body(as[Int])).
          acked.
          take(range.length.toLong).
          toMat(Sink.fold(List.empty[Int]) {
            case (acc, v) =>
              acc ++ List(v)
          })(Keep.both).run

        await(subscription.initialized)

        val data = range map { i => (Promise[Unit], i) }

        val published = AckedSource(data).
          map(Message.queue(_, queueName)).
          runWith(MessagePublisherSink(rabbitControl))

        await(published)
        await(Future.sequence(data.map(_._1.future))) // this asserts that all of the promises were fulfilled
        await(consumed) should be (range)
      }
    }

    it("propagates publish exceptions to promise") {
      new RabbitFixtures {
        val factory = Message.factory(Publisher.queue(Queue.passive("no-existe")))
        val sink = MessagePublisherSink(rabbitControl)

        val data = range map { i => (Promise[Unit], i) }

        val published = AckedSource(data).
          map(Message(_, Publisher.queue(Queue.passive("no-existe")))).
          runWith(sink)

        await(published)
        val Failure(ex) = Try(await(data.head._1.future))
        ex.getMessage should include ("no queue 'no-existe'")
      }
    }
  }


}

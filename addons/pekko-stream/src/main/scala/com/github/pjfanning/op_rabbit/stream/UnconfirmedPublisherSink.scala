package com.github.pjfanning.op_rabbit.stream

import org.apache.pekko.actor.ActorRef
import org.apache.pekko.Done
import org.apache.pekko.pattern.ask
import org.apache.pekko.stream.scaladsl.{Flow, Keep, Sink}
import scala.concurrent.Future
import scala.concurrent.duration._

object UnconfirmedPublisherSink {
  // Publishes messages to RabbitMQ; does not wait for receive confirmation; does not backpressure. Just sends and forgets.
  def apply[T](rabbitControl: ActorRef, messageFactory: MessageForPublicationLike.Factory[T, UnconfirmedMessage], timeoutAfter: FiniteDuration = 30.seconds, qos: Int = 8): Sink[T, Future[Done]] = {
    implicit val akkaTimeout = akka.util.Timeout(timeoutAfter)
    Sink.foreach[T] { payload =>
      rabbitControl ! messageFactory(payload)
    }
  }
}

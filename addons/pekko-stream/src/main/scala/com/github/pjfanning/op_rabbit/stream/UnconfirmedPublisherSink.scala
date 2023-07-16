package com.github.pjfanning.op_rabbit
package stream

import org.apache.pekko.actor.ActorRef
import org.apache.pekko.Done
import org.apache.pekko.pattern.ask
import org.apache.pekko.stream.scaladsl.Sink
import scala.concurrent.Future
import scala.concurrent.duration._

object UnconfirmedPublisherSink {
  // Publishes messages to RabbitMQ; does not wait for receive confirmation; does not backpressure. Just sends and forgets.
  def apply[T](rabbitControl: ActorRef, messageFactory: MessageForPublicationLike.Factory[T, UnconfirmedMessage], timeoutAfter: FiniteDuration = 30.seconds, qos: Int = 8): Sink[T, Future[Done]] = {
    implicit val akkaTimeout: org.apache.pekko.util.Timeout = org.apache.pekko.util.Timeout(timeoutAfter)
    Sink.foreach[T] { payload =>
      rabbitControl ! messageFactory(payload)
    }
  }
}

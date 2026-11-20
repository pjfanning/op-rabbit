package com.github.pjfanning.op_rabbit.helpers

import org.apache.pekko.actor.Actor
import org.apache.pekko.actor.Props
import org.apache.pekko.actor.{ActorRef, ActorSystem}
import org.apache.pekko.pattern.ask
import org.apache.pekko.util.Timeout
import com.rabbitmq.client.Channel
import com.github.pjfanning.op_rabbit.{ClusterConnectionFactory, ConnectionParams, RabbitControl}
import com.github.pjfanning.op_rabbit.{MessageForPublicationLike, RabbitMarshaller, RabbitUnmarshaller}
import com.spingo.scoped_fixtures.ScopedFixtures

import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration._
import scala.language.postfixOps

trait RabbitTestHelpers extends ScopedFixtures {
  implicit val timeout: Timeout = Timeout(5 seconds)
  val killConnection = new MessageForPublicationLike {
    val dropIfNoChannel = true
    def apply(c: Channel): Unit = {
      c.getConnection.close()
    }
  }

  val actorSystemFixture = ScopedFixture[ActorSystem] { setter =>
    val actorSystem = ActorSystem("test")
    val status = setter(actorSystem)
    actorSystem.terminate()
    status
  }
  val rabbitControlFixture = LazyFixture[ActorRef] {
    actorSystemFixture().actorOf(Props[RabbitControl]())
  }

  implicit def actorSystem: ActorSystem = actorSystemFixture()
  def rabbitControl = rabbitControlFixture()
  def await[T](f: Future[T], duration: Duration = timeout.duration) =
    Await.result(f, duration)

  // kills the rabbitMq connection in such a way that the system will automatically recover and reconnect;
  // synchronously waits for the connection to be terminated, and to be re-established
  def reconnect(rabbitMqControl: ActorRef)(implicit actorSystem: ActorSystem): Unit = {
    val connectionActor = await((rabbitMqControl ? RabbitControl.GetConnectionActor).mapTo[ActorRef])

    val done = Promise[Unit]()
    actorSystem.actorOf(Props(new Actor {
      import org.apache.pekko.actor.FSM._
      override def preStart(): Unit = {
        connectionActor ! SubscribeTransitionCallBack(self)
      }

      override def postStop(): Unit = {
        connectionActor ! UnsubscribeTransitionCallBack(self)
      }

      def receive = {
        case CurrentState(ref, state) if state.toString == "Connected" =>
          rabbitMqControl ! killConnection
          // because the connection shutdown cause is seen as isInitiatedByApplication, we need to send the Connect signal again to wake things back up.
          context.become {
            case Transition(ref, from, to) if from.toString == "Connected" && to.toString == "Disconnected" =>
              connectionActor ! com.github.pjfanning.pekko.rabbitmq.ConnectionActor.Connect
              context.become {
                case Transition(ref, from, to) if from.toString == "Disconnected" && to.toString == "Connected" =>
                  done.success(())
                  context stop self
              }
          }
      }
    }))

    await(done.future)
  }

  // Deletes the queue over a dedicated, short-lived connection rather than via rabbitControl, so that test
  // setup/teardown does not depend on the state of the RabbitControl under test (e.g. a publish channel that is
  // mid-recovery, which can silently drop the request and leave the caller waiting on a timeout).
  def deleteQueue(queueName: String): Unit =
    RabbitTestHelpers.deleteQueue(queueName)

  implicit val simpleIntMarshaller: RabbitMarshaller[Int] with RabbitUnmarshaller[Int] = new RabbitMarshaller[Int] with RabbitUnmarshaller[Int] {
    val contentType = "text/plain"
    val contentEncoding = Some("UTF-8")

    def marshall(value: Int) =
      value.toString.getBytes

    def unmarshall(value: Array[Byte], contentType: Option[String], charset: Option[String]) = {
      new String(value).toInt
    }
  }
}

object RabbitTestHelpers {
  private lazy val connectionFactory = {
    val factory = new ClusterConnectionFactory
    ConnectionParams.fromConfig().applyTo(factory)
    factory
  }

  def deleteQueue(queueName: String): Unit = {
    val connection = connectionFactory.newConnection()
    try connection.createChannel().queueDelete(queueName)
    finally connection.close(5000)
  }
}

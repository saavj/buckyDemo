package com.itv.buckydemo

import _root_.fs2.Stream
import cats.effect.IO
import com.itv.bucky._
import com.itv.bucky.decl._
import com.itv.bucky.fs2.{IOAmqpClient, _}
import com.itv.bucky.pattern.requeue.{RequeuePolicy, _}
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.concurrent.Eventually
import org.scalatest.{WordSpec, concurrent}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class Test extends WordSpec with IntegrationSpec with StrictLogging with Eventually {

  override implicit val patienceConfig = PatienceConfig(5 seconds, 1 second)

  "apply" should {

    "consume a message, publish a message" in testFixture { app =>

      DeclarationExecutor(RmqConfig.allDecs, app.amqpClient)

      val testPublisher = app.amqpClient.publisherOf(RmqConfig.incomingPublisherConfig)

      val ourPublisher = app.amqpClient.publisherOf(RmqConfig.outgoingPublisherConfig)

      val ourHandler = new HelloHandler(ourPublisher, _ => IO(true))

      val outgoingHandler = new StubConsumeHandler[IO, Delivery]()
      val outgoingConsumer = app.amqpClient.consumer(RmqConfig.outgoingQueue.name, outgoingHandler)

      val incomingConsumer: Stream[IO, Unit] = rmqHandler(app.amqpClient)(RmqConfig.incomingQueue.name, ourHandler)

      incomingConsumer
        .concurrently(outgoingConsumer)
        .compile
        .drain
        .unsafeRunAsync(_ => ())

      for {
        result <- testPublisher(HelloMessage("Hello"))
      } yield {
        eventually {
          result shouldBe ()
          outgoingHandler.receivedMessages.size shouldBe 1
        }

      }
    }

  }

  private def rmqHandler[T: PayloadUnmarshaller](client: IOAmqpClient)(queueName: QueueName, handler: RequeueHandler[IO, T]): Stream[IO, Unit] =
    RequeueOps(client)
      .requeueHandlerOf[T](
      queueName,
      handler,
      RequeuePolicy(maximumProcessAttempts = 10, 1.seconds),
      implicitly[PayloadUnmarshaller[T]]
    )
}

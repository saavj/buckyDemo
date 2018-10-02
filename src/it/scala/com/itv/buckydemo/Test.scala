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

class Test extends WordSpec with IntegrationSpec with StrictLogging {

  implicit val ePatienceConfig: concurrent.Eventually.PatienceConfig = Eventually.PatienceConfig(5 seconds, 1 second)

  "apply" should {

    "consume a message, publish a message" in testFixture { app =>

      DeclarationExecutor(RmqConfig.allDeclarations, app.amqpClient)

      // used to publish incoming message
      val testPublisher = app.amqpClient.publisherOf(RmqConfig.helloPublisherConfig)

      // the publisher to use in our handler
      val ourPublisher = app.amqpClient.publisherOf(RmqConfig.worldPublisherConfig)

      // The handler that we have created in our service
      val ourHandler = new HelloHandler(ourPublisher, _ => IO.pure(true))

      // the handler and consumer for us to check we've finally published
      val sinkHandler = new StubConsumeHandler[IO, Delivery]()
      val testConsumer: Stream[IO, Unit] = app.amqpClient.consumer(RmqConfig.outgoingQueueName, sinkHandler)

      // our real consumer
      val incomingConsumer: Stream[IO, Unit] = rmqHandler(app.amqpClient)(
        RmqConfig.incomingQueueName,
        ourHandler
      )

      incomingConsumer
        .concurrently(testConsumer)
        .compile.drain.unsafeRunAsync(_ => ())

      for {
        result <- testPublisher.apply(HelloMessage("hello", "ok"))
      } yield {
        result shouldBe(): Unit
        Eventually.eventually {
          // checks HelloHandler has worked and published our ongoing message
          sinkHandler.receivedMessages.size shouldBe 1
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

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

      //todo

      ???

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

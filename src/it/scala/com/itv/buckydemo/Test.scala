package com.itv.buckydemo

import cats.effect.IO
import com.itv.bucky.PayloadMarshaller.StringPayloadMarshaller
import com.itv.bucky._
import com.itv.bucky.fs2._
import com.itv.bucky.decl.{Binding, DeclarationExecutor, Queue}
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.WordSpec
import com.itv.bucky.PublishCommandBuilder._
import scala.concurrent.ExecutionContext.Implicits.global

class Test extends WordSpec with IntegrationSpec with StrictLogging {

  val exchange = ExchangeName("hello")
  val routingKey = RoutingKey("world")
  val queue = Queue(QueueName("buckydemo.helloworld"))


  "apply" should {

    "consume a message, publish a message" in testFixture { app =>
      val binding = Binding(exchange, queue.name, routingKey, Map.empty)

      val handler: StubConsumeHandler[IO, Delivery] = new StubConsumeHandler[IO, Delivery]()

      DeclarationExecutor(
        List(queue) ++ List(binding), app.amqpClient
      )

      app.amqpClient.consumer(queue.name, handler)
        .concurrently(app.amqpClient.consumer(queue.name, handler))
        .compile.drain.unsafeRunAsync(_ => ())

      val publisher = app.amqpClient.publisherOf(publisherConfig)
      publisher.apply(
        """
          |{
          |  "event": "TranscodeComplete",
          |  "details": {
          |    "productionId": "13/330/444#03",
          |    "job": {
          |      "href": "http://localhost:7021/test/data"
          |    }
          |  }
          |}
        """.stripMargin
      )
    }
  }

  private def publisherConfig =
    publishCommandBuilder(StringPayloadMarshaller) using routingKey using exchange

}

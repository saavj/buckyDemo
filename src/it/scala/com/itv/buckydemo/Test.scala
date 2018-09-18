package com.itv.buckydemo

import cats.effect.IO
import com.itv.bucky.PayloadMarshaller.StringPayloadMarshaller
import com.itv.bucky.PublishCommandBuilder._
import com.itv.bucky._
import com.itv.bucky.decl.{Binding, DeclarationExecutor, Queue}
import com.itv.bucky.fs2._
import com.itv.contentdelivery.deliverymetadatapopulator.IntegrationSpec
import com.itv.contentdelivery.deliverymetadatapopulator.configuration.RmqConfig
import com.typesafe.scalalogging.StrictLogging
import com.xebialabs.restito.builder.verify.VerifyHttp._
import com.xebialabs.restito.semantics.Condition._
import io.circe.Decoder
import io.circe.generic.semiauto._
import io.circe.parser._
import itv.contentdelivery.domain.ProductionId
import org.glassfish.grizzly.http.Method
import org.scalatest.concurrent
import org.scalatest.concurrent.Eventually
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class Test extends IntegrationSpec with StrictLogging {

//  implicit val ePatienceConfig: concurrent.Eventually.PatienceConfig = Eventually.PatienceConfig(5 seconds, 1 second)
//
//  override implicit val patienceConfig: PatienceConfig = PatienceConfig(7.seconds, 1.second)
//
//  override implicit lazy val timecodeDecoder: Decoder[Timecode] = deriveDecoder[Timecode]
//
//  override implicit lazy val bundleCode: Decoder[BundleCode] = Decoder.decodeString.map(BundleCode.apply)

  "apply" should {

    "consume a message, publish a message" in testFixture { app =>
      val exchange = ExchangeName("hello")
      val routingKey = RoutingKey("world")
      val queue = Queue(QueueName("buckydemo.helloworld"))

      val binding = Binding(exchange, queue.name, routingKey, Map.empty)

      val handler: StubConsumeHandler[IO, Delivery] = new StubConsumeHandler[IO, Delivery]()

      DeclarationExecutor(
        List(queue, bundleQueue) ++
          List(binding, bundleBinding) ++
          RmqConfig.allDeclarations, app.amqpClient
      )

      app.amqpClient.consumer(queue.name, timecodeHandler)
        .concurrently(app.amqpClient.consumer(bundleQueue.name, bundleHandler))
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
      ).map { _ =>

        Eventually.eventually {
          verifyHttp(app.ctsStubServer).once(
            method(Method.GET),
            uri("/test/data")
          )
          verifyHttp(app.bmsStubServer).once(
            method(Method.GET),
            uri("/bundleCodeDescription/PC01")
          )
          timecodeHandler.receivedMessages.size shouldBe 1
          bundleHandler.receivedMessages.size shouldBe 1
        }

        val json = new String(timecodeHandler.receivedMessages.head.body.value)
        json should include("13/330/444#03")
        json should include(""""productionId":"13/330/444#03"""")
        json should include(""""partType":"Programme Part"""")
        decode[TimecodeCommand](json) shouldBe
          Right(TimecodeCommand(
            ProductionId("13/330/444#03"),
            Seq(

              PlaylistItem(Some(Timecode(0, 0, 10, 0)), Seq.empty),
              PlaylistItem(
                Some(Timecode(0, 0, 45, 0)),

                Seq(
                  ContentPart(PartType("Programme Part"), Timecode(10, 0, 0, 0), Timecode(10, 14, 59, 24)),
                  ContentPart(PartType("Optional Break"), Timecode(10, 20, 0, 0), Timecode(10, 29, 59, 24))
                )
              )
            )
          ))

        bundleHandler.receivedMessages.head.body.unmarshal[String].map(parse).map(_.right.get.spaces2) shouldBe UnmarshalResult.Success(
          """
            |{
            |  "contentId" : "13/330/444#03",
            |  "bundleCode" : "18",
            |  "duration" : {
            |    "hours" : 0,
            |    "minutes" : 0,
            |    "seconds" : 37,
            |    "frames" : 18
            |  },
            |  "lateAndLive" : false,
            |  "bucketName" : "itv-qa-sp",
            |  "renditions" : [
            |    {
            |      "profile" : "CTV011800_4x3",
            |      "checksum" : "123ASF",
            |      "s3Key" : "shared/video/hubmedia/ctv01/741c3640-e706-43dd-86df-f074749f8ac3_mp4.mp4",
            |      "bitrate" : 1800
            |    }
            |  ]
            |}
          """.stripMargin.trim
        )
      }
    }
  }

  private def publisherConfig =
    publishCommandBuilder(StringPayloadMarshaller) using RmqConfig.CloudTranscode.routingKey using RmqConfig.CloudTranscode.exchangeName

}

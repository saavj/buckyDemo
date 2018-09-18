package com.itv.buckydemo

import java.io.File

import _root_.fs2.{Stream, async}
import com.google.common.io.Files
import com.itv.bucky.fs2._
import com.itv.contentdelivery.deliverymetadatapopulator.configuration.Config
import com.typesafe.config.ConfigFactory
import java.net.URL

import org.http4s.Uri
import org.apache.qpid.server.store.MemoryMessageStore
import org.apache.qpid.server.{Broker, BrokerOptions}
import org.apache.qpid.util.FileUtils
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.blaze.Http1Client
import org.scalatest.concurrent.Eventually
import org.scalatest.{Assertion, Matchers, Suite}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait IntegrationSpec extends BaseIntegrationSpec with Matchers with Eventually {

  this: Suite =>

  override implicit val patienceConfig = PatienceConfig(5.seconds, 1.second)

  def startBroker() = {
    val amqpPort = 5672
    val passwordFile = new File("src/it/resources/qpid-passwd")

    val passwordFileLocation: URL = this.getClass.getResource("/qpid-passwd")
    System.setProperty("passwd-location", passwordFileLocation.getPath)

    val workDir: String = new File(passwordFileLocation.toURI).getParent
    System.setProperty("QPID_WORK", workDir)

    val tmpFolder = Files.createTempDir()
    val brokerOptions = new BrokerOptions()

    brokerOptions.setConfigProperty("qpid.work_dir", tmpFolder.getAbsolutePath)
    brokerOptions.setConfigProperty("qpid.amqp_port", s"$amqpPort")
    brokerOptions.setConfigProperty("qpid.password_path", passwordFile.getAbsolutePath)

    brokerOptions.setConfigurationStoreType(MemoryMessageStore.TYPE)

    brokerOptions.setInitialConfigurationLocation(getClass.getResource("/qpid-config.json").toExternalForm)
    val broker = new Broker()
    broker.startup(brokerOptions)
    (broker, tmpFolder)
  }

  case class Application(amqpClient: IOAmqpClient)

  def testFixture(f: Application => IO[Assertion]): Unit = {

    def startServer(client: Client[IO], shutdown: async.mutable.Signal[IO, Boolean]) = IO {
      Server.stream(List.empty, IO.unit).
        interruptWhen(shutdown)
        .compile.drain.unsafeRunAsync(_ => ())
      eventually {
        client.status(Request[IO](uri = baseUriWithPath("/_meta/ping"))).map(_.code shouldBe 200).unsafeRunSync()
      }
    }

    def startRabbitMqStubServer() = IO {
      val rabbitMqStubServer = new StubServer(15672)
      rabbitMqStubServer.start()
      whenHttp(rabbitMqStubServer)
        .`match`(uri("/api/connections"))
        .`then`(status(HttpStatus.OK_200), resourceContent("qpid-api-connections.json"))
      rabbitMqStubServer
    }

    val runTest = for {
      shutdown <- async.signalOf[IO, Boolean](false)
      (broker, brokerTmpFolder) = startBroker()
      httpClient <- Http1Client[IO]()
      _ <- startServer(httpClient, shutdown)
      rmqStub <- startRabbitMqStubServer()
      result <- clientFrom(config.buckyConfig.broker).flatMap { amqpTestClient =>
        Stream.eval(f(Application(amqpTestClient)))
      }.compile.last.attempt
      _ <- IO(rmqStub.stop())
      _ <- httpClient.shutdown
      _ <- shutdown.set(true)
      _ <- IO(broker.shutdown())
      _ <- IO(require(FileUtils.delete(brokerTmpFolder, true), s"Failed to delete $brokerTmpFolder"))
    } yield {
      result match {
        case Right(Some(assertion)) => assertion
        case Left(exception) => throw exception
        case _ => throw new RuntimeException("Boom")
      }
    }
    runTest.unsafeRunSync()
  }
}

trait BaseIntegrationSpec extends BaseSpec {

  val port: Int

  def baseUriWithPath(p: String): Uri = {
    val path = if (p.startsWith("/")) p.drop(1) else p
    Uri.unsafeFromString(s"http://localhost:$port/$path")
  }

}


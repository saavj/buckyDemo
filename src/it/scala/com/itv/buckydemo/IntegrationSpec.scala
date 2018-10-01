package com.itv.buckydemo

import java.io.File

import _root_.fs2.{Stream, async}
import com.google.common.io.Files
import com.itv.bucky.fs2._
import java.net.URL

import cats.effect.IO
import com.itv.bucky.AmqpClientConfig
import org.apache.qpid.server.store.MemoryMessageStore
import org.apache.qpid.server.{Broker, BrokerOptions}
import org.apache.qpid.util.FileUtils
import org.eclipse.jetty.http.HttpStatus
import org.scalatest.concurrent.Eventually
import org.scalatest.{Assertion, Matchers, Suite}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait IntegrationSpec extends Matchers with Eventually {

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

  def testFixture(f: Application => IO[_]): Unit = {

    val runTest = for {
      shutdown <- async.signalOf[IO, Boolean](false)
      (broker, brokerTmpFolder) = startBroker()

      result <- clientFrom(AmqpClientConfig("127.0.0.1", 5672, "guest", "guest")).flatMap { amqpTestClient =>
        Stream.eval(f(Application(amqpTestClient)))
      }.compile.last.attempt

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
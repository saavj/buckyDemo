package com.itv.buckydemo

import _root_.fs2.Stream
import cats.effect.IO
import com.itv.bucky.{AmqpClientConfig, PayloadUnmarshaller, Publisher, QueueName, RequeueHandler, fs2 => buckyfs2}
import com.itv.bucky.Monad.Id
import com.itv.bucky.decl.DeclarationExecutor
import com.itv.bucky.fs2.IOAmqpClient
import com.itv.bucky.pattern.requeue.{RequeuePolicy, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Server extends App {

  def rabbitStream(amqpClientConfig: AmqpClientConfig): Stream[IO, Unit] = IOAmqpClient.use(amqpClientConfig) { amqpClient =>

    val publisher: Id[Publisher[IO, HelloWorldMessage]] = amqpClient.publisherOf[HelloWorldMessage](RmqConfig.publisherConfig)

    val handler: Stream[IO, Unit] = rmqHandler(amqpClient)(
      RmqConfig.queueName,
      new HelloHandler(publisher.apply, _ => IO.pure(true))
    )

    DeclarationExecutor(RmqConfig.allDeclarations, amqpClient)

    Stream.eval(IO(println("Initializing handler"))).flatMap( _ =>
      Stream.eval(IO.unit)
    )
  }

  private def rmqHandler[T: PayloadUnmarshaller](client: IOAmqpClient)(queueName: QueueName, handler: RequeueHandler[IO, T]): Stream[IO, Unit] =
    RequeueOps(client)
      .requeueHandlerOf[T](
      queueName,
      handler,
      RequeuePolicy(maximumProcessAttempts = 10, 3.minute),
      implicitly[PayloadUnmarshaller[T]]
    )

  val run = for {

    _ <- rabbitStream
    _ = println(s"Hello")

  } yield ()

  println("started")

  run.compile.drain.unsafeRunSync()

}

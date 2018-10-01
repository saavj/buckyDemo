package com.itv.buckydemo

import cats.effect.IO
import com.itv.bucky.{Ack, Requeue, RequeueConsumeAction, RequeueHandler}
import com.itv.bucky.CirceSupport._

case class HelloWorldMessage(string: String, ok: String)
object HelloWorldMessage {
  import io.circe.generic.semiauto._

  implicit val encoder = deriveEncoder[HelloWorldMessage]
  implicit val decoder = deriveDecoder[HelloWorldMessage]

  implicit lazy val marshaller = marshallerFromEncodeJson[HelloWorldMessage]
  implicit lazy val unmarshaller = unmarshallerFromDecodeJson[HelloWorldMessage]
}

class HelloHandler(publisher: HelloWorldMessage => IO[Unit], okChecker: String => IO[Boolean]) extends RequeueHandler[IO, HelloWorldMessage] {

  def apply(message: HelloWorldMessage): IO[RequeueConsumeAction] =
    for {
      ok <- okChecker(message.ok)
      action  <- if (ok) {
              println(s"OK message: $message")
              publisher(message).map(_ => Ack)
            } else {
              println(s"NOT OK message: $message")
              IO.pure(Requeue)
            }
    } yield action
}

package com.itv.buckydemo

import cats.effect.IO
import com.itv.bucky.{Ack, Requeue, RequeueConsumeAction, RequeueHandler}
import com.itv.bucky.CirceSupport._
import io.circe.generic.auto._

case class HelloMessage(string: String, ok: String)
object HelloMessage {
  implicit lazy val marshaller = marshallerFromEncodeJson[HelloMessage]
  implicit lazy val unmarshaller = unmarshallerFromDecodeJson[HelloMessage]
}

case class WorldMessage(string: String)
object WorldMessage {
  implicit lazy val marshaller = marshallerFromEncodeJson[WorldMessage]
  implicit lazy val unmarshaller = unmarshallerFromDecodeJson[WorldMessage]
}

class HelloHandler(publisher: WorldMessage => IO[Unit], okChecker: String => IO[Boolean]) extends RequeueHandler[IO, HelloMessage] {

  def apply(message: HelloMessage): IO[RequeueConsumeAction] =
    for {
      ok      <- okChecker(message.ok)
      action  <- if (ok) {
              println(s"OK message: $message")
              publisher(WorldMessage(message.string)).map(_ => Ack)
            } else {
              println(s"NOT OK message: $message")
              IO.pure(Requeue)
            }
    } yield action
}

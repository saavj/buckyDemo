package com.itv.buckydemo

import cats.effect.IO
import com.itv.bucky._
import com.itv.bucky.CirceSupport._
import io.circe.generic.auto._

case class HelloMessage(string: String)
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
  override def apply(v1: HelloMessage): IO[RequeueConsumeAction] = {
    okChecker(v1.string).flatMap { check =>
      if (check) {
        println("check was ok")
        publisher(WorldMessage("World!"))
        IO(Ack)
      } else {
        println("About to deadletter")
        IO(DeadLetter)
      }

    }
  }
}

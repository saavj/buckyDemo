package com.itv.buckydemo

import cats.effect.IO
import com.itv.bucky.{Ack, Requeue, RequeueConsumeAction, RequeueHandler}
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

class HelloHandler(publisher: WorldMessage => IO[Unit], okChecker: String => IO[Boolean]) {

}

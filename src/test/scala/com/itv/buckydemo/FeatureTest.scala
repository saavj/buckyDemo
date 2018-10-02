package com.itv.buckydemo

import cats.effect.IO
import com.itv.bucky.{Ack, DeadLetter, Requeue}
import org.scalatest.{FlatSpec, Matchers}

class FeatureTest extends FlatSpec with Matchers {

  val publisher: WorldMessage => IO[Unit] = ???

  val okChecker: String => IO[Boolean] = ???

  val okMessage = HelloMessage(???)


  "helloHandler" should "Ack ok messages" in {
    val handler = new HelloHandler(publisher, okChecker)


    //todo calling handler shouldBe Ack
  }

//  it should "todo not ok messages" in {

//  }
}
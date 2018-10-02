package com.itv.buckydemo

import cats.effect.IO
import com.itv.bucky.{Ack, DeadLetter, Requeue}
import org.scalatest.{FlatSpec, Matchers}

class FeatureTest extends FlatSpec with Matchers {

  var sideEffect: Option[String] = None
  val publisher: WorldMessage => IO[Unit] = _ => {
    sideEffect = Some("effect")
    IO.pure()
  }

  val okChecker: String => IO[Boolean] = input => if (input == "Joe") IO.pure(true) else IO.pure(false)

  val okMessage = HelloMessage("Joe")

  val notOkMessage = HelloMessage("NotJoe")


  "helloHandler" should "Ack ok messages" in {
    val handler = new HelloHandler(publisher, okChecker)

    handler(okMessage).unsafeRunSync() shouldBe Ack
    sideEffect should contain("effect")

  }

  "helloHandler" should "Deadletter not ok messages" in {
    val handler = new HelloHandler(publisher, okChecker)

    handler(notOkMessage).unsafeRunSync() shouldBe DeadLetter
    sideEffect shouldNot  contain("effect")

  }

}
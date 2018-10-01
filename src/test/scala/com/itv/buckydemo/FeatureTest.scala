package com.itv.buckydemo

import cats.effect.IO
import com.itv.bucky.{Ack, DeadLetter, Requeue}
import org.scalatest.{FlatSpec, Matchers}

class FeatureTest extends FlatSpec with Matchers {

  var sideEffect: Option[String] = None
  val publisher: HelloWorldMessage => IO[Unit] = _ => {
    sideEffect = Some("effect")
    IO.pure(())
  }

  val okChecker: String => IO[Boolean] = _ => IO.pure(true)

  val okHelloMessage = HelloWorldMessage("Hello Sofia", "ok")

  val notOkHelloMessage = HelloWorldMessage("Hello Sofia", "sad")

  "helloHandler" should "Ack ok messages" in {
    val handler = new HelloHandler(publisher, okChecker)

    handler(okHelloMessage).unsafeRunSync() shouldBe Ack
    sideEffect shouldBe Some("effect")
  }

  it should "Requeue not ok messages" in {
    val okChecker: String => IO[Boolean] = _ => IO.pure(false)

    val handler = new HelloHandler(publisher, okChecker)

    handler(notOkHelloMessage).unsafeRunSync() shouldBe Requeue
  }
}
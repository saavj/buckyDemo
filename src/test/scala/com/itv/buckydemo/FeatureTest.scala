package com.itv.buckydemo

import cats.effect.IO
import com.itv.bucky.{Ack, DeadLetter, Requeue}
import org.scalatest.{FlatSpec, Matchers}

class FeatureTest extends FlatSpec with Matchers {

  var sideEffect: Option[String] = None
  val publisher: WorldMessage => IO[Unit] = { _ =>
    sideEffect = Option("Called")
    IO(())
  }

  val okChecker: String => IO[Boolean] = { msg => if (msg == "Hello") IO(true) else IO(false) }

  val okMessage = HelloMessage("Hello")
  val notOkMessage = HelloMessage("...")


  "helloHandler" should "Ack ok messages" in {
    var sideEffect: Option[String] = None
    val publisher: WorldMessage => IO[Unit] = { _ =>
      sideEffect = Option("Called")
      IO(())
    }

    val handler = new HelloHandler(publisher, okChecker)
    handler.apply(okMessage).unsafeRunSync() shouldBe Ack
    sideEffect shouldBe Some("Called")
  }

  it should "not ok messages" in {
    var sideEffect: Option[String] = None
    val publisher: WorldMessage => IO[Unit] = { _ =>
      sideEffect = Option("Called")
      IO(())
    }

    val handler = new HelloHandler(publisher, okChecker)
    handler.apply(notOkMessage).unsafeRunSync() shouldBe DeadLetter
    sideEffect shouldBe None
  }
}
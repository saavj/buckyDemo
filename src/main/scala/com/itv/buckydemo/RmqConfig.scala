package com.itv.buckydemo

import com.itv.bucky.PublishCommandBuilder.publishCommandBuilder
import com.itv.bucky._
import com.itv.bucky.decl.{Declaration, Direct, Exchange}
import com.itv.bucky.pattern.requeue.requeueDeclarations

object RmqConfig {

  // publisher
  val queueName: QueueName = QueueName("helloworld")
  val exchangeName: ExchangeName = ExchangeName("world")
  val routingKey: RoutingKey = RoutingKey("hello")
  val exchange: Exchange = Exchange(exchangeName, exchangeType = Direct).binding(routingKey -> queueName)
  val requeueDeclaration: Iterable[Declaration] = requeueDeclarations(queueName, routingKey)


  val publisherConfig: PublishCommandBuilder.Builder[HelloWorldMessage] =
    publishCommandBuilder(HelloWorldMessage.marshaller)
      .using(exchangeName)
      .using(routingKey)
      .using(MessageProperties.persistentBasic.copy(contentType = Some(ContentType("application/json"))))


  val allDeclarations = List(exchange) ++ requeueDeclaration
}

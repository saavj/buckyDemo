package com.itv.buckydemo

import com.itv.bucky.PublishCommandBuilder.publishCommandBuilder
import com.itv.bucky._
import com.itv.bucky.decl.{Declaration, DeclarationExecutor, Direct, Exchange}
import com.itv.bucky.pattern.requeue.requeueDeclarations

object RmqConfig {

  val exchangeName: ExchangeName = ExchangeName("helloworld")

  // consuming
  val incomingQueueName: QueueName = QueueName("hello")
  val incomingRoutingKey: RoutingKey = RoutingKey("hello.message")
  val incomingRequeueDeclaration: Iterable[Declaration] = requeueDeclarations(incomingQueueName, incomingRoutingKey)


  // publishing
  val outgoingQueueName: QueueName = QueueName("world")
  val outgoungRoutingKey: RoutingKey = RoutingKey("world.message")
  val outgoingRequeueDeclaration: Iterable[Declaration] = requeueDeclarations(outgoingQueueName, outgoungRoutingKey)

  // bindings
  val exchange: Exchange =
    Exchange(exchangeName, exchangeType = Direct)
      .binding(incomingRoutingKey -> incomingQueueName)
      .binding(outgoungRoutingKey -> outgoingQueueName)

  val helloPublisherConfig: PublishCommandBuilder.Builder[HelloMessage] =
    publishCommandBuilder(HelloMessage.marshaller)
      .using(exchangeName)
      .using(incomingRoutingKey)
      .using(MessageProperties.persistentBasic.copy(contentType = Some(ContentType("application/json"))))

  val worldPublisherConfig: PublishCommandBuilder.Builder[WorldMessage] =
    publishCommandBuilder(WorldMessage.marshaller)
      .using(exchangeName)
      .using(outgoungRoutingKey)
      .using(MessageProperties.persistentBasic.copy(contentType = Some(ContentType("application/json"))))


  val allDeclarations = List(exchange) ++ incomingRequeueDeclaration ++ outgoingRequeueDeclaration
}

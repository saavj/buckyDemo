package com.itv.buckydemo

import com.itv.bucky._
import com.itv.bucky.decl._
import com.itv.bucky.pattern.requeue.requeueDeclarations
import com.itv.bucky.PublishCommandBuilder.publishCommandBuilder

object RmqConfig {

  val exchangeName = ExchangeName("helloworld")

  val incomingRoutingKey = RoutingKey("hello")
  val incomingQueue = Queue(QueueName("hello.queue"))
  val incomingRequeueDeclarations = requeueDeclarations(incomingQueue.name, incomingRoutingKey)

  val outgoingRoutingKey = RoutingKey("world")
  val outgoingQueue = Queue(QueueName("world.queue"))
  val outgoingRequeueDeclarations = requeueDeclarations(outgoingQueue.name, outgoingRoutingKey)

  val exchange = Exchange(exchangeName, exchangeType = Direct)
    .binding(incomingRoutingKey -> incomingQueue.name)
    .binding(outgoingRoutingKey -> outgoingQueue.name)

  val incomingPublisherConfig = publishCommandBuilder(HelloMessage.marshaller)
    .using(exchangeName)
    .using(incomingRoutingKey)
    .using(MessageProperties.persistentBasic.copy(contentType = Some(ContentType("application/json"))))

  val outgoingPublisherConfig = publishCommandBuilder(WorldMessage.marshaller)
    .using(exchangeName)
    .using(outgoingRoutingKey)
    .using(MessageProperties.persistentBasic.copy(contentType = Some(ContentType("application/json"))))

  val allDecs = List(exchange) ++ incomingRequeueDeclarations ++ outgoingRequeueDeclarations

}

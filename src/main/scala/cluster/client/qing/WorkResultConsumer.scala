package cluster.client.qing

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import cluster.tcs.qing.WorkResult



object WorkResultConsumer {
  def props(subscribeTo: String): Props =
      Props(new WorkResultConsumer(subscribeTo))
}

// #work-result-consumer
class WorkResultConsumer(subscribeTo: String) extends Actor with ActorLogging {

  val mediator = DistributedPubSub(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(subscribeTo, self)

  def receive = {
    case _: DistributedPubSubMediator.SubscribeAck =>
    case WorkResult(workId, result) =>
      log.info("Consumed result: {}", result)
  }
}
// #work-result-consumer


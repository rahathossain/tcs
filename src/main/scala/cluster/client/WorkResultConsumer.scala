package cluster.client

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator



object WorkResultConsumer {
  def props(subscribeTo: String): Props =
      Props(new WorkResultConsumer(subscribeTo))
}

// #work-result-consumer
class WorkResultConsumer(subscribeTo: String) extends Actor with ActorLogging {

  import cluster.tcs.{WorkResult}

  val mediator = DistributedPubSub(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(subscribeTo, self)

  def receive = {
    case _: DistributedPubSubMediator.SubscribeAck =>
    case WorkResult(workId, result) =>
      log.info("Consumed result: {}", result)
  }
}
// #work-result-consumer


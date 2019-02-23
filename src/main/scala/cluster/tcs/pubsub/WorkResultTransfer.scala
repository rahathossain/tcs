package cluster.tcs.pubsub

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}

object WorkResultTransfer {
  def props(transform: String => String, subscribeTo: String, publishTo: String): Props =
    Props(new WorkResultTransfer(transform, subscribeTo, publishTo))
}

// #work-result-consumer
class WorkResultTransfer(transform: String => String, subscribeTo: String, publishTo: String) extends Actor with ActorLogging {

  val mediator = DistributedPubSub(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(subscribeTo, self)

  def receive = {
    case _: DistributedPubSubMediator.SubscribeAck =>
    case WorkResult(workId, result) =>
      log.info("Transferring result: {}, from {} to {} ", result, subscribeTo, publishTo)

    mediator ! DistributedPubSubMediator.Publish(publishTo, Work( workId, transform(result)) )
  }

}



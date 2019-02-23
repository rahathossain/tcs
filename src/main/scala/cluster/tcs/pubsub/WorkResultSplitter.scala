package cluster.tcs.pubsub

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}

object WorkResultSplitter {
  def props(transform: String => String, subscribeTo: String, publishTo: String): Props =
    Props(new WorkResultSplitter(transform, subscribeTo, publishTo))
}

// #work-result-consumer
class WorkResultSplitter(transform: String => String, subscribeTo: String, publishTo: String) extends Actor with ActorLogging {

  val mediator = DistributedPubSub(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(subscribeTo, self)

  def receive = {
    case _: DistributedPubSubMediator.SubscribeAck =>
    case WorkResult(workId, result: List[String]) =>
      log.info("Splitting result: {}, from {} to {} ", result, subscribeTo, publishTo)

      result.map ( r =>
        mediator ! DistributedPubSubMediator.Publish(publishTo, Work( workId, transform(r)) )
      )
  }

}




package cluster.tcs

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator

object WorkResultSplitter {
  def props(transform: Any => Any, subscribeTo: String, publishTo: String): Props =
    Props(new WorkResultSplitter(transform, subscribeTo, publishTo))
}

// #work-result-consumer
class WorkResultSplitter(transform: Any => Any, subscribeTo: String, publishTo: String) extends Actor with ActorLogging {

  val mediator = DistributedPubSub(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(subscribeTo, self)

  def receive = {
    case _: DistributedPubSubMediator.SubscribeAck =>
    case WorkResult(workId, result: List[Any]) =>
      log.info("Splitting result: {}, from {} to {} ", result, subscribeTo, publishTo)

      result.map ( r =>
        mediator ! DistributedPubSubMediator.Publish(publishTo, Work( workId, transform(r)) )
      )
  }

}




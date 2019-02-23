package cluster.tcs.pubsub

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}

object WorkResultRouter {
  def props(transform: Any => Any, routeCondition: Any => Boolean, subscribeTo: String,
            eitherPublishTo: String, orPublishTo: String): Props =
    Props(new WorkResultRouter(transform, routeCondition, subscribeTo, eitherPublishTo, orPublishTo))
}

// #work-result-router
class WorkResultRouter(transform: Any => Any, routeCondition: Any => Boolean, subscribeTo: String,
                       eitherPublishTo: String, orPublishTo: String) extends Actor with ActorLogging {

  val mediator = DistributedPubSub(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(subscribeTo, self)

  def receive = {
    case _: DistributedPubSubMediator.SubscribeAck =>
    case WorkResult(workId, result) =>
      log.info("Routing result: {}, from {} to , either {} or {}",
                result, subscribeTo, eitherPublishTo, orPublishTo)

      def publishTo(topic: String) =
        mediator ! DistributedPubSubMediator.Publish(topic, Work( workId, transform(result).toString) )

      if(routeCondition()) publishTo(eitherPublishTo) else publishTo(orPublishTo)
  }



}
// #work-result-router



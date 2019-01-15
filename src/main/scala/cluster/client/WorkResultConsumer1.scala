package cluster.client

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator

import cluster.tcs._

object WorkResultConsumer1 {
  def props: Props = Props(new WorkResultConsumer1)
}

// #work-result-consumer
class WorkResultConsumer1 extends Actor with ActorLogging {

  import AppConfig._

  val mediator = DistributedPubSub(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(ResultsTopic1, self)

  def receive = {
    case _: DistributedPubSubMediator.SubscribeAck =>
    case WorkResult(workId, result) =>
      log.info("Consumed result: {}", result)
  }

}
// #work-result-consumer



object WorkResultConsumer2 {
  def props: Props = Props(new WorkResultConsumer2)
}

// #work-result-consumer
class WorkResultConsumer2 extends Actor with ActorLogging {

  import AppConfig._

  val mediator = DistributedPubSub(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(ResultsTopic1, self)

  def receive = {
    case _: DistributedPubSubMediator.SubscribeAck =>
    case WorkResult(workId, result) =>
      log.info("Consumed result: {}", result)
  }

}
// #work-result-consumer
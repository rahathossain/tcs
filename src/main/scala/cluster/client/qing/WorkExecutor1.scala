package cluster.client.qing

import java.util.concurrent.ThreadLocalRandom

import akka.actor.{Actor, Props}
import cluster.tcs.proto._

import scala.concurrent.duration._

/**
 * Work executor is the actor actually performing the work.
 */
object WorkExecutor1 {
  def props = Props(new WorkExecutor1)
}

class WorkExecutor1 extends Actor {
  import context.dispatcher

  def receive = {
    case ExecuteWork(n1: String) =>
      val n = n1.toInt
      val n2 = n * n
      val result = s"$n * $n = $n2"

      // simulate that the processing time varies
      val randomProcessingTime = ThreadLocalRandom.current.nextInt(1, 3).seconds
      context.system.scheduler.scheduleOnce(randomProcessingTime, sender(), WorkExecuted(result))
  }

}

object WorkExecutor2 {
  def props = Props(new WorkExecutor2)
}

class WorkExecutor2 extends Actor {
  import context.dispatcher

  def receive = {
    case ExecuteWork(n1: String) =>
      val n = n1.toInt
      val n2 = n + n
      val result = s"$n + $n = $n2"

      // simulate that the processing time varies
      val randomProcessingTime = ThreadLocalRandom.current.nextInt(1, 3).seconds
      context.system.scheduler.scheduleOnce(randomProcessingTime, sender(), WorkExecuted(result))

  }

}
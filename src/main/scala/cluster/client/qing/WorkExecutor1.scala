package cluster.client.qing

import java.util.concurrent.ThreadLocalRandom

import akka.actor.{Actor, Props}
import cluster.tcs.qing.WorkExecutorProtocol.{DoWork, WorkComplete}

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
    case DoWork(n: Int) =>
      val n2 = n * n
      val result = s"$n * $n = $n2"

      // simulate that the processing time varies
      val randomProcessingTime = ThreadLocalRandom.current.nextInt(1, 3).seconds
      context.system.scheduler.scheduleOnce(randomProcessingTime, sender(), WorkComplete(result))
  }

}

object WorkExecutor2 {
  def props = Props(new WorkExecutor2)
}

class WorkExecutor2 extends Actor {
  import context.dispatcher

  def receive = {
    case DoWork(n: Int) =>
      val n2 = n + n
      val result = s"$n + $n = $n2"

      // simulate that the processing time varies
      val randomProcessingTime = ThreadLocalRandom.current.nextInt(1, 3).seconds
      context.system.scheduler.scheduleOnce(randomProcessingTime, sender(), WorkComplete(result))

  }

}
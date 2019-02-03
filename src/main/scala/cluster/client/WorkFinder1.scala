package cluster.client

import java.util.UUID

import akka.actor.{Actor, ActorLogging, Props, Timers}

// #companion object
object WorkFinder1 {
  def props = Props(new WorkFinder1)
}
// #companion object


class WorkFinder1 extends Actor with ActorLogging with Timers {
  import cluster.tcs.JobSeekerProtocol._

  def nextWorkId(): String = UUID.randomUUID().toString

  override def receive: Receive = {
    case Seek =>
      val x = nextWorkId()
      sender() ! JobFound(x)
  }
}

package cluster.tcs

import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

import akka.actor.{Actor, ActorLogging, Props, Timers}
import akka.pattern._
import akka.util.Timeout

import scala.concurrent.duration._

/**
 * Dummy front-end that periodically sends a workload to the master.
 */
object FrontEnd {

  def props(proxyProps: Props, workFinderProps: Props): Props = Props(new FrontEnd(proxyProps, workFinderProps))

  private case object NotOk
  private case object Tick
  private case object Retry
}

// #front-end
class FrontEnd(proxyProps: Props, workFinderProps: Props) extends Actor with ActorLogging with Timers {
  import FrontEnd._
  import context.dispatcher
  import JobSeekerProtocol._

  val masterProxy = context.actorOf(proxyProps, name = "masterProxy" )
  val workFinderActor = context.actorOf(workFinderProps, name="workFinder")

  var workCounter = 0

  //def nextWorkId(): String = UUID.randomUUID().toString

  override def preStart(): Unit = {
    timers.startSingleTimer("tick", Tick, 5.seconds)
  }

  def receive = idle

  def idle: Receive = {
    case Tick =>
      log.info("Seeking work ....")
      workFinderActor ! Seek

    case JobFound(workId: String) =>
      log.info("Produced work: {}", workCounter)
      workCounter += 1
      val work = Work(workId, workCounter)
      context.become(busy(work))

  }

  def busy(workInProgress: Work): Receive = {
    sendWork(workInProgress)

    {
      case Master.Ack(workId) =>
        log.info("Got ack for workId {}", workId)
        val nextTick = ThreadLocalRandom.current.nextInt(3, 10).seconds
        timers.startSingleTimer(s"tick", Tick, nextTick)
        context.become(idle)

      case NotOk =>
        log.info("Work {} not accepted, retry after a while", workInProgress.workId)
        timers.startSingleTimer("retry", Retry, 3.seconds)

      case Retry =>
        log.info("Retrying work {}", workInProgress.workId)
        sendWork(workInProgress)
    }
  }

  def sendWork(work: Work): Unit = {
    implicit val timeout = Timeout(5.seconds)
    (masterProxy ? work).recover {
      case _ => NotOk
    } pipeTo self
  }

}
// #front-end
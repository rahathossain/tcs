package cluster.client.qing

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Timers}
import akka.pattern._
import akka.util.Timeout
import cluster.tcs.qing.{Master, Transport}
import cluster.tcs.qing.TransportExecutorProtocol.{DoTransfer, TransportComplete}

import scala.concurrent.duration._

/**
  * Transport executor is the actor actually performing the transport.
  */
object TransportExecutor1 {

  def props(proxyProps: Props): Props = Props(new TransportExecutor1(proxyProps))

  private case object NotOk
  private case object Tick
  private case object Retry

}

class TransportExecutor1(proxyProps: Props) extends Actor with ActorLogging with Timers {

  import TransportExecutor1._
  import context.dispatcher

  val targetMasterProxy = context.actorOf(proxyProps, name = "targetMasterProxy" )

  def receive = idle

  def idle: Receive = {

    case DoTransfer(transportId: String, result: Any) =>
      log.info(s"Transfering transportId = $transportId and result = $result")
      val transport = Transport(transportId, transform1(result) )
      val transporterRef = sender()
      context.become(busy(transport, transporterRef))

  }

  def busy(transportInProgress: Transport, transporterRef: ActorRef): Receive = {
    sendTransport(transportInProgress)

    {
      case Master.Ack(transportId) =>
        log.info("Got ack for transportId {}", transportId)
        transporterRef ! TransportComplete(transportId)
        context.become(idle)

      case NotOk =>
        log.info("Transport {} not accepted, retry after a while", transportInProgress.transportId)
        timers.startSingleTimer("retry", Retry, 3.seconds)

      case Retry =>
        log.info("Retrying transport {}", transportInProgress.transportId)
        sendTransport(transportInProgress)
    }
  }


  def sendTransport(transport: Transport): Unit = {
    implicit val timeout = Timeout(5.seconds)
    (targetMasterProxy ? transport).recover {
      case _ => NotOk
    } pipeTo self
  }



  def transform1(result: Any): Int = {
    val i = result.toString.indexOf('=')
    val x = result.toString.substring(i+1).trim
    x.toInt
  }



}





// ##TransportExecutor2



/**
  * Transport executor is the actor actually performing the transport.
  */
object TransportExecutor2 {
  def props: Props = Props(new TransportExecutor2)
}

class TransportExecutor2 extends Actor with ActorLogging with Timers {

  def receive: Receive = {

    case DoTransfer(transportId: String, result: Any) =>
      log.info(s"Transfering transportId = $transportId and result = $result")
      val transport = Transport(transportId, result)
      log.info(s" \n\n *** result = $result ")
      sender() ! TransportComplete(transportId)
  }
}
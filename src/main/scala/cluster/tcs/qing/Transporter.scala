package cluster.tcs.qing

import java.util.UUID

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor._

import scala.concurrent.duration._

/**
  * The transporter is actually more of a middle manager, delegating the actual transport
  * to the TransportExecutor, supervising it and keeping itself available to interact with the transport master.
  */
object Transporter {

  def props(masterProxy: ActorRef, transportExecutorProps: TransportExecutorProtocol.TransportExecutorProps): Props =
    Props(new Transporter(masterProxy, transportExecutorProps))

}

class Transporter(masterProxy: ActorRef, transportExecutorProps: TransportExecutorProtocol.TransportExecutorProps)
  extends Actor with Timers with ActorLogging {
  import MasterTransporterProtocol._
  import context.dispatcher


  val transporterId = UUID.randomUUID().toString
  val registerInterval = context.system.settings.config.getDuration("distributed-transporters.transporter-registration-interval").getSeconds.seconds

  val registerTask = context.system.scheduler.schedule(0.seconds, registerInterval, masterProxy, RegisterTransporter(transporterId))

  val transportExecutor = createTransportExecutor()

  var currentTransportId: Option[String] = None
  def transportId: String = currentTransportId match {
    case Some(transportId) => transportId
    case None         => throw new IllegalStateException("Not transporting")
  }

  def receive = idle

  def idle: Receive = {
    case TransportIsReady =>
      // this is the only state where we reply to TransportIsReady
      masterProxy ! TransporterRequestsTransport(transporterId)

    case Transport(transportId, job: String) =>
      log.info("Got transport: {}", job)
      currentTransportId = Some(transportId)
      //transportExecutor ! TransportExecutorProtocol.DoTransport(job)
      transportExecutor ! TransportExecutorProtocol.DoTransfer(transportId, job)
      context.become(transporting)
  }

  def transporting: Receive = {
    case TransportExecutorProtocol.TransportComplete(result) =>
      log.info("Transport is complete. Result {}.", result)
      masterProxy ! TransportIsDone(transporterId, transportId, result)
      context.setReceiveTimeout(5.seconds)
      context.become(waitForTransportIsDoneAck(result))

    case _: Transport =>
      log.warning("Yikes. Master told me to do transport, while I'm already transporting.")

  }

  def waitForTransportIsDoneAck(result: String): Receive = {
    case TransportAck(id) if id == transportId =>
      masterProxy ! TransporterRequestsTransport(transporterId)
      context.setReceiveTimeout(Duration.Undefined)
      context.become(idle)

    case ReceiveTimeout =>
      log.info("No ack from master, resending transport result")
      masterProxy ! TransportIsDone(transporterId, transportId, result)

  }

  def createTransportExecutor(): ActorRef =
  // in addition to starting the actor we also watch it, so that
  // if it stops this transporter will also be stopped
    context.watch(context.actorOf(transportExecutorProps(), "transport-executor"))


  override def supervisorStrategy = OneForOneStrategy() {
    case _: ActorInitializationException => Stop
    case _: Exception =>
      currentTransportId foreach { transportId => masterProxy ! TransportFailed(transporterId, transportId) }
      context.become(idle)
      Restart
  }

  override def postStop(): Unit = {
    registerTask.cancel()
    masterProxy ! DeRegisterTransporter(transporterId)
  }

}
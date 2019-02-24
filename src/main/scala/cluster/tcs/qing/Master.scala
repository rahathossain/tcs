package cluster.tcs.qing

import akka.actor.{ActorLogging, ActorRef, Props, Timers}
import akka.cluster.pubsub.DistributedPubSubMediator
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}


import scala.concurrent.duration.{Deadline, FiniteDuration, _}

/**
 * The master actor keep tracks of all available workers, and all scheduled and ongoing work items
 */
object Master {

  def props(id: String, workTimeout: FiniteDuration, inTopic: String ,resultsTopic: String): Props =
    Props(new Master(id, workTimeout, inTopic, resultsTopic))



  private sealed trait WorkerStatus
  private case object Idle extends WorkerStatus
  private case class Busy(workId: String, deadline: Deadline) extends WorkerStatus
  private case class WorkerState(ref: ActorRef, status: WorkerStatus, staleWorkerDeadline: Deadline)

  private case object CleanupTick

  private sealed trait TransporterStatus
  private case object TransporterIdle extends TransporterStatus
  private case class TransporterBusy(transportId: String, deadline: Deadline) extends TransporterStatus
  private case class TransporterState(ref: ActorRef, status: TransporterStatus, staleTransporterDeadline: Deadline)

  private case object TransporterCleanupTick

}

class Master(id: String, workTimeout: FiniteDuration, inTopic: String, resultsTopic: String)
                          extends Timers with PersistentActor with ActorLogging {
  import Master._
  import WorkState._

  override val persistenceId: String = id



  val considerWorkerDeadAfter: FiniteDuration =
    context.system.settings.config.getDuration("distributed-workers.consider-worker-dead-after").getSeconds.seconds
  def newStaleWorkerDeadline(): Deadline = considerWorkerDeadAfter.fromNow

  timers.startPeriodicTimer("cleanup", CleanupTick, workTimeout / 2)

  // the set of available workers is not event sourced as it depends on the current set of workers
  private var workers = Map[String, WorkerState]()

  // workState is event sourced to be able to make sure work is processed even in case of crash
  private var workState = WorkState.empty

  //val mediator: ActorRef = DistributedPubSub(context.system).mediator

  //mediator ! DistributedPubSubMediator.Subscribe(inTopic, self)

  //TODO inTopic - need to subscribe


  // #Transporter
  import TransportState._
  val transportTimeout = workTimeout
  val considerTransporterDeadAfter: FiniteDuration =
    context.system.settings.config.getDuration("distributed-transporters.consider-transporter-dead-after").getSeconds.seconds
  def newStaleTransporterDeadline(): Deadline = considerTransporterDeadAfter.fromNow

  //timers.startPeriodicTimer("TransporterCleanup", TransporterCleanupTick, transportTimeout / 2)
  timers.startPeriodicTimer("TransporterCleanup", TransporterCleanupTick, transportTimeout )
  // the set of available transporters is not event sourced as it depends on the current set of transporters
  private var transporters = Map[String, TransporterState]()

  // transportState is event sourced to be able to make sure transport is processed even in case of crash
  private var transportState = TransportState.empty
  // #Transporter



  override def receiveRecover: Receive = {

    case SnapshotOffer(_, workStateSnapshot: WorkState) =>
      // If we would have  logic triggering snapshots in the actor
      // we would start from the latest snapshot here when recovering
      log.info("Got snapshot work state")
      workState = workStateSnapshot

    case event: WorkDomainEvent =>
      // only update current state by applying the event, no side effects
      workState = workState.updated(event)
      log.info("Replayed {}", event.getClass.getSimpleName)


    case SnapshotOffer(_, transportStateSnapshot: TransportState) =>
      // If we would have  logic triggering snapshots in the actor
      // we would start from the latest snapshot here when recovering
      log.info("Got snapshot transport state")
      transportState = transportStateSnapshot

    case event: TransportDomainEvent =>
      // only update current state by applying the event, no side effects
      transportState = transportState.updated(event)
      log.info("Replayed {}", event.getClass.getSimpleName)

    case RecoveryCompleted =>
      log.info("Recovery completed")

  }

  override def receiveCommand: Receive = {
    case _: DistributedPubSubMediator.SubscribeAck =>
    case RegisterWorker(workerId) =>
      if (workers.contains(workerId)) {
        workers += (workerId -> workers(workerId).copy(ref = sender(), staleWorkerDeadline = newStaleWorkerDeadline()))
      } else {
        log.info("Worker registered: {}", workerId)
        val initialWorkerState = WorkerState(
          ref = sender(),
          status = Idle,
          staleWorkerDeadline = newStaleWorkerDeadline())
        workers += (workerId -> initialWorkerState)

        if (workState.hasWork)
          sender() ! WorkIsReady
      }

    // #graceful-remove
    case DeRegisterWorker(workerId) =>
      workers.get(workerId) match {
        case Some(WorkerState(_, Busy(workId, _), _)) =>
          // there was a workload assigned to the worker when it left
          log.info("Busy worker de-registered: {}", workerId)
          persist(WorkerFailed(workId)) { event ⇒
            workState = workState.updated(event)
            notifyWorkers()
          }
        case Some(_) =>
          log.info("Worker de-registered: {}", workerId)
        case _ =>
      }
      workers -= workerId
    // #graceful-remove

    case WorkerRequestsWork(workerId) =>
      if (workState.hasWork) {
        workers.get(workerId) match {
          case Some(workerState @ WorkerState(_, Idle, _)) =>
            val work = workState.nextWork
            persist(WorkStarted(work.workId)) { event =>
              workState = workState.updated(event)
              log.info("Giving worker {} some work {}", workerId, work.workId)
              val newWorkerState = workerState.copy(
                status = Busy(work.workId, Deadline.now + workTimeout),
                staleWorkerDeadline = newStaleWorkerDeadline())
              workers += (workerId -> newWorkerState)
              sender() ! work
            }
          case _ =>
        }
      }

    case WorkIsDone(workerId, workId, result) =>
      // idempotent - redelivery from the worker may cause duplicates, so it needs to be
      if (workState.isDone(workId)) {
        // previous Ack was lost, confirm again that this is done
        sender() ! WorkIsDoneAck(workId)
      } else if (!workState.isInProgress(workId)) {
        log.info("Work {} not in progress, reported as done by worker {}", workId, workerId)
      } else {
        log.info("Work {} is done by worker {}", workId, workerId)
        changeWorkerToIdle(workerId, workId)
        persist(WorkCompleted(workId, result)) { event ⇒
          workState = workState.updated(event)
          //mediator ! DistributedPubSubMediator.Publish(resultsTopic, WorkResult(workId, result))
          // Ack back to original sender
          sender ! WorkIsDoneAck(workId)

          // #TRANSPORTER
          transportResult(Transport(workId, result))
        }
      }

    case WorkFailed(workerId, workId) =>
      if (workState.isInProgress(workId)) {
        log.info("Work {} failed by worker {}", workId, workerId)
        changeWorkerToIdle(workerId, workId)
        persist(WorkerFailed(workId)) { event ⇒
          workState = workState.updated(event)
          notifyWorkers()
        }
      }

    // #persisting
    case work: Work =>
      // idempotent
      if (workState.isAccepted(work.workId)) {
        sender() ! MasterAck(work.workId)
      } else {
        log.info("Accepted work: {} - by {}", work.workId, id)
        persist(WorkAccepted(work)) { event ⇒
          // Ack back to original sender
          sender() ! MasterAck(work.workId)
          workState = workState.updated(event)
          notifyWorkers()
        }
      }
    // #persisting

    // #pruning
    case CleanupTick =>
      workers.foreach {
        case (workerId, WorkerState(_, Busy(workId, timeout), _)) if timeout.isOverdue() =>
          log.info("Work timed out: {}", workId)
          workers -= workerId
          persist(WorkerTimedOut(workId)) { event ⇒
            workState = workState.updated(event)
            notifyWorkers()
          }


        case (workerId, WorkerState(_, Idle, lastHeardFrom)) if lastHeardFrom.isOverdue() =>
          log.info("Too long since heard from worker {}, pruning", workerId)
          workers -= workerId

        case _ => // this one is a keeper!
      }
    // #pruning



    //#TRANSPORT
    case RegisterTransporter(transporterId) =>
      if (transporters.contains(transporterId)) {
        transporters += (transporterId -> transporters(transporterId).copy(ref = sender(), staleTransporterDeadline = newStaleTransporterDeadline()))
      } else {
        log.info("Transporter registered: {}", transporterId)
        val initialTransporterState = TransporterState(
          ref = sender(),
          status = TransporterIdle,
          staleTransporterDeadline = newStaleTransporterDeadline())
        transporters += (transporterId -> initialTransporterState)

        if (transportState.hasTransport)
          sender() ! TransportIsReady
      }

    // #graceful-remove
    case DeRegisterTransporter(transporterId) =>
      transporters.get(transporterId) match {
        case Some(TransporterState(_, TransporterBusy(transportId, _), _)) =>
          // there was a transportload assigned to the transporter when it left
          log.info("TransporterBusy transporter de-registered: {}", transporterId)
          persist(TransporterFailed(transportId)) { event ⇒
            transportState = transportState.updated(event)
            notifyTransporters()
          }
        case Some(_) =>
          log.info("Transporter de-registered: {}", transporterId)
        case _ =>
      }
      transporters -= transporterId
    // #graceful-remove

    case TransporterRequestsTransport(transporterId) =>
      if (transportState.hasTransport) {
        transporters.get(transporterId) match {
          case Some(transporterState @ TransporterState(_, TransporterIdle, _)) =>
            val transport = transportState.nextTransport
            persist(TransportStarted(transport.transportId)) { event =>
              transportState = transportState.updated(event)
              log.info("Giving transporter {} some transport {}", transporterId, transport.transportId)
              val newTransporterState = transporterState.copy(
                status = TransporterBusy(transport.transportId, Deadline.now + transportTimeout),
                staleTransporterDeadline = newStaleTransporterDeadline())
              transporters += (transporterId -> newTransporterState)
              sender() ! transport
            }
          case _ =>
        }
      }

    case TransportIsDone(transporterId, transportId, result) =>
      // idempotent - redelivery from the transporter may cause duplicates, so it needs to be
      if (transportState.isDone(transportId)) {
        // previous Ack was lost, confirm again that this is done
        sender() ! TransportAck(transportId)
      } else if (!transportState.isInProgress(transportId)) {
        log.info("Transport {} not in progress, reported as done by transporter {}", transportId, transporterId)
      } else {
        log.info("Transport {} is done by transporter {}", transportId, transporterId)
        changeTransporterToTransporterIdle(transporterId, transportId)
        persist(TransportCompleted(transportId, result)) { event ⇒
          transportState = transportState.updated(event)

          sender ! TransportAck(transportId)
        }
      }

    case TransportFailed(transporterId, transportId) =>
      if (transportState.isInProgress(transportId)) {
        log.info("Transport {} failed by transporter {}", transportId, transporterId)
        changeTransporterToTransporterIdle(transporterId, transportId)
        persist(TransporterFailed(transportId)) { event ⇒
          transportState = transportState.updated(event)
          notifyTransporters()
        }
      }

    // #persisting
    case transport: Transport =>
      // idempotent
      if (transportState.isAccepted(transport.transportId)) {
        sender() ! MasterAck(transport.transportId)
      } else {
        log.info("Accepted transport: {} - by {}", transport.transportId, id)
        persist(TransportAccepted(transport)) { event ⇒
          // Ack back to original sender
          sender() ! MasterAck(transport.transportId)
          transportState = transportState.updated(event)
          notifyTransporters()
        }
      }
    // #persisting

    // #pruning
    case TransporterCleanupTick =>
      transporters.foreach {
        case (transporterId, TransporterState(_, TransporterBusy(transportId, timeout), _)) if timeout.isOverdue() =>
          log.info("Transport timed out: {}", transportId)
          transporters -= transporterId
          persist(TransporterTimedOut(transportId)) { event ⇒
            transportState = transportState.updated(event)
            notifyTransporters()
          }


        case (transporterId, TransporterState(_, TransporterIdle, lastHeardFrom)) if lastHeardFrom.isOverdue() =>
          log.info("Too long since heard from transporter {}, pruning", transporterId)
          transporters -= transporterId

        case _ => // this one is a keeper!
      }
    // #pruning
    //#TRANSPORT

  }

  def notifyWorkers(): Unit =
    if (workState.hasWork) {
      workers.foreach {
        case (_, WorkerState(ref, Idle, _)) => ref ! WorkIsReady
        case _                           => // busy
      }
    }

  def changeWorkerToIdle(workerId: String, workId: String): Unit =
    workers.get(workerId) match {
      case Some(workerState @ WorkerState(_, Busy(`workId`, _), _)) ⇒
        val newWorkerState = workerState.copy(status = Idle, staleWorkerDeadline = newStaleWorkerDeadline())
        workers += (workerId -> newWorkerState)
      case _ ⇒
        // ok, might happen after standby recovery, worker state is not persisted
    }

  def tooLongSinceHeardFrom(lastHeardFrom: Long) =
    System.currentTimeMillis() - lastHeardFrom > considerWorkerDeadAfter.toMillis

  //#TRANSPORT
  def notifyTransporters(): Unit =
    if (transportState.hasTransport) {
      transporters.foreach {
        case (_, TransporterState(ref, TransporterIdle, _)) => ref ! TransportIsReady
        case _                           => // busy
      }
    }

  def changeTransporterToTransporterIdle(transporterId: String, transportId: String): Unit =
    transporters.get(transporterId) match {
      case Some(transporterState @ TransporterState(_, TransporterBusy(`transportId`, _), _)) ⇒
        val newTransporterState = transporterState.copy(status = TransporterIdle, staleTransporterDeadline = newStaleTransporterDeadline())
        transporters += (transporterId -> newTransporterState)
      case _ ⇒
      // ok, might happen after standby recovery, transporter state is not persisted
    }

  def tooLongSinceHeardFrom2(lastHeardFrom: Long) =
    System.currentTimeMillis() - lastHeardFrom > considerTransporterDeadAfter.toMillis

  def transportResult( tr: Transport ) = {
    tr match {
        case transport: Transport =>
          // idempotent
          if (transportState.isAccepted(transport.transportId)) {
            sender() ! MasterAck(transport.transportId)
          } else {
            log.info("Accepted transport: {} - by {}", transport.transportId, id)
            persist(TransportAccepted(transport)) { event ⇒
              // Ack back to original sender
              sender() ! MasterAck(transport.transportId)
              transportState = transportState.updated(event)
              notifyTransporters()
            }
          }
        case _ => log.error("transportResult didn't match")
    }
  }

  //#TRANSPORT

}
package cluster.tcs.qing

import scala.collection.immutable.Queue
import cluster.tcs.proto._

object TransportState {

  def empty: TransportState = TransportState(
    pendingTransport = Queue.empty,
    transportInProgress = Map.empty,
    acceptedTransportIds = Set.empty,
    doneTransportIds = Set.empty)

  trait TransportDomainEvent
  // #events
  case class TransportAccepted(transport: Transport) extends TransportDomainEvent
  case class TransportStarted(transportId: String) extends TransportDomainEvent
  case class TransportCompleted(transportId: String, result: Any) extends TransportDomainEvent
  case class TransporterFailed(transportId: String) extends TransportDomainEvent
  case class TransporterTimedOut(transportId: String) extends TransportDomainEvent
  // #events
}

case class TransportState private (
                                    private val pendingTransport: Queue[Transport],
                                    private val transportInProgress: Map[String, Transport],
                                    private val acceptedTransportIds: Set[String],
                                    private val doneTransportIds: Set[String]) {

  import TransportState._

  def hasTransport: Boolean = pendingTransport.nonEmpty
  def nextTransport: Transport = pendingTransport.head
  def isAccepted(transportId: String): Boolean = acceptedTransportIds.contains(transportId)
  def isInProgress(transportId: String): Boolean = transportInProgress.contains(transportId)
  def isDone(transportId: String): Boolean = doneTransportIds.contains(transportId)

  def updated(event: TransportDomainEvent): TransportState = event match {
    case TransportAccepted(transport) ⇒
      copy(
        pendingTransport = pendingTransport enqueue transport,
        acceptedTransportIds = acceptedTransportIds + transport.transportId)

    case TransportStarted(transportId) ⇒
      val (transport, rest) = pendingTransport.dequeue
      require(transportId == transport.transportId, s"TransportStarted expected transportId $transportId == ${transport.transportId}")
      copy(
        pendingTransport = rest,
        transportInProgress = transportInProgress + (transportId -> transport))

    case TransportCompleted(transportId, result) ⇒
      copy(
        transportInProgress = transportInProgress - transportId,
        doneTransportIds = doneTransportIds + transportId)

    case TransporterFailed(transportId) ⇒
      copy(
        pendingTransport = pendingTransport enqueue transportInProgress(transportId),
        transportInProgress = transportInProgress - transportId)

    case TransporterTimedOut(transportId) ⇒
      copy(
        pendingTransport = pendingTransport enqueue transportInProgress(transportId),
        transportInProgress = transportInProgress - transportId)
  }

}

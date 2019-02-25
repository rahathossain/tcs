package cluster.tcs.qing

import scala.collection.immutable.Queue
import cluster.tcs.proto._

object TransportState {

  def empty: TransportState = TransportState(
    pendingTransport = Queue.empty,
    transportInProgress = Map.empty,
    acceptedTransportIds = Set.empty,
    doneTransportIds = Set.empty)



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

  def updated(event: TransportAccepted) = event match {
    case TransportAccepted(transport_op) ⇒
      val transport = transport_op.get
      copy(
        pendingTransport = pendingTransport enqueue transport,
        acceptedTransportIds = acceptedTransportIds + transport.transportId)
  }

  def updated(event: TransportStarted) = event match {
    case TransportStarted(transportId) ⇒
      val (transport, rest) = pendingTransport.dequeue
      require(transportId == transport.transportId, s"TransportStarted expected transportId $transportId == ${transport.transportId}")
      copy(
        pendingTransport = rest,
        transportInProgress = transportInProgress + (transportId -> transport))
  }

  def updated(event: TransportCompleted) = event match {
    case TransportCompleted(transportId, result) ⇒
      copy(
        transportInProgress = transportInProgress - transportId,
        doneTransportIds = doneTransportIds + transportId)
  }

  def updated(event: TransporterFailed) = event match {
    case TransporterFailed(transportId) ⇒
      copy(
        pendingTransport = pendingTransport enqueue transportInProgress(transportId),
        transportInProgress = transportInProgress - transportId)
  }

  def updated(event: TransporterTimedOut) = event match {
    case TransporterTimedOut(transportId) ⇒
      copy(
        pendingTransport = pendingTransport enqueue transportInProgress(transportId),
        transportInProgress = transportInProgress - transportId)
  }


}

package cluster.tcs.qing

import akka.actor.Props

case class Work(workId: String, job: String)

case class WorkResult(workId: String, result: String)


object MasterWorkerProtocol {
  // Messages from Workers
  case class RegisterWorker(workerId: String)
  case class DeRegisterWorker(workerId: String)
  case class WorkerRequestsWork(workerId: String)
  case class WorkIsDone(workerId: String, workId: String, result: String)
  case class WorkFailed(workerId: String, workId: String)

  // Messages to Workers
  case object WorkIsReady
  case class Ack(id: String)
}

object WorkExecutorProtocol {
  case class DoWork(n: String)
  case class WorkComplete(result: String)
  type WorkExecutorProps = () => Props
}


// TRANSPORT

case class Transport(transportId: String, job: String)

case class TransportResult(transportId: String, result: String)


object MasterTransporterProtocol {
  // Messages from Transporters
  case class RegisterTransporter(transporterId: String)
  case class DeRegisterTransporter(transporterId: String)
  case class TransporterRequestsTransport(transporterId: String)
  case class TransportIsDone(transporterId: String, transportId: String, result: String)
  case class TransportFailed(transporterId: String, transportId: String)

  // Messages to Transporters
  case object TransportIsReady
  case class Ack(id: String)
}

object TransportExecutorProtocol {
  case class DoTransport(n: String)
  case class TransportComplete(result: String)
  case class DoTransfer(transportId: String, n: String)
  case class TransferComplete(transportId: String, n: String)
  type TransportExecutorProps = () => Props
}

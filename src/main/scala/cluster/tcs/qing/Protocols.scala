package cluster.tcs.qing

import akka.actor.Props

case class Work(workId: String, job: Any)

case class WorkResult(workId: String, result: Any)


object MasterWorkerProtocol {
  // Messages from Workers
  case class RegisterWorker(workerId: String)
  case class DeRegisterWorker(workerId: String)
  case class WorkerRequestsWork(workerId: String)
  case class WorkIsDone(workerId: String, workId: String, result: Any)
  case class WorkFailed(workerId: String, workId: String)

  // Messages to Workers
  case object WorkIsReady
  case class Ack(id: String)
}

object WorkExecutorProtocol {
  case class DoWork(n: Any)
  case class WorkComplete(result: Any)
  type WorkExecutorProps = () => Props
}


// TRANSPORT

case class Transport(transportId: String, job: Any)

case class TransportResult(transportId: String, result: Any)


object MasterTransporterProtocol {
  // Messages from Transporters
  case class RegisterTransporter(transporterId: String)
  case class DeRegisterTransporter(transporterId: String)
  case class TransporterRequestsTransport(transporterId: String)
  case class TransportIsDone(transporterId: String, transportId: String, result: Any)
  case class TransportFailed(transporterId: String, transportId: String)

  // Messages to Transporters
  case object TransportIsReady
  case class Ack(id: String)
}

object TransportExecutorProtocol {
  case class DoTransport(n: Any)
  case class TransportComplete(result: Any)
  case class DoTransfer(transportId: String, n: Any)
  case class TransferComplete(transportId: String, n: Any)
  type TransportExecutorProps = () => Props
}

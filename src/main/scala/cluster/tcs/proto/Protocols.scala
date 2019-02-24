package cluster.tcs.proto

import akka.actor.Props

  //proto buffed
  //case class MasterAck(workId: String)

  case class Work(workId: String, job: String)
  case class WorkResult(workId: String, result: String)


  //# - object MasterWorkerProtocol {
  // Messages from Workers
  case class RegisterWorker(workerId: String)
  case class DeRegisterWorker(workerId: String)
  case class WorkerRequestsWork(workerId: String)
  case class WorkIsDone(workerId: String, workId: String, result: String)
  case class WorkFailed(workerId: String, workId: String)

  // Messages to Workers
  case object WorkIsReady
  case class WorkIsDoneAck(id: String)
//# - }

  // WorkExecutorProtocol
  case class ExecuteWork(n: String)
  case class WorkExecuted(result: String)

  object WorkExecutorProtocol {
    type WorkExecutorProps = () => Props
  }


// TRANSPORT

  case class Transport(transportId: String, job: String)
  case class TransportResult(transportId: String, result: String)


  //# - object MasterTransporterProtocol {
  // Messages from Transporters
  case class RegisterTransporter(transporterId: String)
  case class DeRegisterTransporter(transporterId: String)
  case class TransporterRequestsTransport(transporterId: String)
  case class TransportIsDone(transporterId: String, transportId: String, result: String)
  case class TransportFailed(transporterId: String, transportId: String)

  // Messages to Transporters
  case object TransportIsReady
  case class TransportAck(id: String)
  //# - }


  // TransportExecutorProtocol
  case class DoTransport(n: String)
  case class TransportComplete(result: String)
  case class DoTransfer(transportId: String, n: String)
  case class TransferComplete(transportId: String, n: String)

  object TransportExecutorProtocol {
    type TransportExecutorProps = () => Props
  }

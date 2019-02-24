package cluster.tcs.proto

import akka.actor.Props

  // #### - commented protocols are proto buffed, defined inside proto.proto

  // ## - WORK related protocols
  //case class MasterAck(workId: String)
  //case class Work(workId: String, job: String)
  //case class WorkResult(workId: String, result: String)

  // # - Messages from Workers
  //case class RegisterWorker(workerId: String)
  //case class DeRegisterWorker(workerId: String)
  //case class WorkerRequestsWork(workerId: String)
  //case class WorkIsDone(workerId: String, workId: String, result: String)
  //case class WorkFailed(workerId: String, workId: String)

  // # - Messages to Workers
  //case object WorkIsReady
  //case class WorkIsReady(workId: String)
  //case class WorkIsDoneAck(id: String)


  // # - WorkExecutorProtocol
  //case class ExecuteWork(n: String)
  //case class WorkExecuted(result: String)

  object WorkExecutorProtocol {
    type WorkExecutorProps = () => Props
  }


  // ## - TRANSPORT related protocols

  //case class Transport(transportId: String, job: String)
  //case class TransportResult(transportId: String, result: String)

  // # - Messages from Transporters
  //case class RegisterTransporter(transporterId: String)
  //case class DeRegisterTransporter(transporterId: String)
  //case class TransporterRequestsTransport(transporterId: String)
  //case class TransportIsDone(transporterId: String, transportId: String, result: String)
  //case class TransportFailed(transporterId: String, transportId: String)

  // # - Messages to Transporters
  //case object TransportIsReady
  //case class TransportIsReady(marker: String)
  //case class TransportAck(id: String)


  // # - TransportExecutorProtocol
  //case class DoTransport(n: String) - not in use
  //case class TransportComplete(result: String)
  //case class DoTransfer(transportId: String, n: String)
  //case class TransferComplete(transportId: String, n: String) - not in use

  object TransportExecutorProtocol {
    type TransportExecutorProps = () => Props
  }

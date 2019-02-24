package cluster.tcs.pubsub

import akka.actor.Props

  case class MasterAck(workId: String)
  case class Work(workId: String, job: String)
  case class WorkResult(workId: String, result: String)


//object MasterWorkerProtocol {
  // Messages from Workers
  case class RegisterWorker(workerId: String)
  case class DeRegisterWorker(workerId: String)
  case class WorkerRequestsWork(workerId: String)
  case class WorkIsDone(workerId: String, workId: String, result: String)
  case class WorkFailed(workerId: String, workId: String)

  // Messages to Workers
  case object WorkIsReady
  case class WorkIsDoneAck(id: String)
//}

  //WorkExecutorProtocol
  case class ExecuteWork(n: String)
  case class WorkExecuted(result: String)

object WorkExecutorProtocol {
  type WorkExecutorProps = () => Props
}

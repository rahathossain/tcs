package cluster.tcs

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

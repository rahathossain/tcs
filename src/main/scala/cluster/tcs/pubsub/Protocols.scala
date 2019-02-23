package cluster.tcs.pubsub

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

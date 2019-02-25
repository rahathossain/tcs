package cluster.tcs.qing

import scala.collection.immutable.Queue
import cluster.tcs.proto._

object WorkState {

  def empty: WorkState = WorkState(
    pendingWork = Queue.empty,
    workInProgress = Map.empty,
    acceptedWorkIds = Set.empty,
    doneWorkIds = Set.empty)
}

case class WorkState private(
                              private val pendingWork: Queue[Work],
                              private val workInProgress: Map[String, Work],
                              private val acceptedWorkIds: Set[String],
                              private val doneWorkIds: Set[String]) {



  def hasWork: Boolean = pendingWork.nonEmpty

  def nextWork: Work = pendingWork.head

  def isAccepted(workId: String): Boolean = acceptedWorkIds.contains(workId)

  def isInProgress(workId: String): Boolean = workInProgress.contains(workId)

  def isDone(workId: String): Boolean = doneWorkIds.contains(workId)

  def updated(event: WorkAccepted): WorkState = event match {
    case WorkAccepted(work_op) ⇒
      val work = work_op.get
      copy(
        pendingWork = pendingWork enqueue work,
        acceptedWorkIds = acceptedWorkIds + work.workId)
  }


  def updated(event: WorkStarted): WorkState = event match {
    case WorkStarted(workId) ⇒
      val (work, rest) = pendingWork.dequeue
      require(workId == work.workId, s"WorkStarted expected workId $workId == ${work.workId}")
      copy(
        pendingWork = rest,
        workInProgress = workInProgress + (workId -> work))
  }

  def updated(event: WorkCompleted): WorkState = event match {
    case WorkCompleted(workId, result) ⇒
      copy(
        workInProgress = workInProgress - workId,
        doneWorkIds = doneWorkIds + workId)
  }


  def updated(event: WorkerFailed): WorkState = event match {
    case WorkerFailed(workId) ⇒
      copy(
        pendingWork = pendingWork enqueue workInProgress(workId),
        workInProgress = workInProgress - workId)
  }


  def updated(event: WorkerTimedOut): WorkState = event match {
    case WorkerTimedOut(workId) ⇒
      copy(
        pendingWork = pendingWork enqueue workInProgress(workId),
        workInProgress = workInProgress - workId)
  }


}

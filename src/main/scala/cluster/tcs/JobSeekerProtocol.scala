package cluster.tcs

object JobSeekerProtocol {
  case object Seek
  case class JobFound(jobId: String)
}

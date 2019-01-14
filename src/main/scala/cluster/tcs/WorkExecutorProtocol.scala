package cluster.tcs

import akka.actor.Props

object WorkExecutorProtocol {
  case class DoWork(n: Int)
  case class WorkComplete(result: String)
  type WorkExecutorProps = () => Props
}

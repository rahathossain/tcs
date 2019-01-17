package cluster.tcs

import akka.actor.Props

object WorkExecutorProtocol {
  case class DoWork(n: Any)
  case class WorkComplete(result: Any)
  type WorkExecutorProps = () => Props
}

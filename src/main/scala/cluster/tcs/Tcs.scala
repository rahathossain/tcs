package cluster.tcs

import akka.actor.{ActorSystem, Props}
import akka.cluster.singleton.{ClusterSingletonProxy, ClusterSingletonProxySettings}
import cluster.client.Main.config

object Tcs {

  /**
    * Start a node with the role backend on the given port. (This may also
    * start the shared journal, see below for details)
    */
  def startCS(port: Int,
                   singletonName: String, singletonRole: String,
                   inTopic: String ,resultTopic: String): Unit = {
    val system = ActorSystem("ClusterSystem", config(port, singletonRole))
    MasterSingleton.startSingleton(system, singletonName, singletonRole, inTopic, resultTopic)
  }


  /**
    * Start a worker node, with n actual workers that will accept and process workloads
    *
    *
    */
  // #worker
  def startWorker(port: Int, workers: Int,
                  singletonName: String, singletonRole: String,
                  workExecutorProps: WorkExecutorProtocol.WorkExecutorProps): Unit = {
    val system = ActorSystem("ClusterSystem", config(port, "worker"))
    val masterProxy = system.actorOf(
          proxyProps(system, singletonName, singletonRole), name = "masterProxy")

    (1 to workers).foreach(n =>
      system.actorOf(Worker.props(masterProxy, workExecutorProps), s"worker-$n")
    )
  }
  // #worker

  // #proxy
  def proxyProps(system: ActorSystem, singletonName: String, singletonRole: String) =
    ClusterSingletonProxy.props(
      settings = ClusterSingletonProxySettings(system).withRole(singletonRole),
      singletonManagerPath = s"/user/$singletonName")
  // #proxy

}

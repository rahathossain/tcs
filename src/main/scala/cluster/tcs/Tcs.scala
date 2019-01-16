package cluster.tcs

import akka.actor.{ActorSystem, Props}
import akka.cluster.singleton.{ClusterSingletonProxy, ClusterSingletonProxySettings}
import com.typesafe.config.{Config, ConfigFactory}

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

  def config(port: Int, role: String): Config =
    ConfigFactory.parseString(s"""
      akka.remote.netty.tcp.port=$port
      akka.cluster.roles=[$role]
    """).withFallback(ConfigFactory.load())

}

class Tcs(port: Int, singletonName: String, singletonRole: String, val inTopic: String ,val resultTopic: String,
          workExecutorProps: WorkExecutorProtocol.WorkExecutorProps) {

  import Tcs._

  val system = ActorSystem("ClusterSystem", config(port, "front-end"))

  def startCS(port: Int) = Tcs.startCS(port, singletonName, singletonRole, inTopic, resultTopic)
  def startWorker(port: Int, workers: Int) =
            Tcs.startWorker(port, workers, singletonName, singletonRole, workExecutorProps)



  /**
    * Start a front end node that will submit work to the backend nodes
    */
  def startFrontEnd(frontEndProps:  (Props) => Props ) =
          system.actorOf(frontEndProps(proxyProps), "front-end")

  def proxyProps = Tcs.proxyProps(system, singletonName, singletonRole)

  def startResultConsumer(props: (String) => Props) = system.actorOf(props(resultTopic), "consumer")


  def pipeTo(transform: Any => Any, otherTcs: Tcs) =
    system.actorOf(WorkResultTransfer.props(transform, resultTopic, otherTcs.inTopic), "transfer-transform")

  def pipeTo(otherTcs: Tcs) =
      system.actorOf(WorkResultTransfer.props((_: Any)=>_ , resultTopic, otherTcs.inTopic), "transfer")

  def --> (otherTcs: Tcs) =
    system.actorOf(WorkResultTransfer.props((_: Any)=>_, resultTopic, otherTcs.inTopic), "transfer-forward")

  def <-- (otherTcs: Tcs) =
    system.actorOf(WorkResultTransfer.props((_: Any)=>_, otherTcs.resultTopic, inTopic), "transfer-backward")

}

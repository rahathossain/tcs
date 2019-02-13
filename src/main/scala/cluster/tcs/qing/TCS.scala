package cluster.tcs.qing

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration._

object TCS {

  // #singleton
  private def startSingleton(system: ActorSystem, name: String, role: String, in: String , out: String) = {
    val workTimeout = system.settings.config.getDuration("distributed-workers.work-timeout").getSeconds.seconds

    system.actorOf(
      ClusterSingletonManager.props(
        Master.props(id=name, workTimeout, in, out),
        PoisonPill,
        ClusterSingletonManagerSettings(system).withRole(role)
      ),
      name)
  }
  // #singleton


  // #worker
  private def startWorkerNode(system: ActorSystem, masterProxy: ActorRef, workers: Int,
                  singletonName: String, singletonRole: String,
                  workExecutorProps: WorkExecutorProtocol.WorkExecutorProps): Unit = {

    (1 to workers).foreach(n =>
      system.actorOf(Worker.props(masterProxy, workExecutorProps), s"worker-$n")
    )
  }
  // #worker

  // #transporter
  private def startTransporterNode(system: ActorSystem, masterProxy: ActorRef, transporters: Int,
                                   singletonName: String, singletonRole: String,
                                   transportExecutorProps: TransportExecutorProtocol.TransportExecutorProps): Unit = {
    (1 to transporters).foreach(n =>
      system.actorOf(Transporter.props(masterProxy, transportExecutorProps), s"transporter-$n")
    )
  }
  // #transporter

  // #proxy
  private def proxyProps(system: ActorSystem, singletonName: String, singletonRole: String) =
    ClusterSingletonProxy.props(
      settings = ClusterSingletonProxySettings(system).withRole(singletonRole),
      singletonManagerPath = s"/user/$singletonName")
  // #proxy

  private def config(port: Int, role: String): Config =
    ConfigFactory.parseString(s"""
      akka.remote.netty.tcp.port=$port
      akka.cluster.roles=[$role]
    """).withFallback(ConfigFactory.load())

}

class TCS(singletonName: String, singletonRole: String, val inTopic: String, val resultTopic: String ) {

  import TCS._


  /** ActorSystem --> ClusterSystem
    * #singletonRole
    *
    * Start a node with the role backend on the given port. (This may also
    * start the shared journal, see below for details)
    */
  def startMaster(port: Int) = {
    val system = ActorSystem("ClusterSystem", config(port, singletonRole))
    startSingleton(system, singletonName, singletonRole, inTopic, resultTopic)
  }



  def startWorker(port: Int, workersCount: Int,
                             workExecutorProps: WorkExecutorProtocol.WorkExecutorProps) = {
    val system = ActorSystem("ClusterSystem", config(port, "worker"))
    val masterProxy = system.actorOf(proxyProps(system, singletonName, singletonRole), name = "masterProxy")
    startWorkerNode(system, masterProxy, workersCount, singletonName, singletonRole, workExecutorProps)
    (system, masterProxy)
  }


  def startTransporter(system: ActorSystem, masterProxy: ActorRef, transportersCount: Int,
                       transportExecutorProps: TransportExecutorProtocol.TransportExecutorProps) = {
    startTransporterNode(system, masterProxy, transportersCount, singletonName, singletonRole, transportExecutorProps)
  }


  /*
  def startWorkerTransporter(port: Int, workersCount: Int, transportersCount: Int,
                             workExecutorProps: WorkExecutorProtocol.WorkExecutorProps,
                             transportExecutorProps: TransportExecutorProtocol.TransportExecutorProps) = {
    val system = ActorSystem("ClusterSystem", config(port, "worker"))
    val masterProxy = system.actorOf(proxyProps(system, singletonName, singletonRole), name = "masterProxy")

    startWorkerNode(system, masterProxy, workersCount, singletonName, singletonRole, workExecutorProps)

    startTransporterNode(system, masterProxy, transportersCount, singletonName, singletonRole, transportExecutorProps)

    (system, masterProxy)
  }
  */


  /** ActorSystem --> ClusterSystem
    * #helper-node
    *
    * all the below actors will be created under front-end node.
    */

  def helperNode(port: Int) = ActorSystem("ClusterSystem", config(port, "helper-node"))

  def masterProxyProps(system: ActorSystem, singletonName: String, singletonRole: String) =
            proxyProps(system, singletonName, singletonRole)

  def startFrontEnd(system: ActorSystem, frontEndProps:  (Props) => Props ) =
    system.actorOf(frontEndProps(proxyProps(system, singletonName, singletonRole)), "front-end")


  def startResultConsumer(system: ActorSystem, props: (String) => Props) =
    system.actorOf(props(resultTopic), "consumer")



}

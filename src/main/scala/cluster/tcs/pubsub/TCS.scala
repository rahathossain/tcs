package cluster.tcs.pubsub

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import cluster.tcs._
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
  private def startWorkerNode(system: ActorSystem, workers: Int,
                  singletonName: String, singletonRole: String,
                  workExecutorProps: WorkExecutorProtocol.WorkExecutorProps): Unit = {

    val masterProxy = system.actorOf(
          proxyProps(system, singletonName, singletonRole), name = "masterProxy")

    (1 to workers).foreach(n =>
      system.actorOf(Worker.props(masterProxy, workExecutorProps), s"worker-$n")
    )
  }
  // #worker

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

class TCS(port: Int, singletonName: String, singletonRole: String, val inTopic: String, val resultTopic: String,
          workExecutorProps: WorkExecutorProtocol.WorkExecutorProps) {

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

  /** ActorSystem --> ClusterSystem
    * #worker
    *
    * Start a worker node, with n actual workers that will accept and process workloads
    */
  def startWorker(port: Int, workers: Int) = {
    val system = ActorSystem("ClusterSystem", config(port, "worker"))
    startWorkerNode(system, workers, singletonName, singletonRole, workExecutorProps)
  }



  /** ActorSystem --> ClusterSystem
    * #front-end
    *
    * all the below actors will be created under front-end node.
    */
  val system = ActorSystem("ClusterSystem", config(port, "front-end"))

  def startFrontEnd(frontEndProps:  (Props) => Props ) =
    system.actorOf(frontEndProps(proxyProps(system, singletonName, singletonRole)), "front-end")


  def startResultConsumer(props: (String) => Props) =
    system.actorOf(props(resultTopic), "consumer")


  /*** Pipe To
    *
    * @param transform
    * @param fromTopic
    * @param toTopic
    * @return
    */
  private def pipeTo(transform: Any => Any, fromTopic: String, toTopic: String): ActorRef =
      system.actorOf(WorkResultTransfer.props(transform, fromTopic, toTopic) )

  def pipeTo(otherTcs: TCS): ActorRef = pipeTo((_: Any)=>_ , resultTopic, otherTcs.inTopic)
  def pipeTo(transform: Any => Any, otherTcs: TCS): ActorRef = pipeTo(transform, resultTopic, otherTcs.inTopic)


  def --> (otherTcs: TCS): ActorRef = pipeTo(otherTcs)
  def --> (transform: Any => Any, otherTcs: TCS): ActorRef = pipeTo(transform, otherTcs)

  def <-- (otherTcs: TCS): ActorRef = pipeTo((_: Any)=>_, otherTcs.resultTopic, inTopic)
  def <-- (transform: Any => Any, otherTcs: TCS): ActorRef = pipeTo(transform, otherTcs.resultTopic, inTopic)

  /*** Spray To - Splitter
    *
    * @param transform
    * @param fromTopic
    * @param toTopic
    * @return
    */

  private def sprayTo(transform: Any => Any, fromTopic: String, toTopic: String): ActorRef =
    system.actorOf(WorkResultSplitter.props(transform, fromTopic, toTopic) )

  def sprayTo(otherTcs: TCS): ActorRef = sprayTo((_: Any)=>_ , resultTopic, otherTcs.inTopic)
  def sprayTo(transform: Any => Any, otherTcs: TCS): ActorRef = sprayTo(transform, resultTopic, otherTcs.inTopic)


  /*** Router To - router
    *
    * @param transform
    * @param condition
    * @param fromTopic
    * @param eitherTopic
    * @param orTopic
    * @return
    */
  private def routeTo(transform: Any => Any, condition: Any => Boolean, fromTopic: String,
                      eitherTopic: String, orTopic: String): ActorRef =
    system.actorOf(WorkResultRouter.props(transform, condition, fromTopic, eitherTopic, orTopic) )

  def routeTo(condition: Any => Boolean, eitherTcs: TCS, orTcs: TCS): ActorRef =
    routeTo((_:Any)=>_, condition, this.resultTopic, eitherTcs.inTopic, orTcs.inTopic)

  def routeTo(transform: Any => Any, condition: Any => Boolean, eitherTcs: TCS, orTcs: TCS): ActorRef =
    routeTo(transform, condition, this.resultTopic, eitherTcs.inTopic, orTcs.inTopic)

}

package cluster.client

import java.io.File
import java.util.concurrent.CountDownLatch

import akka.actor.ActorSystem
import akka.persistence.cassandra.testkit.CassandraLauncher
import com.typesafe.config.{Config, ConfigFactory}

object Main {

  import AppConfig._



  def main(args: Array[String]): Unit = {

    startClusterInSameJvm()

    /*
    args.headOption match {

      case None =>
        startClusterInSameJvm()

      case Some(portString) if portString.matches("""\d+""") =>
        val port = portString.toInt
        if (backEndPortRange.contains(port)) startBackEnd(port)
        else if (frontEndPortRange.contains(port)) startFrontEnd(port)
        else startWorker(port, args.lift(1).map(_.toInt).getOrElse(1))

      case Some("cassandra") =>
        startCassandraDatabase()
        println("Started Cassandra, press Ctrl + C to kill")
        new CountDownLatch(1).await()

    }*/
  }




  def startClusterInSameJvm(): Unit = {
    startCassandraDatabase()

    // two backend nodes
    cluster.tcs.Tcs.startBackEnd(2561, singletonName2, singletonRole2, inTopic2 ,ResultsTopic2)
    cluster.tcs.Tcs.startBackEnd(2562, singletonName2, singletonRole2, inTopic2 ,ResultsTopic2)
    // two front-end nodes
    startFrontEnd2(3010)
    startFrontEnd2(3011)
    // two worker nodes with two worker actors each
    cluster.tcs.Tcs.startWorker(5011, 2, singletonName2, singletonRole2,  () => WorkExecutor2.props)
    cluster.tcs.Tcs.startWorker(5012, 2, singletonName2, singletonRole2,  () => WorkExecutor2.props)

    // two backend nodes
    cluster.tcs.Tcs.startBackEnd(2551, singletonName1, singletonRole1, inTopic1 ,ResultsTopic1)
    cluster.tcs.Tcs.startBackEnd(2552, singletonName1, singletonRole1, inTopic1 ,ResultsTopic1)
    // two front-end nodes
    startFrontEnd1(3000)
    startFrontEnd1(3001)
    // two worker nodes with two worker actors each
    cluster.tcs.Tcs.startWorker(5001, 2, singletonName1, singletonRole1,  () => WorkExecutor1.props)
    cluster.tcs.Tcs.startWorker(5002, 2, singletonName1, singletonRole1,  () => WorkExecutor1.props)

  }

  /**
   * Start a node with the role backend on the given port. (This may also
   * start the shared journal, see below for details)
   */
  //def startBackEnd(port: Int, resultsTopic: String): Unit =  cluster.tcs.Tcs.startBackEnd(port, resultsTopic)


  /**
   * Start a front end node that will submit work to the backend nodes
   */
  // #front-end
  def startFrontEnd1(port: Int): Unit = {
    val system = ActorSystem("ClusterSystem", config(port, "front-end"))
    system.actorOf(FrontEnd.props, "front-end")
    system.actorOf(WorkResultConsumer1.props, "consumer")
  }

  def startFrontEnd2(port: Int): Unit = {
    val system = ActorSystem("ClusterSystem", config(port, "front-end"))
    //system.actorOf(FrontEnd.props, "front-end")
    system.actorOf(WorkResultConsumer2.props, "consumer")
  }
  // #front-end

  /**
   * Start a worker node, with n actual workers that will accept and process workloads
   */
  // #worker
  //def startWorker(port: Int, workers: Int): Unit = cluster.tcs.Tcs.startWorker(port, workers, () => WorkExecutor1.props)
  // #worker

  def config(port: Int, role: String): Config =
    ConfigFactory.parseString(s"""
      akka.remote.netty.tcp.port=$port
      akka.cluster.roles=[$role]
    """).withFallback(ConfigFactory.load())

  /**
   * To make the sample easier to run we kickstart a Cassandra instance to
   * act as the journal. Cassandra is a great choice of backend for Akka Persistence but
   * in a real application a pre-existing Cassandra cluster should be used.
   */
  def startCassandraDatabase(): Unit = {
    val databaseDirectory = new File("target/cassandra-db")
    CassandraLauncher.start(
      databaseDirectory,
      CassandraLauncher.DefaultTestConfigResource,
      clean = false,
      port = 9042
    )

    // shut the cassandra instance down when the JVM stops
    sys.addShutdownHook {
      CassandraLauncher.stop()
    }
  }

}

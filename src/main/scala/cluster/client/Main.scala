package cluster.client

import java.io.File
import java.util.concurrent.CountDownLatch

import akka.actor.ActorSystem
import akka.persistence.cassandra.testkit.CassandraLauncher
import cluster.tcs.Tcs
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

    val tcs2 = new Tcs(3010, singletonName2, singletonRole2, inTopic2 ,ResultsTopic2, () => WorkExecutor2.props )
    // two backend nodes
    tcs2.startCS(2561)
    tcs2.startCS(2562)


    tcs2.startResultConsumer(WorkResultConsumer.props)

    //tcs2.startFrontEnd()

    // two worker nodes with two worker actors each
    tcs2.startWorker(5011, 2)
    tcs2.startWorker(5012, 2)


    val tcs1 = new Tcs(3000, singletonName1, singletonRole1, inTopic1 ,ResultsTopic1, () => WorkExecutor1.props )
    // two backend nodes
    tcs1.startCS(2551)
    tcs1.startCS(2552)
    // two front-end nodes

    tcs1.startFrontEnd(FrontEnd.props)

    tcs1.pipeTo(transform1, tcs2)

    tcs1  --> (transform1, tcs2)

    tcs1  --> (transform1, tcs2)

    // two worker nodes with two worker actors each
    tcs1.startWorker(5001, 2 )
    tcs1.startWorker(5002, 2)

  }

  /**
   * Start a node with the role backend on the given port. (This may also
   * start the shared journal, see below for details)
   */


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

  def transform1(result: Any): Int = {
    val i = result.toString.indexOf('=')
    val x = result.toString.substring(i+1).trim
    x.toInt
  }

}

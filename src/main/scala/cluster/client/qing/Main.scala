package cluster.client.qing

import java.io.File

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.singleton.{ClusterSingletonProxy, ClusterSingletonProxySettings}
import akka.persistence.cassandra.testkit.CassandraLauncher
import cluster.tcs.qing.TCS

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

    //TCS2
    val tcs2 = new TCS(singletonName2, singletonRole2, inTopic2 ,ResultsTopic2 )
    (2561 to 2562).map( tcs2.startMaster( _ ) )
    (5011 to 5012).map { p =>
      val (x, y) = tcs2.startWorker( p , workersCount = 2, () => WorkExecutor2.props)
      tcs2.startTransporter(x, y, transportersCount = 2, () => TransportExecutor2.props )
      true
    }

    //tcs2.startResultConsumer(WorkResultConsumer.props)
    //tcs2.startFrontEnd()


    // #TCS1
    val tcs1 = new TCS(singletonName1, singletonRole1, inTopic1 ,ResultsTopic1 )

    (2551 to 2554).map( tcs1.startMaster( _ ) )

    (5001 to 5002).map { p =>
      val (x, y) = tcs1.startWorker( p , workersCount = 2, () => WorkExecutor1.props)
      val pm2 = tcs1.masterProxyProps(x, singletonName2, singletonRole2)

      tcs1.startTransporter(x, y, transportersCount = 2,
        () => TransportExecutor1.props(pm2)
      )
      true
    }
    tcs1.startFrontEnd(tcs1.helperNode(port=3000), FrontEnd.props)

  }

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

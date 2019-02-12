package cluster.client.qing

import java.io.File

import akka.actor.Props
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


    val tcs1 = new TCS(port=3000, singletonName1, singletonRole1, inTopic1 ,ResultsTopic1 )

    startMasterWorkerTransporter(tcs1,
      masterPorts=2551 to 2554, workPorts=5001 to 5002, count=2,
      () => WorkExecutor1.props,
      () => TransportExecutor1.props(tcs1.masterProxyProps(singletonName2, singletonRole2)) )

    tcs1.startFrontEnd(FrontEnd.props)

    //TransportExecutor2

    val tcs2 = new TCS(3010, singletonName2, singletonRole2, inTopic2 ,ResultsTopic2 )
    startMasterWorkerTransporter(tcs2,
      masterPorts=2561 to 2562, workPorts=5011 to 5012, count=2,
      () => WorkExecutor2.props,
      () => TransportExecutor2.props )

    //tcs2.startResultConsumer(WorkResultConsumer.props)
    //tcs2.startFrontEnd()

  }


  def startMasterWorkerTransporter(tcs: TCS, masterPorts: Range, workPorts: Range, count: Int,
                                   workerExec: () => Props , transExec: () => Props) = {

    masterPorts.map(tcs.startMaster( _ ) )

    workPorts.map( tcs.startWorkerTransporter(_, count, workerExec, transExec) )

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

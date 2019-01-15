Team of Cluster Singleton
=========================

# What is cluster singleton ?

Cluster Singleton manages one singleton actor instance among all cluster nodes or a group of nodes tagged with a specific role.
In akka, the cluster singleton is a pattern, implemented by `akka.cluster.singleton.ClusterSingletonManager`. 

<https://doc.akka.io/docs/akka/2.5/cluster-singleton.html>

# What is TCS  

TCS is a simple API using cluster singleton (e.g. Cluston) with following characteristics

1. TCS have one Cluston with persistent actor and Worker actors but no Work Executors 
2. TCS subscribe to a supplied topic (inTopic) and publish to another topic (resultsTopic)   
3. TCS has a persistentID, TCS uses the supplied singletonName as  persistentID
4. TCS r
4. TCS does provide protocol messages for the WorkExecutors 
5. TCS expect number of Worker count e.g. `workerCount` 

# Parameters for TCS

To start cluster singleton of TCS, need to supply:
 * port 
 * singletonName as String 
 * singletonRole name as String 
 * inTopic as String 
 * resultsTopic as String  

```scala 
def startCS(port: Int,
            singletonName: String, singletonRole: String,
            inTopic: String ,resultTopic: String)
``` 

To start Workers of TCS, need to supply:
 * port 
 * number of workers 
 * singletonName as String 
 * singletonRole name as String 
 * inTopic as String 
 * resultsTopic as String  
 * call back function that supply WorkExecutorProps 

```scala 
def startWorker(port: Int, workers: Int,
                singletonName: String, singletonRole: String,
                workExecutorProps: WorkExecutorProtocol.WorkExecutorProps)
``` 




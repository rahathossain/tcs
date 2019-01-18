Team of Cluster Singleton
=========================

# What is cluster singleton ?

Cluster Singleton manages one singleton actor instance among all cluster nodes or a group of nodes tagged with a specific role.
In akka, the cluster singleton is a pattern, implemented by `akka.cluster.singleton.ClusterSingletonManager`. 

<https://doc.akka.io/docs/akka/2.5/cluster-singleton.html>

# What is TCS  

TCS is a simple API using cluster singleton (e.g. Cluston) with following characteristics

1. TCS have one Cluston with persistent actor and Worker actors but no Work Executor, so you have the option to write your own custom Work Executor 
2. TCS subscribe to a supplied topic `inTopic` and publish to another topic `resultsTopic`   
3. TCS perform the work request what ever posted on `inTopic` and publish result on `resultsTopic` 
4. TCS has a persistentID, TCS uses the supplied singletonName as  persistentID
5. TCS does provide protocol messages to be used for the your WorkExecutors 
6. TCS expect number of Worker count e.g. `workerCount` 
7. One TCS can forward it's results to another TCS by piping it's `resultsTopic` to other's `inTopic` and vice versa 

# Parameters for TCS


TCS requires following parameters: 
 * port 
 * number of workers 
 * singletonName as String 
 * singletonRole name as String 
 * inTopic as String 
 * resultsTopic as String  
 * call back function that supply WorkExecutorProps 

The port number used for TCS common task node. Other than common task node, 
TCS got Master nodes and Worker nodes.

Master node also called Cluster Singleton node or Cluston nodes which is persistent node, 
where one node is active at a time and rest of the nodes are standby in a cluster. 

Worker nodes are all active and running and they work in parallel, 
which are running same or multiple machines.  

```scala 
class Tcs(port: Int, singletonName: String, singletonRole: String, val inTopic: String ,val resultTopic: String,
          workExecutorProps: WorkExecutorProtocol.WorkExecutorProps)
```

To start one Master node (or cluster singleton node) of TCS, you need to supply:
 * port  
  
You may need multiple Master nodes. If you want to spin up multiple Master node on same machine, then
obviously use different port to avoid conflict.  
  
```scala
def startCS(port: Int)
``` 

To start Workers of TCS, need to supply:
 * port 
 * number of workers in each node 

You may need multiple Worker nodes. If you want to spin up multiple Worker node on same machine, then
obviously use different port to avoid conflict. One node can have multiple worker, which can be 
configured by just providing the 2nd parameter.   

```scala 
def startWorker(port: Int, workers: Int) 
``` 

# TCS connector functions  
* create more actors and utility:
  - pipeTo or --> , can be used to connect two TCS, tcs1 and tcs2. `tcs1 --> tcs2` means, 
     it copy result from tcs1.resultsTopic to tcs2.inTopic. payload can be transformed 
     while copying using `tcs1 --> (transform, tcs2) ` where `transform` is Any => Any     
      
  - sprayTo or --E, is basically splitter. This can be used if we have `List[Any]` as `tcs1.resultsTopic`
    and we want extract the values out of the list and put onto `tcs2.inTopic` one by one. function can be
    called as `tcs1 --E tcs2`  or along with transform `tcs1 --E (transform, tcs2)`
    
  - routeTo -- TODO    
  - 
Team of Cluster Singleton
=========================

# What is Team of Cluster Singleton or TCS?

TCS is reusable, resilient/fault tolerant, highly available and durable software component 
to design special micro-services based cluster solution.

A TCS is clustered group of micro-services, capable of performing some type of custom work in parallel 
using its worker nodes while also able to manage these works using its master node, and also provide options 
to communicate to other TCS and external clients via helper nodes.

### TCS Master node: only one active master node 
The active master node of TCS, is the coordinator of all works perform the TCS. As an example: with the help of 
the active master node, we can make sure that, a single work gets done only once. 
By design there is only one active master node at any single point of time in a running cluster.  
However, design also ensures that the active master node is not the single point of failure, by providing 
the options to run at multiple instances of any TCS which would introduce some standby master nodes. 
On failure of active master node, a stand-by master node can take over its place automatically.

### TCS - under the hood 
Under the hood, TCS is actor based AKKA clustered module backed by apache Cassandra cluster, 
and the master node follows AKKA cluster singleton pattern which ensures only one active node tagged 
with same role name in a cluster. In the core of the master node there is persistent actor, which persist 
all work events on Cassandra cluster and stand-by mode and stand-by master node can recover based on replying 
the event or saved snapshot using Event sourcing model. 

A TCS communicate with other TCS via pub sub model. Each TCS has two topics: input Topic and result topic. 
Idea is simple, a TCS perform any work whichever it finds in its input topic and finally put the result in 
its result topic. When we connect multiple TCS, then the connector just copy the result from result topic of 
one TCS and put that to input Topic of another TCS. Note that, when any TCS got any work onto its input topic, 
the active master node only accepts the valid work which are not done yet based on work id, 
to prevent duplicate work. Worker nodes registered themselves with the master node, so that 
master node can assign work. Depends on the load of work, we can create more workers for that TCS, so that 
work can be done in parallel.

### TCS conclusion 
Finally we can say, team of cluster singleton or TCS is a cluster of distributed actors 
around the cluster singleton actor, which may perform a specific type of work. 
TCS can be highly available, resilient, elastic and distributed. Note that, cluster singleton alone can't 
do the complete unit of work in parallel, it require worker and helpers actors, that's why the we need a team, 
which we call team of cluster singleton.    

### What is cluster singleton ?

Cluster Singleton manages one singleton actor instance among all cluster nodes or 
a group of nodes tagged with a specific role.
In akka, the cluster singleton is a pattern, implemented by `akka.cluster.singleton.ClusterSingletonManager`. 

<https://doc.akka.io/docs/akka/2.5/cluster-singleton.html>

# TCS API 

TCS is a simple API using cluster singleton (e.g. Cluston) with following characteristics:

1. TCS have one Cluston with persistent actor and Worker actors but no Work Executor, 
    so you have the option to write your own custom Work Executor 
2. TCS subscribe to a supplied topic `inTopic` and publish to another topic `resultsTopic`   
3. TCS perform the work request what ever posted on `inTopic` and publish result on `resultsTopic` 
4. TCS has a persistentID, TCS uses the supplied singletonName as  persistentID
5. TCS does provide protocol messages to be used for the your WorkExecutors 
6. TCS expect number of Worker count e.g. `workerCount` 
7. One TCS can forward it's results to another TCS by piping it's `resultsTopic` to other's `inTopic` by using
   `pipeTo` function. TCS also have `routeTo` function to perform routing results between two TCSs  
   and also `sprayTo` function which is just work as a splitter.  
       

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
  - pipeTo (or `-->`) , can be used to connect two TCS, tcs1 and tcs2. `tcs1 --> tcs2` means, 
     it copy result from tcs1.resultsTopic to tcs2.inTopic, which is input topic for tcs2. 
     pipeTo is overloaded function, it support transformation of payload as well while passing
     payload from `tcs1` to `tcs2`. To transform payload while copying use 
     `tcs1 --> (transform, tcs2) ` where `transform` is Any => Any     
      
  - sprayTo, is basically splitter. This can be used if we have `List[Any]` as `tcs1.resultsTopic`
    and we want extract the values out of the list and put onto `tcs2.inTopic` one by one. function can be
    called as `tcs1 sprayTo tcs2`  or along with transform `tcs1 sprayTo (transform, tcs2)`
    
  - routeTo, provide an option to send result payload of `tcs1` to either `tcs2` or `tcs3` based on supplied 
     condition i.e. `routeTo(condition, tcs2, tcs3)` , like above functions, this is also an overloaded 
     function and overloaded version support transformation, `routeTo(transform, conditition, tcs2, tcs3)`
     where `transform` is `Any => Any` and `condition` is `Any => Boolean`
   
  
# Inspiration of TCS 
Inspiration of TCS came from lightbend demo project, 
<https://developer.lightbend.com/guides/akka-distributed-workers-scala/>

 The above demo shows, how to create cluster singleton `Master` nodes, where only one `Master` node
 is active in a cluster and rest of the `Master` nodes are standby. All the `Worker` nodes in the 
 cluster register themselves with `Master` node. `Master` node assign work to registered `Worker` nodes. 
 However, `Worker` nodes are actually middle manager, they spawn `WorkExecutor` actors which does the actual work. 
 `FrontEnd` node holds the functionally to submit the work request to `Master` nodes and also consumed the result 
 of the requested work which is performed by `Master` and `Worker` nodes. 
 
 The above demo can perform one type of work. If we need to perform series of different types of work one after 
 another then we simply need multiple copy of the above module and wire them together. TCS tries to provide that
 cascading functionality out of the box. 
 
 ## Protocols 
 
 `case class Work(workId: String, job: Any)`
   * Produced By: FrontEnd, WorkResultRouter, WorkResultSplitter, WorkResultTransfer
   * Forward By: Master   
   * Consumed By: Worker
 
 
 `case class WorkResult(workId: String, result: Any)`
   * Produced By: Master
   * Consumed By: WorkResultConsumer, WorkResultRouter, WorkResultSplitter, WorkResultTransfer 

 
# TODO
* At the moment there's no timeout options for TCS worker.
* TTL Setting for persistent Journal  

## TTL Setting [TODO] 
Cleanup of tag_views table
By default the tag_views table keeps tagged events indefinitely, even when the original events have been removed. 
Depending on the volume of events this may not be suitable for production.

Before going live decide a time to live (TTL) and, if small enough, consider using the Time Window Compaction Strategy. 
See `events-by-tag.time-to-live` in reference.conf for how to set this.

## Pub Sub model issue [TODO] 
Message can get lost in DistributedPubSubMediator. 
add custom Transporter along with or other than DistributedPubSubMediator or use Kafka Topic  

There maybe two ways:

*First way is:* 
    Use the same retry technique as Front End. 
    Front End is Producer and Master is consumer in this case. 
    Front End (Producer in this case) sends a work messages to Master and wait for ACK for certain time
    and if no ACK received then it resend the work message. 
    Master (consumer in this case) discard any duplicate work message but provide ACK for each work message received from Front End. 

Same way, Master (Producer in the new case) need to send Response message to ResultConsumer with timeout value and resend if no ACK received within time frame and ResultConsumer need to discard duplicate but response with ACK for the work request received. 
  
*Second Way:*
Kafka  


## Delay Configs  


### FrontEnd Delay configs [TODO]
* initialDelay = 5
* nextTickLowerLimit = 3
* nextTickUpperLimit = 10 
* retryTimeout = 5
* sendWorkTimout = 5
```conf

front-end {    
        initial-delay-for {
            any = 5
            tcs1 = 5
        }
        next-tick-lower-limit-for {
            any = 3
            tcs1 = 3
        }
        next-tick-upper-limit-for {
            any = 10
            tcs1 = 10
        }
        retry-timeout-for {
            any = 5
            tcs1 = 5
        }
        send-work-timout-for {
            any = 3
            tcs1 = 3
        }        
        
}

```

### Master Delay configs [TODO]
* distributed-workers.consider-worker-dead-after = inside `application.conf` file 
* front-end.missed-work-checking-interval = 60 [TODO]  

### Worker Delay configs [TODO]
* distributed-workers.worker-registration-interval = inside `application.conf` file
* workIsDoneNotificationToMasterTimeout = 5 (currently hard coded inside Worker)
* distributed-workers.worker-master-ack-timeout = 5 [TODO]   

### config files
Delay configs inside `application.conf` file 
 
```conf
# Configuration related to the app is in its own namespace

distributed-workers {

  # Each worker pings the master with this interval
  # to let it know that it is alive
  worker-registration-interval = 10s
  
  # If a worker hasn't gotten in touch in this long
  # it is removed from the set of workers
  consider-worker-dead-after = 60s

  # If a workload hasn't finished in this long it
  # is considered failed and is retried
  work-timeout = 10s
}
``` 

## Other TCS Configs



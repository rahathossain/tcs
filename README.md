Team of Cluster Singleton
=========================

# What is cluster singleton ?

In akka, the cluster singleton is a pattern, implemented by `akka.cluster.singleton.ClusterSingletonManager`. It manages one singleton actor instance among all cluster nodes or a group of nodes tagged with a specific role.

<https://doc.akka.io/docs/akka/2.5/cluster-singleton.html>

# What is TCS  

TCS is a simple API using cluster singleton (e.g. Cluston) with following characteristics

1. TCS have one Cluston with persistent actor and Worker actors but no Work Executors 
2. TCS subscribe to a topic (inputTopic) and publish to another topic (resultTopic) with default topic name `inTopic` and `outTopic`  
3. TCS has a persistentID with default name `master`
4. TCS does provide protocol messages for the WorkExecutors 
5. TCS expect number of Worker count e.g. `workerCount` 

 


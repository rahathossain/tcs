package cluster.client.pubsub

object AppConfig {

  // note that 2551 and 2552 are expected to be seed nodes though, even if
  // the back-end starts at 2000
  val backEndPortRange = 2000 to 2999

  val frontEndPortRange = 3000 to 3999

  val singletonName1 = "pub-sub-master1"
  val singletonRole1 = "pub-sub-back-end1"
  val inTopic1 = "pub-sub-inTopic1"
  val ResultsTopic1 = "pub-sub-outTopic1"

  val singletonName2 = "pub-sub-master2"
  val singletonRole2 = "pub-sub-back-end2"
  val inTopic2 = "pub-sub-inTopic2"
  val ResultsTopic2 = "pub-sub-outTopic2"
}

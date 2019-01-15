package cluster.client

object AppConfig {

  // note that 2551 and 2552 are expected to be seed nodes though, even if
  // the back-end starts at 2000
  val backEndPortRange = 2000 to 2999

  val frontEndPortRange = 3000 to 3999

  val singletonName1 = "master1"
  val singletonRole1 = "back-end1"
  val inTopic1 = "inTopic1"
  val ResultsTopic1 = "outTopic1"

  val singletonName2 = "master2"
  val singletonRole2 = "back-end2"
  val inTopic2 = "inTopic2"
  val ResultsTopic2 = "outTopic2"
}

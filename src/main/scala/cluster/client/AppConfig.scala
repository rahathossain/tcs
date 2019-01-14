package cluster.client

object AppConfig {

  // note that 2551 and 2552 are expected to be seed nodes though, even if
  // the back-end starts at 2000
  val backEndPortRange = 2000 to 2999

  val frontEndPortRange = 3000 to 3999


  val ResultsTopic1 = "myResultTopic1"
}

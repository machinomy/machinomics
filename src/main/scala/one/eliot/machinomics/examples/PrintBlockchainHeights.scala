package one.eliot.machinomics.examples

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import one.eliot.machinomics.net.{Herd, Node, Testnet3Network}

object PrintBlockchainHeights extends App {
  implicit val actorSystem = ActorSystem("foo")
  val network = Testnet3Network
  val node = actorSystem.actorOf(Node.props(network))
  node ! Node.Start()
}

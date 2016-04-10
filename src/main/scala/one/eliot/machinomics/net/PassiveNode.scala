package one.eliot.machinomics.net

import java.net.{Inet4Address, InetSocketAddress}

import akka.actor.{ActorLogging, ActorRef, Actor}

import scala.util.Random

class PassiveNode(network: Network) extends Actor with Node with ActorLogging {

  var peers: Seq[ActorRef] = Seq.empty

  implicit val ec = context.dispatcher

  override def receive = {
    case Node.Start() => {
      log.info(s"Starting passive node for ${network.name}")
      network.peers().onSuccess {
        case addresses: Seq[InetSocketAddress] =>
          val selected = Random.shuffle(addresses.filter(_.getAddress.isInstanceOf[Inet4Address])).take(5)
          peers = for (addr <- selected) yield context.actorOf(Peer.props(addr, self, network))
      }
    }
    case Node.Stop() => log.info(s"Stopping passive node for ${network.name}")
  }
}

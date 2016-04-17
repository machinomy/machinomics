package one.eliot.machinomics.net

import java.net.{Inet4Address, InetSocketAddress}

import akka.actor.{ActorLogging, ActorRef, Actor}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.util.Random

class Herd(network: Network) extends Actor with ActorLogging {

  var peers: Seq[ActorRef] = Seq.empty

  implicit val ec = context.dispatcher

  implicit val timeout = Timeout(1.hour)

  override def receive = {
    case Herd.Connect(peerCount: Int) =>
      val s = sender()
      log.info(s"Starting passive node for ${network.name}")
      network.peers().onSuccess {
        case addresses: Seq[InetSocketAddress] =>
          val selected = Random.shuffle(addresses.filter(_.getAddress.isInstanceOf[Inet4Address])).take(peerCount)
          peers = for (addr <- selected) yield context.actorOf(Peer.props(addr, network))
          s ! Herd.DidConnect()
      }

    case Herd.Handshake() =>
      log.info(s"Start handshaking with ${peers.length} peers")
      val futures = peers.map(peer => peer ? Peer.ConnectCommand)
      Await.ready(Future.sequence(futures), Duration.Inf)

      for (elem <- futures) log.debug(elem.toString)
      sender() ! Herd.DidHandshake()

    case Herd.Disconnect() => log.info(s"Stopping passive node for ${network.name}")
  }
}

object Herd {
  sealed trait Message
  case class Connect(peerCount: Int = 5) extends Message
  case class Handshake() extends Message
  case class Disconnect() extends Message

  sealed trait Notification extends Message
  case class DidConnect() extends Notification
  case class DidHandshake() extends Notification
}

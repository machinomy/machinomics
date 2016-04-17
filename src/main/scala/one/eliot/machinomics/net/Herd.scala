package one.eliot.machinomics.net

import java.net.{Inet4Address, InetSocketAddress}

import akka.actor.{Actor, ActorLogging}
import one.eliot.machinomics.net.discovery.PeerDiscovery

import scala.util.Random

class Herd(network: Network) extends Actor with ActorLogging {

  implicit val ec = context.dispatcher

  override def receive = {
    case Herd.Connect(peerCount) =>
      val s = sender()
      log.info(s"Starting passive node for ${network.name}")
      val discovered = PeerDiscovery.forNetwork(network)
      discovered.onSuccess {
        case addresses: Seq[InetSocketAddress] =>
          val selected = Random.shuffle(addresses.filter(_.getAddress.isInstanceOf[Inet4Address])).take(peerCount)
          val peers = for (addr <- selected) yield context.actorOf(Peer.props(addr, network))
          val initialState = HerdState.MayBe(s, network, peers.toSet)
          context.become(onConnecting(initialState))
          notify(initialState, Herd.DidConnect())
          log.info("ffooo")
          log.info(s.toString())
      }

    case Herd.Disconnect() => log.info(s"Stopping passive node for ${network.name}")
  }

  def onConnecting(state: HerdState.HerdState): Receive = {
    case Herd.Handshake() =>
      log.info(s"Start handshaking with ${state.mayBePeers.size} peers")
      for (peer <- state.mayBePeers) { peer ! Peer.ConnectCommand() }
      next(state)

    case Peer.DidAcknowledge(peerState) =>
      notify(state, Herd.DidHandshake())
      next(HerdState.Factory.connect(state, sender()))
  }

  def onConnected(state: HerdState.HerdState): Receive = {
    case Herd.GetHeaders() =>
      for (peer <- state.connectedPeers) {
        peer ! Peer.HeadersQuery()
        next(HerdState.Factory.sendForHeaders(state, peer))
      }
  }

  def onGettingHeaders(state: HerdState.HerdState): Receive = {
    case Peer.GotHeaders(peerState, headers) =>
      log.info(s"Getting ${headers.length} headers")
      peerState match {
        case peerState: PeerState.GettingHeaders => next(state)
        case peerState: PeerState.Acknowledged =>
          notify(state, Herd.GotHeaders())
          next(HerdState.Factory.finishGettingHeader(state, sender()))
      }
  }

  def onSomethingUnexpected(state: HerdState.HerdState): Receive = {
    case x: Any =>
      log.error(x.toString)
      throw new Herd.UnexpectedBehavior(state)
  }

  def next(state: HerdState.HerdState): Unit = {
    state match {
      case state: HerdState.MayBe => context.become(onConnecting(state) orElse onSomethingUnexpected(state))
      case state: HerdState.Connected => context.become(onConnecting(state) orElse onSomethingUnexpected(state))
      case state: HerdState.GettingHeaders => context.become(onGettingHeaders(state) orElse onConnected(state) orElse onSomethingUnexpected(state))
      case _ => onSomethingUnexpected(state)
    }
  }

  def notify(state: HerdState.HerdState, message: Herd.Message) = state.node ! message

}

object Herd {
  sealed trait Message
  case class Connect(peerCount: Int = 5) extends Message
  case class Handshake() extends Message
  case class GetHeaders() extends Message
  case class Disconnect() extends Message

  sealed trait Notification extends Message
  case class DidConnect() extends Notification
  case class DidHandshake() extends Notification
  case class GotHeaders() extends Notification

  class UnexpectedBehavior(state: HerdState.HerdState) extends Throwable
}

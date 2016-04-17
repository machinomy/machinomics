package one.eliot.machinomics.net

import java.net.InetSocketAddress

import akka.actor._
import akka.io.{IO, Tcp}
import akka.util.{ByteString, ByteStringBuilder}
import one.eliot.machinomics.blockchain.BlockHeader
import one.eliot.machinomics.net.protocol._
import scodec.Attempt.{Failure, Successful}
import scodec._
import scodec.bits._

class Peer(remote: InetSocketAddress, network: Network) extends Actor with ActorLogging {

  import Tcp._
  import context.system

  var _blockHeaderCount = 0

  override def receive = { case Peer.ConnectCommand() =>
    IO(Tcp) ! Connect(remote)
    val initialState = PeerState.Initial(network, NetworkAddress(remote, network), sender)
    context.become(onConnected(initialState) orElse onFailedConnection(initialState) orElse onSomethingUnexpected(initialState))
  }

  def onConnected(prevState: PeerState.Initial): Receive = { case c @ Connected(r, l) =>
    log.info(s"DidConnect to $r")
    sender ! Register(self)
    val currentState = prevState.connected(sender)
    notify(currentState, Peer.DidConnect(currentState))
    sendMessage(prevState.network, currentState, protocol.VersionPayload(prevState.network, prevState.address))
    next(onVersionPayloadReceived, currentState)
  }

  def onVersionPayloadReceived(prevState: PeerState.Connected): Receive = onMessageReceived[protocol.VersionPayload](prevState) { payload =>
    log.info(s"Done version handshake with ${prevState.address}")
    val currentState = prevState.registered(
      selfReportedAddress = payload.myAddress,
      services = payload.services,
      version = payload.version,
      userAgent = payload.userAgent,
      height = payload.height
    )
    notify(currentState, Peer.DidRegister(currentState))
    sendMessage(prevState.network, currentState, protocol.VerackPayload())
    next(onVerackPayloadReceived, currentState)
  }

  def onVerackPayloadReceived(prevState: PeerState.Registered): Receive = onMessageReceived[protocol.VerackPayload](prevState) { payload =>
    log.info(s"Acknowledged connection to ${prevState.address}")
    val currentState = prevState.acknowledged
    notify(currentState, Peer.DidAcknowledge(currentState))
    next(receiveAcknowledged, currentState)
  }

  def receiveAcknowledged(prevState: PeerState.Acknowledged): Receive = {
    case Peer.HeadersQuery() =>
      log.info("Peer:receiveAcknowledged, case Peer.HeadersQuery")
      val currentState = prevState.gettingHeaders
      sendMessage(prevState.network, currentState, protocol.GetHeadersPayload(prevState.network.genesisHash))
      next(onHeadersReceive, currentState)
  }

  def onHeadersReceive(state: PeerState.GettingHeaders): Receive = onMessageReceived[protocol.HeadersPayload](state) { payload =>
    log.info(s"downloaded: ${state.downloadedHeaderCount} headers")
    if (state.downloadedHeaderCount < state.height) {
      val headersSent = payload.headers.takeRight(10)
      log.info(headersSent.map(x => x.hash).mkString("; "))
      log.info(s"last: ${payload.headers.last.hash.toString}, ${payload.headers.last} ${ByteString(payload.headers.last.hash.toString)}")
      val currentState = state.next(payload.count)
      sendMessage(state.network, currentState, protocol.GetHeadersPayload(headersSent.map(_.hash)))
      next(onHeadersReceive, currentState)
      notify(currentState, Peer.GotHeaders(currentState, payload.headers))
    } else {
      log.info(s"last: ${payload.headers.last.hash}, ${payload.headers.last}")
      val currentState = state.acknowledged
      notify(currentState, Peer.GotHeaders(currentState, payload.headers))
    }
  }

  def onFailedConnection(state: PeerState.Initial): Receive = { case CommandFailed(_: Connect) =>
    log.error(s"Can not connect to ${state.address}")
  }

  def onSomethingUnexpected(state: PeerState.PeerState): Receive = {
    case e: Received => log.error(e.toString) //throw new Peer.ReceivedUnexpectedBytesError(e.data, state)
  }

  def next[A <: PeerState.PeerState](behavior: A => Receive, state: A): Unit = context.become(behavior(state) orElse onSomethingUnexpected(state))

  def notify(state: PeerState.PeerState, message: Peer.Message) = state.herd ! message

  def onMessageReceived[A <: Payload : Codec](state: PeerState.PeerState)(f: A => Unit, buffer: ByteStringBuilder = ByteString.newBuilder): Receive = { case Received(blob) =>
    buffer.append(blob)
    protocol.Message.decode[A](buffer.result()) match {
      case Successful(DecodeResult(message, _)) =>
        buffer.clear()
        log.info(s"Received ${message.payload.command} message")
        f(message.payload)
      case Failure(e) =>
        context.become(onMessageReceived(state)(f, buffer) orElse onSomethingUnexpected(state))
    }
  }

  def sendMessage[A <: protocol.Payload : Codec](network: Network, state: PeerState.PeerState with PeerState.Wired, payload: A) = {
    val message = protocol.Message(network, payload)
    log.info(s"Sending $payload on $network")
    for {
      bits <- Codec.encode(message)
    } yield state.wire ! Write(bits)
  }

  implicit def bitVectorToByteString(bits: BitVector): ByteString = ByteString(bits.toByteArray)
}

object Peer {
  def props(remote: InetSocketAddress, network: Network) = Props(classOf[Peer], remote, network)

  sealed trait Message
  sealed trait Command extends Message
  case class ConnectCommand() extends Command
  sealed trait Query extends Message
  case class HeadersQuery() extends Query

  sealed trait Notification extends Message
  case class DidConnect(state: PeerState.Connected) extends Notification
  case class DidRegister(state: PeerState.Registered) extends Notification
  case class DidAcknowledge(state: PeerState.Acknowledged) extends Notification
  case class GotHeaders(state: PeerState.PeerState, headers: List[BlockHeader]) extends Notification

  case class Failed(state: PeerState.PeerState) extends Notification

  class ReceivedUnexpectedBytesError(byteString: ByteString, state: PeerState.PeerState) extends Throwable
}

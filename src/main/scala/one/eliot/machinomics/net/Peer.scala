package one.eliot.machinomics.net

import java.net.InetSocketAddress

import akka.actor._
import akka.io.{IO, Tcp}
import akka.util.{ByteString, ByteStringBuilder}
import one.eliot.machinomics.net.protocol._
import scodec.Attempt.{Failure, Successful}
import scodec._
import scodec.bits._

class Peer(remote: InetSocketAddress, network: Network) extends Actor with ActorLogging {

  import Tcp._
  import context.system

  var _blockHeaderCount = 0

  override def receive = { case Peer.ConnectCommand =>
    IO(Tcp) ! Connect(remote)
    val initialState = PeerState.Initial(network, NetworkAddress(remote, network), sender)
    context.become(onConnected(initialState) orElse onFailedConnection(initialState) orElse onSomethingUnexpected)
  }

  def onConnected(prevState: PeerState.Initial): Receive = { case c @ Connected(r, l) =>
    log.info(s"DidConnect to $r")
    sender ! Register(self)
    val currentState = prevState.connected
//    notify(currentState, Peer.DidConnect(currentState))
    sendMessage(prevState.network, protocol.VersionPayload(prevState.network, prevState.address))
    next(onVersionPayloadReceived(currentState))
  }

  def onVersionPayloadReceived(prevState: PeerState.Connected): Receive = onMessageReceived[protocol.VersionPayload] { payload =>
    log.info(s"Done version handshake with ${prevState.address}")
    val currentState = prevState.registered(
      selfReportedAddress = payload.myAddress,
      services = payload.services,
      version = payload.version,
      userAgent = payload.userAgent,
      height = payload.height
    )
//    notify(currentState, Peer.DidRegister(currentState))
    sendMessage(prevState.network, protocol.VerackPayload())
    next(onVerackPayloadReceived(currentState))
  }

  def onVerackPayloadReceived(prevState: PeerState.Registered): Receive = onMessageReceived[protocol.VerackPayload] { payload =>
    log.info(s"Acknowledged connection to ${prevState.address}")
    val currentState = prevState.acknowledged
    notify(currentState, Peer.DidAcknowledge(currentState))
    next(receiveAcknowledged(currentState))
  }

  def receiveAcknowledged(state: PeerState.Acknowledged): Receive = {
    case Peer.HeadersQuery() =>
      sendMessage(state.network, protocol.GetHeadersPayload(state.network.genesisHash))
      next(onHeadersReceive(state))
  }

  def onHeadersReceive(state: PeerState.Acknowledged, blockHeaderCount: Long = 0): Receive = onMessageReceived[protocol.HeadersPayload] { payload =>
    log.info(s"downloaded: $blockHeaderCount headers")
    if (blockHeaderCount < state.height) {
      val headersSent = payload.headers.takeRight(10)
      log.info(headersSent.map(x => x.hash).mkString("; "))
      log.info(s"last: ${payload.headers.last.hash.toString}, ${payload.headers.last} ${ByteString(payload.headers.last.hash.toString)}")
      sendMessage(state.network, protocol.GetHeadersPayload(headersSent.map(_.hash)))
      next(onHeadersReceive(state, blockHeaderCount + payload.count))
    } else {
      log.info(s"last: ${payload.headers.last.hash}, ${payload.headers.last}")
    }
  }

  def onFailedConnection(state: PeerState.Initial): Receive = { case CommandFailed(_: Connect) =>
    log.error(s"Can not connect to ${state.address}")
  }

  def onSomethingUnexpected: Receive = { case e =>
    log.error(e.toString)
  }

  def next(behavior: Receive) = context.become(behavior orElse onSomethingUnexpected)

  def notify(state: PeerState.PeerState, message: Peer.Message) = state.herd ! message

  def onMessageReceived[A <: Payload : Codec](f: A => Unit, buffer: ByteStringBuilder = ByteString.newBuilder): Receive = { case Received(blob) =>
    buffer.append(blob)
    protocol.Message.decode[A](buffer.result()) match {
      case Successful(DecodeResult(message, _)) =>
        buffer.clear()
        log.info(s"Received ${message.payload.command} message")
        f(message.payload)
      case Failure(e) =>
        context.become(onMessageReceived(f, buffer) orElse onSomethingUnexpected)
    }
  }

  def sendMessage[A <: protocol.Payload : Codec](network: Network, payload: A) = {
    val message = protocol.Message(network, payload)
    log.info(s"Sending $payload on $network")
    for {
      bits <- Codec.encode(message)
    } yield sender ! Write(bits)
  }

  implicit def bitVectorToByteString(bits: BitVector): ByteString = ByteString(bits.toByteArray)
}

object Peer {
//  def props = Props(classOf[Peer])
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

  case class Failed(state: PeerState.PeerState) extends Notification
}

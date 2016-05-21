package one.eliot.machinomics.net

import java.net.InetSocketAddress

import akka.actor._
import akka.io.{IO, Tcp}
import akka.util.{ByteString, ByteStringBuilder}
import one.eliot.machinomics.blockchain.{BlockHeader, DoubleHash}
import one.eliot.machinomics.net.protocol.{HeadersPayload, VerackPayload, VersionPayload}
import scodec.Attempt.{Failure, Successful}
import scodec.{Codec, DecodeResult}

import scala.concurrent.duration._

class PeerConnection(remote: InetSocketAddress, network: Network, replyTimeout: FiniteDuration) extends FSM[PeerConnection.Step, PeerConnection.State] with ActorLogging {
  import PeerConnection._
  import context.system

  val MAX_BUFFER = 16 * 1024 * 1024

  startWith(Initial, InitialState)

  when(Initial) {
    case Event(StartCommand, InitialState) =>
      IO(Tcp) ! Tcp.Connect(remote)
      log.info(s"Connecting to $remote")
      goto(Connecting) using HerdState(sender)
  }

  when(Connecting, replyTimeout) {
    case Event(Tcp.Connected(remoteAddress, localAddress), HerdState(herd)) =>
      log.info(s"Did connect to $remoteAddress")
      sender ! Tcp.Register(self)
      herd ! DidConnect
      goto(Connected) using WiredState(sender)
  }

  when(Connected) {
    case Event(HandshakeCommand, WiredState(wire))  =>
      tcpSend(wire, protocol.VersionPayload(network, NetworkAddress(remote, network)))
      expect[VersionPayload] using WiredHerdState(wire, sender)
    case Event(GetHeadersCommand(startHash), WiredState(wire)) =>
      tcpSend(wire, protocol.GetHeadersPayload(startHash))
      expect[HeadersPayload] using WiredHerdState(wire, sender)
  }

  whenExpect[VersionPayload, WiredHerdState] { case (payload, state) =>
    tcpSend(state.wire, protocol.VerackPayload())
    expect[VerackPayload]
  }

  whenExpect[VerackPayload, WiredHerdState] { case (payload, state) =>
    state.herd ! DidHandshake
    goto(Connected) using WiredState(state.wire)
  }

  whenExpect[HeadersPayload, WiredHerdState] { case (payload, state) =>
    state.herd ! GotHeaders(payload.headers)
    goto(Connected) using WiredState(state.wire)
  }

  whenUnhandled {
    case Event(Tcp.CommandFailed(cmd), _) =>
      throwDisconnected()
    case Event(e: Tcp.ConnectionClosed, _) =>
      throwDisconnected()
    case Event(StopCommand, WiredHerdState(wire, herd)) =>
      wire ! Tcp.Close
      herd ! DidStop
      goto(Initial)
    /*case Event(StateTimeout, s) =>
      throwDisconnected */
    case Event(Tcp.Received(blob), s) =>
      log.warning("Received unhandled blob {} in state {}/{}", blob.map(_.toChar).mkString, stateName, s)
      stay
    case Event(e, s) =>
      log.warning("Received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  initialize()

  def tcpSend[A <: protocol.Payload : Codec](wire: ActorRef, payload: A) = {
    val message = protocol.Message(network, payload)
    log.info(s"Sending ${payload.command}")
    for {
      bits <- Codec.encode(message)
    } yield wire ! Tcp.Write(ByteString(bits.toByteArray))
  }

  def expect[A <: protocol.Payload : Codec : Manifest]: State = goto(expectedPayloadState[A])

  def whenExpect[A <: protocol.Payload : Codec : Manifest, S: Manifest](f: (A, S) => State): Unit = {
    when(expectedPayloadState[A], replyTimeout) {
      case Event(Tcp.Received(blob), BufferingState(buffer, s: S)) => decode(s, blob, f, buffer)
      case Event(Tcp.Received(blob), s: S) => decode(s, blob, f, ByteString.newBuilder)
    }
  }

  def decode[A <: protocol.Payload : Codec : Manifest, S](s: S, blob: ByteString, f: (A, S) => State, buffer: ByteStringBuilder): State = {
    val currentBuffer = buffer.append(blob)
    println(currentBuffer.length)
    if (currentBuffer.length > MAX_BUFFER) {
      throwOverloaded(s"Got ${currentBuffer.length} bytes while parsing ${manifest[A].runtimeClass.getSimpleName}")
    }
    protocol.Message.decode[A](currentBuffer.result()) match {
      case Successful(DecodeResult(message, _)) =>
        log.info(s"Received ${message.payload.command} message")
        log.debug(s"Received ${message.payload}")
        f(message.payload, s)
      case Failure(e) =>
        stay using BufferingState(currentBuffer, s)
    }
  }

  def expectedPayloadState[A <: protocol.Payload : Manifest] = PayloadExpected(manifest[A].runtimeClass.getName)

  def throwDisconnected(msg: String = "")= throw DisconnectedException(msg)

  def throwOverloaded(msg: String = "") = throw DataOverloadException(msg)
}

object PeerConnection {
  case class DisconnectedException(msg: String = "") extends Exception
  case class DataOverloadException(msg: String = "") extends Exception

  sealed trait Step
  case object Initial extends Step
  case object Connecting extends Step
  case object Connected extends Step
  case class PayloadExpected(payloadClassName: String) extends Step

  sealed trait Message
  sealed trait IncomingMessage
  case object StartCommand extends IncomingMessage
  case object StopCommand extends IncomingMessage
  case object HandshakeCommand extends IncomingMessage
  case class  GetHeadersCommand(hash: DoubleHash) extends IncomingMessage

  sealed trait OutgoingMessage
  case object DidConnect extends OutgoingMessage
  case object DidHandshake extends OutgoingMessage
  case object DidStop extends OutgoingMessage
  case class GotHeaders(headers: Seq[BlockHeader]) extends OutgoingMessage

  sealed trait State
  case object InitialState extends State
  case class HerdState(herd: ActorRef) extends State
  case class WiredState(wire: ActorRef) extends State
  case class WiredHerdState(wire: ActorRef, herd: ActorRef) extends State
  case class BufferingState[S](buffer: ByteStringBuilder, state: S) extends State

  def props(remote: InetSocketAddress, network: Network, replyTimeout: FiniteDuration = 30.seconds) = Props(classOf[PeerConnection], remote, network, replyTimeout)
}

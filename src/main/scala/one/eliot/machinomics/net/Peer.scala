package one.eliot.machinomics.net

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import one.eliot.machinomics.net.protocol._
import scodec.Attempt.{Failure, Successful}
import scodec._
import scodec.bits.BitVector

class Peer(remote: InetSocketAddress, node: ActorRef, network: Network) extends Actor with ActorLogging {

  import Tcp._
  import context.system

  var services:   Services = Services()
  var version:    ProtocolVersion.Value = network.protocolVersion
  var selfReportedAddress:    NetworkAddress = NetworkAddress(remote, network)
  var userAgent:  String = ""
  var height:     Int = 0
  var acked:      Boolean = false

  IO(Tcp) ! Connect(remote)

  override def receive = onConnected orElse onFailedConnection orElse onSomethingUnexpected

  def onConnected: Actor.Receive = {
    case c @ Connected(r, l) => {
      log.info(s"Connected to $r")
      sender ! Register(self)
      sendMessage(protocol.VersionPayload(network, remote.getAddress))
      context.become(onVersionPayloadReceived orElse onSomethingUnexpected)
    }
  }

  def onVersionPayloadReceived: Actor.Receive = onMessageReceived[protocol.VersionPayload] { payload =>
    services = payload.services
    version  = payload.version
    selfReportedAddress  = payload.myAddress
    userAgent = payload.userAgent
    height = payload.height
    sendMessage(protocol.VerackPayload())
    context.become(onVerackPayloadReceived orElse onSomethingUnexpected)
  }

  def onVerackPayloadReceived: Actor.Receive = onMessageReceived[protocol.VerackPayload] { payload =>
    acked = true
    println(height)
  }

  def onFailedConnection: Actor.Receive = { case CommandFailed(_: Connect) =>
    log.error(s"Can not connect to $remote")
  }

  def onSomethingUnexpected: Actor.Receive = { case e =>
    println(e)
  }

  def onMessageReceived[A <: Payload : Codec](f: A => Unit): Actor.Receive = { case Received(blob) =>
    protocol.Message.decode[A](blob) match {
      case Successful(DecodeResult(message, _)) => {
        log.info(s"Received ${message.payload.command} message")
        f(message.payload)
      }
      case Failure(e) => onSomethingUnexpected(e)
    }
  }

  def sendMessage[A <: protocol.Payload : Codec](payload: A) = {
    val message = protocol.Message(network, payload)
    log.info(s"Sending $payload on $network")
    for {
      bits <- Codec.encode(message)
    } yield sender ! Write(bits)
  }

  implicit def bitVectorToByteString(bits: BitVector): ByteString = ByteString(bits.toByteArray)
}

object Peer {
  def props(remote: InetSocketAddress, node: ActorRef, network: Network) = Props(classOf[Peer], remote, node, network)
}

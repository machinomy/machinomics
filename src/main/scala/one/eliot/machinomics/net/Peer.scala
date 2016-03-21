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

  override def receive = {
    case CommandFailed(_: Connect) => {
      log.error(s"Can not connect to $remote")
    }
    case c @ Connected(r, l) => {
      log.info(s"Connected to $r")
      sender ! Register(self)
      sendMessage(protocol.VersionPayload(network, remote.getAddress))
      context.become {
        case Received(blob) => Codec.decode[protocol.Message[protocol.VersionPayload]](BitVector(blob)) match {
          case Successful(DecodeResult(message, _)) => {
            services = message.payload.services
            version  = message.payload.version
            selfReportedAddress  = message.payload.myAddress
            userAgent = message.payload.userAgent
            height = message.payload.height
            sendMessage(protocol.VerackPayload())
            context.become {
              case Received(b) => {
                println("After verack:")
                Codec.decode[protocol.Message[protocol.VerackPayload]](BitVector(b)) match {
                  case Successful(DecodeResult(Message(v, VerackPayload()), _)) => {
                    acked = true
                    println(height)
                  }
                  case Failure(e) => println(e)
                }
              }
              case e => println(e)
            }
          }
          case Failure(e) => println(e)
        }
        case e => println(e)
      }
    }
    case e => println(e)
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

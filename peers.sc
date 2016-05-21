import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.pattern._
import akka.util.Timeout
import one.eliot.machinomics.net.{PeerConnection, Testnet3Network}
import akka.actor.ActorDSL._

import scala.concurrent.duration._

val network = Testnet3Network
val ip = "testnet-seed.bitcoin.schildbach.de"
val address = new InetSocketAddress(ip, 18333)

implicit val actorSystem = ActorSystem()
implicit val ec = actorSystem.dispatcher
implicit val timeout = Timeout(30.seconds)
val peerConnection = actorSystem.actorOf(PeerConnection.props(address, network))

val y = actor(ctor = new Act {
  become {
    case "start" => {
      println(self)
      for {
        _ <- peerConnection ? PeerConnection.StartCommand
        _ <- peerConnection ? PeerConnection.HandshakeCommand
        y <- peerConnection ? PeerConnection.GetHeadersCommand(network.genesisHash)
      } println(y)
    }
  }
})

y ! "start"

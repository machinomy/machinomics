//package one.eliot.machinomics.examples
//
//import one.eliot.machinomics.net.Testnet3Network
//import scala.concurrent.ExecutionContext.Implicits.global
//
//object PrintDnsPeers extends App {
//  val peers = Testnet3Network.peers()
//  peers.onSuccess {
//    case addresses => println(addresses)
//  }
//  peers.onFailure {
//    case e => println(e)
//  }
//}

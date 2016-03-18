package one.eliot.machinomics.net.discovery

sealed trait Seed {
  def discovery: Discovery[_]
}

case class DnsSeed(hostname: String) extends Seed {
  def discovery = new DnsDiscovery(this)
}

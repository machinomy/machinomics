package one.eliot.machinomics.net.protocol

import scodec._
import scodec.codecs._

import scala.annotation.tailrec
import scala.collection.immutable.BitSet

case class Services(nodeNetwork: Boolean = false)

object Services {
  case class Feature[A](check: A => Boolean, set: A => A)

  val FEATURE_MAPPING: Map[Int, Feature[Services]] = Map(
    1 -> Feature(_.nodeNetwork, _.copy(nodeNetwork = true))
  )

  def toBitSet(s: Services): BitSet = {
    val fields = for { (index, feature) <- FEATURE_MAPPING if feature.check(s) } yield index
    BitSet.empty ++ fields
  }

  @tailrec
  def fromBitSet(b: BitSet, s: Services = Services()): Services = b.headOption match {
    case Some(i) => fromBitSet(b - i, FEATURE_MAPPING(i).set(s))
    case None => s
  }

  def fromLong(l: Long): Services = fromBitSet(BitSet.fromBitMask(Array(l)))
  def toLong(s: Services): Long = toBitSet(s).toBitMask.head

  implicit val codec: Codec[Services] = int64L.xmap(fromLong(_), toLong(_))

}

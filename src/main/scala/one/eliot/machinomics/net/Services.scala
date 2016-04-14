package one.eliot.machinomics.net

import scodec.Codec
import scodec.codecs.int64L

import scala.annotation.tailrec
import scala.collection.immutable.BitSet

case class Services(nodeNetwork: Boolean = false)

object Services {
  case class Feature(check: Services => Boolean, set: Services => Services)

  val FEATURE_MAPPING: Map[Int, Feature] = Map(
    1 -> Feature(_.nodeNetwork, _.copy(nodeNetwork = true))
  ).withDefaultValue(Feature(s => true, s => s))

  def toBitSet(s: Services): BitSet = {
    val fields = for { (index, feature) <- FEATURE_MAPPING if feature.check(s) } yield index
    BitSet.empty ++ fields
  }

  @tailrec
  def fromBitSet(b: BitSet, s: Services = Services()): Services = b.headOption match {
    case Some(i) if i > 0 => fromBitSet(b - i, FEATURE_MAPPING(i).set(s))
    case _ => s
  }

  def fromLong(l: Long): Services = fromBitSet(BitSet.fromBitMask(Array(l)))
  def toLong(s: Services): Long = toBitSet(s).toBitMask.head

  implicit val codec: Codec[Services] = int64L.xmap(fromLong(_), toLong(_))

}

package one.eliot

import java.security.MessageDigest

package object machinomics {
  type Hash = Array[Byte]

  object Hash {

    def doubleSHA256(data: Array[Byte]): Hash = {
      val messageDigest = MessageDigest.getInstance("SHA-256")
      messageDigest.update(data)
      messageDigest.digest()
    }

  }
}

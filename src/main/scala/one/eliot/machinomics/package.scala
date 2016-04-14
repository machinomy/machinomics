package one.eliot

import java.security.MessageDigest

package object machinomics {
  type Hash = Array[Byte]

  object Hash {

    def SHA256(data: Array[Byte]): Hash = {
      val messageDigest = MessageDigest.getInstance("SHA-256")
      messageDigest.update(data)
      messageDigest.digest()
    }

    def doubleSHA256(data: Array[Byte]): Hash = {
      SHA256(SHA256(data))
    }

  }
}

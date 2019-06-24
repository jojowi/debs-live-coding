package util

object hash2String {
  def apply(hash: Array[Byte]): String = {
    val bigI = BigInt(hash)
    var string = bigI + ""
    var negative = false
    if (string.startsWith("-")) {
      negative = true
      string = string.substring(1)
    }
    while(string.length < 19) string = "0" + string
    if (negative) "-" + string.substring(0, 4) else " " + string.substring(0, 4)
  }
}
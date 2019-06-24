package chord

import loci._
import loci.communicator.tcp._



object Main extends App {
  val printNotifier = Notifier[Unit]
  val nullNotifier = Notifier[Unit]
  val getNotifier = Notifier[Int]
  val nullGetNotifier = Notifier[Int]
  val putNotifier = Notifier[(Int, String)]
  val nullPutNotifier = Notifier[(Int, String)]

  def createInstance(p: Int) = {
    multitier start new Instance[ChordHashMap.Node](
      contexts.Pooled.global,
      listen[ChordHashMap.Node] { TCP(p) } and
      connect[ChordHashMap.Node] { TCP("localhost", 1090) }) {
      val port = p
      val init = true
      val print = nullNotifier.notification
      val get = nullGetNotifier.notification
      val put = nullPutNotifier.notification
    }
    Thread.sleep(500)
  }

  multitier start new Instance[ChordHashMap.Node](
    contexts.Pooled.global,
    listen[ChordHashMap.Node] { TCP(1090) }){
    val port = 1090
    val init = false
    val print = printNotifier.notification
    val get = getNotifier.notification
    val put = putNotifier.notification
  }

  Thread.sleep(500)

  createInstance(1091)
  
  createInstance(1092)

  createInstance(1093)

  printNotifier()

  Thread.sleep(500)

  getNotifier(0)
  putNotifier(0, "Hallo")
  getNotifier(0)
  println()
  for( i <- 1 to 5){
    val key = new java.security.SecureRandom().nextInt()
    getNotifier(key)
    putNotifier(key, "Test" + i)
    getNotifier(key)
    println()
  }

  printNotifier()

  Thread.sleep(500)

  createInstance(1094)

  createInstance(1095)

  printNotifier()
}

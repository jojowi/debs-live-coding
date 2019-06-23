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
  val first = multitier start new Instance[ChordHashMap.Node](
    contexts.Pooled.global,
    listen[ChordHashMap.Node] { TCP(1090) }){
    val port = 1090
    val init = false
    val print = printNotifier.notification
    val get = getNotifier.notification
    val put = putNotifier.notification
  }
  Thread.sleep(1000)
  multitier start new Instance[ChordHashMap.Node](
    contexts.Pooled.global,
    listen[ChordHashMap.Node] { TCP(1091) } and
    connect[ChordHashMap.Node] { TCP("localhost", 1090) }){
    val port = 1091
    val init = true
    val print = nullNotifier.notification
    val get = nullGetNotifier.notification
    val put = nullPutNotifier.notification
  }
  Thread.sleep(1000)
  multitier start new Instance[ChordHashMap.Node](
    contexts.Pooled.global,
    listen[ChordHashMap.Node] { TCP(1092) } and
      connect[ChordHashMap.Node] { TCP("localhost", 1090) }){
    val port = 1092
    val init = true
    val print = nullNotifier.notification
    val get = nullGetNotifier.notification
    val put = nullPutNotifier.notification
  }




  Thread.sleep(1000)
  multitier start new Instance[ChordHashMap.Node](
    contexts.Pooled.global,
    listen[ChordHashMap.Node] { TCP(1093) } and
      connect[ChordHashMap.Node] { TCP("localhost", 1090) }){
    val port = 1093
    val init = true
    val print = nullNotifier.notification
    val get = nullGetNotifier.notification
    val put = nullPutNotifier.notification
  }

  Thread.sleep(1000)
  multitier start new Instance[ChordHashMap.Node](
    contexts.Pooled.global,
    listen[ChordHashMap.Node] { TCP(1094) } and
      connect[ChordHashMap.Node] { TCP("localhost", 1090) }){
    val port = 1094
    val init = true
    val print = nullNotifier.notification
    val get = nullGetNotifier.notification
    val put = nullPutNotifier.notification
  }

  Thread.sleep(1000)
  multitier start new Instance[ChordHashMap.Node](
    contexts.Pooled.global,
    listen[ChordHashMap.Node] { TCP(1095) } and
      connect[ChordHashMap.Node] { TCP("localhost", 1090) }){
    val port = 1095
    val init = true
    val print = nullNotifier.notification
    val get = nullGetNotifier.notification
    val put = nullPutNotifier.notification
  }


  Thread.sleep(1000)
  printNotifier()

  Thread.sleep(1000)
  getNotifier(0)
  putNotifier(0, "Hallo")
  getNotifier(0)
  for( i <- 1 to 10){
    val key = new java.security.SecureRandom().nextInt()
    getNotifier(key)
    putNotifier(key, "Test" + i)
    getNotifier(key)
  }

  printNotifier()
  Thread.sleep(1000)
  multitier start new Instance[ChordHashMap.Node](
    contexts.Pooled.global,
    listen[ChordHashMap.Node] { TCP(1096) } and
      connect[ChordHashMap.Node] { TCP("localhost", 1090) }){
    val port = 1096
    val init = true
    val print = nullNotifier.notification
    val get = nullGetNotifier.notification
    val put = nullPutNotifier.notification
  }

  Thread.sleep(1000)
  multitier start new Instance[ChordHashMap.Node](
    contexts.Pooled.global,
    listen[ChordHashMap.Node] { TCP(1097) } and
      connect[ChordHashMap.Node] { TCP("localhost", 1090) }){
    val port = 1097
    val init = true
    val print = nullNotifier.notification
    val get = nullGetNotifier.notification
    val put = nullPutNotifier.notification
  }
  Thread.sleep(1000)
  printNotifier()
}

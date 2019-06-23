package chord

import loci.communicator.tcp.TCP

import scala.concurrent._
import scala.concurrent.duration._
import loci.serializer.upickle._

import scala.collection.mutable._
import loci._

import scala.collection.mutable
import scala.math.BigInt

@multitier object ChordHashMap {
  val md = java.security.MessageDigest.getInstance("SHA-1")
  val hashbytes = 8
  val timeout = new FiniteDuration(1, SECONDS)
  val rand = new java.security.SecureRandom()
  val uid = hash(rand.nextInt)
  @peer type Peer
  @peer type Node <: Peer { type Tie <: Multiple[Node] }
  final case class Neighbor(uid: Array[Byte], remote: Remote[Node])
  val port: Local[Int] on Node
  val init: Local[Boolean] on Node
  val print: Local[Notification[Unit]] on Node
  val get: Local[Notification[Int]] on Node
  val put: Local[Notification[(Int, String)]] on Node
  var pred: Local[Neighbor] on Node = null
  var succ: Local[Neighbor] on Node = null
  var hashMap: mutable.HashMap[BigInt, String] = mutable.HashMap()
  var pID = true
  var initialized = false

  def printID(): on[Unit, Node] = on[Node] {implicit! =>
    if (pID) {
      print(hashMap.toString)
      remote(succ.remote) call printID()
    } else {
      pID = true
      println()
      println()
    }
  }

  def get(key: Int): Option[String] on Node = on[Node] {implicit! =>
    val k = hash(key)
    if (between(pred.uid, k, uid)) hashMap.get(BigInt(k))
    else Await.result((remote(succ.remote) call get(key)).asLocal, timeout)
  }

  def put(key: Int, value: String): Unit on Node = on[Node] {implicit! =>
    val k = hash(key)
    if (between(pred.uid, k, uid)) hashMap.put(BigInt(k), value)
    else Await.ready((remote(succ.remote) call put(key, value)).asLocal, timeout)
  }

  def main(): Unit on Node =
    on[Node] {implicit! =>
      var notif: Notifiable[loci.Remote[ChordHashMap.this.Node]] = null
      if (init) notif = remote[Node].joined.foreach(node => {
        notif.remove()
        initialize(node)
      })
      if (init && remote[Node].connected.size > 0) {
        initialize(remote[Node].connected.head)
      }
      print.foreach(unit => {
        println()
        println()
        println("Printing IDs...")
        printID()
        pID = false
      })
      get.foreach(key => {
        val ret = get(key)
        if (ret.isEmpty) {
          println("Querying   " + hash2String(hash(key)) + ", found nothing")
        } else {
          println("Querying   " + hash2String(hash(key)) + ", found " + ret.get)
        }
      })
      put.foreach(req => {
        println("Adding key " + hash2String(hash(req._1)) + ", value " + req._2)
        put(req._1, req._2)
      })
    }



  // INITIALIZE

  def initialize(node: Remote[Node]) = on[Node].local { implicit! =>
    if (!initialized) {
      initialized = true
      if (!Await.result((remote(node) call hasNeighbors()).asLocal, timeout)) {
        val otherUID = Await.result((remote(node) call getUID()).asLocal, timeout)
        pred = Neighbor(otherUID, node)
        succ = Neighbor(otherUID, node)
        notifyOthers()
      } else {
        initPred(node)
      }
    }
  }

  def hasNeighbors() = on[Node] { implicit! =>
    pred != null
  }

  def getUID() = on[Node] { implicit! =>
    uid
  }

  def initPred(node: Remote[Node]) = on[Node].local { implicit! =>
    val pred = Await.result((remote(node) call findPred(uid)).asLocal, timeout)
    var notif: Notifiable[loci.Remote[ChordHashMap.this.Node]] = null

    notif = remote[Node].joined.foreach(predRemote => {
      predRemote.protocol match {
        case TCP(host, port) =>
          if (port == pred._1) {
            notif.remove()
            initSucc(predRemote)
            this.pred = Neighbor(pred._2, predRemote)
          }
      }
    })
    remote[Node] connect { TCP("localhost", pred._1) }
  }

  def findPred(otherUID: Array[Byte]): on[(Int, Array[Byte]), Node] = on[Node] { implicit! =>
    if (between(uid, otherUID, succ.uid)) {
      (port, uid)
    } else {
      Await.result((remote(succ.remote) call findPred(otherUID)).asLocal, Duration.Inf)
    }
  }

  def initSucc(pred: Remote[Node]) = on[Node].local { implicit! =>
    val succ = Await.result((remote(pred) call getSucc()).asLocal, new FiniteDuration(5, SECONDS))
    var notif: Notifiable[loci.Remote[ChordHashMap.this.Node]] = null
    notif = remote[Node].joined.foreach(node => {
      node.protocol match {
        case TCP(host, port) =>
          if (port == succ._1) {
            notif.remove()
            this.succ = Neighbor(succ._2, node)
            notifyOthers()
          }
      }
    })
    remote[Node] connect { TCP("localhost", succ._1) }
  }

  def getSucc(): on[(Int, Array[Byte]), Node] = on[Node] { implicit! =>
    val port = Await.result((remote(succ.remote) call getPort).asLocal, Duration.Inf)
    (port, succ.uid)
  }

  def getPort(): on[Int, Node] = on[Node] { implicit! =>
    port
  }

  def notifyOthers() = on[Node].local { implicit! =>
    val future1 = (remote(succ.remote) call updatePred(uid)).asLocal
    val future2 = (remote(pred.remote) call updateSucc(uid)).asLocal
    Await.ready(future1, new FiniteDuration(1, SECONDS))
    Await.ready(future2, new FiniteDuration(1, SECONDS))
    remote(succ.remote) call migrateValues()
  }

  def updateSucc(key: Array[Byte]) = on[Node].sbj { implicit! => node: Remote[Node] =>
    succ = Neighbor(key, node)
  }

  def updatePred(key: Array[Byte]) = on[Node].sbj { implicit! => node: Remote[Node] =>
    pred = Neighbor(key, node)
  }

  def migrateValues() = on[Node] { implicit! =>
    for ((key, value) <- hashMap) {
      if (!between(pred.uid, key.toByteArray, uid)) {
        remote(pred.remote) call put(key, value)
        hashMap.remove(key)
      }
    }
  }

  def put(key: BigInt, value: String): Unit on Node = on[Node] {implicit! =>
    hashMap.put(key, value)
  }

  // HELPER

  def between(pred: Array[Byte], key: Array[Byte], uid: Array[Byte]): Boolean = {
    (compare(pred, uid) < 0 && compare(pred, key) < 0 && compare(uid, key) > 0) ||
      (compare(pred, uid) > 0 && compare(pred, key) > 0 && compare(uid, key) > 0) ||
      (compare(pred, uid) > 0 && compare(pred, key) < 0 && compare(uid, key) < 0)
  }

  def hash(num: Int): Array[Byte] = {
    md.digest(BigInt(num).toByteArray).slice(0, hashbytes)
  }
  def compare(key1: Array[Byte], key2: Array[Byte]): Int = {
    BigInt(key1).compare(BigInt(key2))
  }
  def print(msg: String) = {
    println(hash2String(uid) + ": " + msg)
  }
  def hash2String(hash: Array[Byte]): String = {
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
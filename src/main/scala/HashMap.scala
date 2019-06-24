package chord

import util._

import loci._
import loci.communicator.tcp.TCP
import loci.serializer.upickle._

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.{FiniteDuration, SECONDS}
import scala.math.BigInt


@multitier object ChordHashMap {
  val HASHBYTES = 8
  val TIMEOUT = new FiniteDuration(1, SECONDS)

  @peer type Peer
  @peer type Node <: Peer { type Tie <: Multiple[Node] }

  val port: Local[Int] on Node
  val init: Local[Boolean] on Node
  val print: Local[Notification[Unit]] on Node
  val get: Local[Notification[Int]] on Node
  val put: Local[Notification[(Int, String)]] on Node

  final case class Neighbor(uid: Array[Byte], remote: Remote[Node])
  var pred: Local[Neighbor] on Node = null
  var succ: Local[Neighbor] on Node = null
  var hashMap: Local[mutable.HashMap[BigInt, String]] on Node = mutable.HashMap[BigInt, String]()
  val uid: Local[Array[Byte]] on Node = createUID

  var pID: Local[Boolean] on Node = true
  var initialized: Local[Boolean] on Node = false

  def main(): Unit on Node =
    on[Node] {

      var notif: Notifiable[Remote[Node]] = null
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

  def printID(): on[Unit, Node] = on[Node] {
    if (pID) {
      print(hashMap.toString)
      remote(succ.remote) call printID()
    } else {
      pID = true
      println()
      println()
    }
  }

  def get(key: Int): Option[String] on Node = on[Node] {
    val k = hash(key)
    if (between(pred.uid, k, uid)) hashMap.get(BigInt(k))
    else Await.result((remote(succ.remote) call get(key)).asLocal, TIMEOUT)
  }

  def put(key: Int, value: String): Unit on Node = on[Node] {
    val k = hash(key)
    if (between(pred.uid, k, uid)) hashMap.put(BigInt(k), value)
    else Await.ready((remote(succ.remote) call put(key, value)).asLocal, TIMEOUT)
  }

  // INITIALIZE

  def initialize(node: Remote[Node]) = on[Node].local {
    if (!initialized) {
      initialized = true
      if (!Await.result((remote(node) call hasNeighbors()).asLocal, TIMEOUT)) {
        val otherUID = Await.result((remote(node) call getUID()).asLocal, TIMEOUT)
        pred = Neighbor(otherUID, node)
        succ = Neighbor(otherUID, node)
        notifyOthers()
      } else {
        initPred(node)
      }
    }
  }

  def hasNeighbors() = on[Node] {
    pred != null
  }

  def getUID() = on[Node] {
    uid
  }

  def initPred(node: Remote[Node]) = on[Node].local {
    val pred = Await.result((remote(node) call findPred(uid)).asLocal, TIMEOUT)
    var notif: Notifiable[Remote[Node]] = null

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

  def findPred(otherUID: Array[Byte]): on[(Int, Array[Byte]), Node] = on[Node] {
    if (between(uid, otherUID, succ.uid)) {
      (port, uid)
    } else {
      Await.result((remote(succ.remote) call findPred(otherUID)).asLocal, TIMEOUT)
    }
  }

  def initSucc(pred: Remote[Node]) = on[Node].local {
    val succ = Await.result((remote(pred) call getSucc()).asLocal, TIMEOUT)
    var notif: Notifiable[Remote[Node]] = null
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

  def getSucc(): on[(Int, Array[Byte]), Node] = on[Node] {
    val port = Await.result((remote(succ.remote) call getPort).asLocal, TIMEOUT)
    (port, succ.uid)
  }

  def getPort(): on[Int, Node] = on[Node] {
    port
  }

  def notifyOthers() = on[Node].local {
    val future1 = (remote(succ.remote) call updatePred(uid)).asLocal
    val future2 = (remote(pred.remote) call updateSucc(uid)).asLocal
    Await.ready(future1, TIMEOUT)
    Await.ready(future2, TIMEOUT)
    val list = Await.result((remote(succ.remote) call migrateValues()).asLocal, TIMEOUT)
    for ((key, value) <- list) {
      hashMap.put(key, value)
    }
  }

  def updateSucc(key: Array[Byte]) = on[Node].sbj { node: Remote[Node] =>
    succ = Neighbor(key, node)
  }

  def updatePred(key: Array[Byte]) = on[Node].sbj { node: Remote[Node] =>
    pred = Neighbor(key, node)
  }

  def migrateValues() = on[Node] {
    var list = List[(BigInt, String)]()
    for ((key, value) <- hashMap) {
      if (!between(pred.uid, key.toByteArray, uid)) {
        list = (key, value) :: list
        hashMap.remove(key)
      }
    }
    list
  }

  // HELPER

  def between(pred: Array[Byte], key: Array[Byte], uid: Array[Byte]): Boolean = {
    (compare(pred, uid) < 0 && compare(pred, key) < 0 && compare(uid, key) > 0) ||
      (compare(pred, uid) > 0 && compare(uid, key) > 0) ||
      (compare(pred, uid) > 0 && compare(pred, key) < 0)
  }

  def createUID(): Array[Byte] = {
    val rand = new java.security.SecureRandom()
    hash(rand.nextInt)
  }

  def hash(num: Int): Array[Byte] = {
    val md = java.security.MessageDigest.getInstance("SHA-1")
    md.digest(BigInt(num).toByteArray).slice(0, HASHBYTES)
  }

  def compare(key1: Array[Byte], key2: Array[Byte]): Int = {
    BigInt(key1).compare(BigInt(key2))
  }

  def print(msg: String) = on[Node] {
    println(hash2String(uid) + ": " + msg)
  }
}
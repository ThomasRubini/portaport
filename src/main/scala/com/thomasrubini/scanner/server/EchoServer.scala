package com.thomasrubini.scanner.server

import com.thomasrubini.scanner.net.SocketIo
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import scala.jdk.CollectionConverters.*

final case class ServerStartReport(startedPorts: List[Int], failedPorts: Map[Int, String])

final class EchoServer(host: String, ports: List[Int]):
  private val ReadTimeoutMs = 1000
  private val running = AtomicBoolean(false)
  private val stopSignal = CountDownLatch(1)
  private val listeners = ConcurrentHashMap[Int, ServerSocket]()
  private val acceptorPool = Executors.newCachedThreadPool()
  private val connectionPool = Executors.newCachedThreadPool()

  def start(): ServerStartReport =
    if !running.compareAndSet(false, true) then
      throw IllegalStateException("Server already started")

    val failures = scala.collection.mutable.Map.empty[Int, String]
    val address = InetAddress.getByName(host)

    ports.foreach { port =>
      try
        val socket = new ServerSocket(port, 50, address)
        listeners.put(port, socket)
        acceptorPool.submit(
          new Runnable:
            override def run(): Unit = acceptLoop(port, socket)
        )
      catch
        case exception: Exception =>
          failures.put(port, exception.getMessage)
    }

    ServerStartReport(
      startedPorts = listeners.keySet().asScala.toList.sorted,
      failedPorts = failures.toMap
    )

  def awaitTermination(): Unit =
    stopSignal.await()

  def stop(): Unit =
    if running.compareAndSet(true, false) then
      listeners.values().asScala.foreach { socket =>
        try socket.close()
        catch case _: Exception => ()
      }
      listeners.clear()
      shutdownExecutor(acceptorPool)
      shutdownExecutor(connectionPool)
      stopSignal.countDown()

  private def acceptLoop(port: Int, serverSocket: ServerSocket): Unit =
    while running.get() do
      try
        val socket = serverSocket.accept()
        connectionPool.submit(
          new Runnable:
            override def run(): Unit = handleConnection(socket)
        )
      catch
        case _: java.net.SocketException => ()
        case _: Exception                => ()

  private def handleConnection(socket: Socket): Unit =
    try
      socket.setSoTimeout(ReadTimeoutMs)
      val payload = SocketIo.readAll(socket.getInputStream)
      val output = socket.getOutputStream
      output.write(payload)
      output.flush()
    catch case _: Exception => ()
    finally
      try socket.close()
      catch case _: Exception => ()

  private def shutdownExecutor(executor: ExecutorService): Unit =
    executor.shutdownNow()

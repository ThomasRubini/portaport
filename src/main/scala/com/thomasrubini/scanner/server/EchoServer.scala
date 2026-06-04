package com.thomasrubini.scanner.server

import com.thomasrubini.scanner.cli.IpVersion
import com.thomasrubini.scanner.cli.Transport
import com.thomasrubini.scanner.net.IpAddressResolver
import com.thomasrubini.scanner.net.SocketIo
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import scala.jdk.CollectionConverters.*

final case class ServerStartReport(startedPorts: List[Int], failedPorts: Map[Int, String])

final class EchoServer(host: String, ports: List[Int], transport: Transport, ipVersion: IpVersion):
  private val ReadTimeoutMs = 1000
  private val running = AtomicBoolean(false)
  private val stopSignal = CountDownLatch(1)
  private val tcpListeners = ConcurrentHashMap[Int, ServerSocket]()
  private val udpListeners = ConcurrentHashMap[Int, DatagramSocket]()
  private val acceptorPool = Executors.newCachedThreadPool()
  private val connectionPool = Executors.newCachedThreadPool()

  /** Starts listeners for the selected transport across all requested ports. */
  def start(): ServerStartReport =
    if !running.compareAndSet(false, true) then
      throw IllegalStateException("Server already started")

    val failures = scala.collection.mutable.Map.empty[Int, String]
    val addressResult = IpAddressResolver.resolve(host, ipVersion)

    addressResult match
      case Left(error) =>
        running.set(false)
        return ServerStartReport(Nil, ports.map(_ -> error).toMap)
      case Right(address) =>
        ports.foreach { port =>
          try
            transport match
              case Transport.Tcp =>
                val socket = new ServerSocket(port, 50, address)
                tcpListeners.put(port, socket)
                acceptorPool.submit(
                  new Runnable:
                    override def run(): Unit = acceptLoop(socket)
                )
              case Transport.Udp =>
                val socket = new DatagramSocket(InetSocketAddress(address, port))
                udpListeners.put(port, socket)
                acceptorPool.submit(
                  new Runnable:
                    override def run(): Unit = udpLoop(socket)
                )
              case Transport.Ip =>
                failures.put(port, "server does not support --protocol ip")
          catch
            case exception: Exception =>
              failures.put(port, exception.getMessage)
        }

    ServerStartReport(
      startedPorts = startedPorts,
      failedPorts = failures.toMap
    )

  /** Blocks until a stop signal is received. */
  def awaitTermination(): Unit =
    stopSignal.await()

  /** Stops listeners and worker pools. */
  def stop(): Unit =
    if running.compareAndSet(true, false) then
      tcpListeners.values().asScala.foreach { socket =>
        try socket.close()
        catch case _: Exception => ()
      }
      tcpListeners.clear()
      udpListeners.values().asScala.foreach { socket =>
        try socket.close()
        catch case _: Exception => ()
      }
      udpListeners.clear()
      shutdownExecutor(acceptorPool)
      shutdownExecutor(connectionPool)
      stopSignal.countDown()

  /** Accepts TCP connections and dispatches each one for echo handling. */
  private def acceptLoop(serverSocket: ServerSocket): Unit =
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

  /** Handles one TCP connection by echoing the received payload. */
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

  /** Receives UDP datagrams and echoes each payload back to the sender. */
  private def udpLoop(socket: DatagramSocket): Unit =
    while running.get() do
      try
        val buffer = Array.ofDim[Byte](65507)
        val packet = DatagramPacket(buffer, buffer.length)
        socket.receive(packet)

        val outbound = DatagramPacket(packet.getData, packet.getLength, packet.getAddress, packet.getPort)
        socket.send(outbound)
      catch
        case _: java.net.SocketException => ()
        case _: Exception                => ()

  /** Returns all ports that have an active listener. */
  private def startedPorts: List[Int] =
    (tcpListeners.keySet().asScala ++ udpListeners.keySet().asScala).toList.sorted

  /** Forces executor shutdown during server stop. */
  private def shutdownExecutor(executor: ExecutorService): Unit =
    executor.shutdownNow()

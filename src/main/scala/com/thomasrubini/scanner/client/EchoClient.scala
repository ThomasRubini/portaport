package com.thomasrubini.scanner.client

import com.thomasrubini.scanner.cli.IpVersion
import com.thomasrubini.scanner.cli.Transport
import com.thomasrubini.scanner.net.IpAddressResolver
import java.net.InetSocketAddress
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket
import com.thomasrubini.scanner.net.SocketIo
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.sys.process.Process
import scala.sys.process.ProcessLogger
import scala.util.Using

case class ScanResult(open: List[Int], closed: List[Int])

object EchoClient:
  private given ExecutionContext = ExecutionContext.global
  private val ProbePayload = "scala-scanner-probe".getBytes("UTF-8")
  private val MaxConcurrency = 50

  /** Checks host reachability at IP level using the system ping command. */
  def checkIpReachable(
      host: String,
      timeoutMs: Int,
      ipVersion: IpVersion
  ): Either[String, Boolean] =
    val addressFlag = ipVersion match
      case IpVersion.V4 => "-4"
      case IpVersion.V6 => "-6"
    val isWindows = System.getProperty("os.name").toLowerCase.contains("windows")
    val args =
      if isWindows then
        Seq("ping", "-n", "1", "-w", timeoutMs.toString, host)
      else
        val timeoutSec = (timeoutMs / 1000).max(1)
        Seq("ping", addressFlag, "-c", "1", "-W", timeoutSec.toString, host)
    try
      val exitCode = Process(args).run(ProcessLogger(_ => ())).exitValue()
      Right(exitCode == 0)
    catch
      case e: Exception =>
        Left(s"Failed to check reachability for '$host': ${e.getMessage}")

  /** Scans ports and returns those that complete the requested protocol probe. */
  def scanOpenPorts(
      host: String,
      ports: List[Int],
      timeoutMs: Int,
      transport: Transport,
      ipVersion: IpVersion,
      onProgress: (Int, Int) => Unit = (_, _) => ()
  ): Either[String, ScanResult] =
    IpAddressResolver.resolve(host, ipVersion) match
      case Left(error) => Left(error)
      case Right(address) =>
        val total = ports.size
        val batched = ports.grouped(MaxConcurrency).toList
        val futures = batched.map { batch =>
          Future.traverse(batch) { port =>
            onProgress(port, total)
            Future(port -> isEchoOpen(address, port, timeoutMs, transport))
          }
        }
        val checked = futures.map(Await.result(_, Duration.Inf)).flatten
        val (open, closed) = checked.partition(_._2)
        Right(ScanResult(open.map(_._1).sorted, closed.map(_._1).sorted))

  /** Checks whether a single port answers the configured protocol echo probe. */
  private def isEchoOpen(
      address: InetAddress,
      port: Int,
      timeoutMs: Int,
      transport: Transport
  ): Boolean =
    transport match
      case Transport.Tcp => isTcpEchoOpen(address, port, timeoutMs)
      case Transport.Udp => isUdpEchoOpenWithRetry(address, port, timeoutMs)
      case _ => throw new AssertionError("Unexpected transport")

  /** Executes the TCP echo probe on a single target port. */
  private def isTcpEchoOpen(address: InetAddress, port: Int, timeoutMs: Int): Boolean =
    Using(new Socket()) { socket =>
      socket.connect(InetSocketAddress(address, port), timeoutMs)
      socket.setSoTimeout(timeoutMs)

      val output = socket.getOutputStream
      output.write(ProbePayload)
      output.flush()
      socket.shutdownOutput()

      val echoed = SocketIo.readAll(socket.getInputStream)
      echoed.sameElements(ProbePayload)
    }.getOrElse(false)

  /** Executes the UDP echo probe on a single target port. */
  private def isUdpEchoOpen(address: InetAddress, port: Int, timeoutMs: Int): Boolean =
    Using(new DatagramSocket()) { socket =>
      socket.setSoTimeout(timeoutMs)
      socket.connect(InetSocketAddress(address, port))

      val outbound = DatagramPacket(ProbePayload, ProbePayload.length)
      socket.send(outbound)

      val inboundBuffer = Array.ofDim[Byte](ProbePayload.length)
      val inbound = DatagramPacket(inboundBuffer, inboundBuffer.length)
      socket.receive(inbound)

      val received = inbound.getData.take(inbound.getLength)
      received.sameElements(ProbePayload)
    }.getOrElse(false)

  private def isUdpEchoOpenWithRetry(address: InetAddress, port: Int, timeoutMs: Int): Boolean =
    (1 to 3).exists(_ => isUdpEchoOpen(address, port, timeoutMs))

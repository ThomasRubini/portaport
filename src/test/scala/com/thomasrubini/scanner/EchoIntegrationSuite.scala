package com.thomasrubini.scanner

import com.thomasrubini.scanner.cli.IpVersion
import com.thomasrubini.scanner.cli.Transport
import com.thomasrubini.scanner.client.EchoClient
import com.thomasrubini.scanner.client.ScanResult
import com.thomasrubini.scanner.server.EchoServer
import munit.FunSuite

import java.net.DatagramSocket
import java.net.ServerSocket
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

final class EchoIntegrationSuite extends FunSuite:
  private given ExecutionContext = ExecutionContext.global

  test("client finds open echo port") {
    val openPort = findFreePort()
    val server = EchoServer("127.0.0.1", List(openPort), Transport.Tcp, IpVersion.V4)
    val report = server.start()

    try
      assertEquals(report.startedPorts, List(openPort))
      val Right(ScanResult(openPorts, _)) = EchoClient.scanOpenPorts(
        "127.0.0.1",
        List(openPort),
        timeoutMs = 300,
        Transport.Tcp,
        IpVersion.V4
      )
      assertEquals(openPorts, List(openPort))
    finally server.stop()
  }

  test("client excludes closed port") {
    val closedPort = findFreePort()
    val Right(ScanResult(openPorts, closedPorts)) = EchoClient.scanOpenPorts(
      "127.0.0.1",
      List(closedPort),
      timeoutMs = 200,
      Transport.Tcp,
      IpVersion.V4
    )
    assertEquals(openPorts, Nil)
    assertEquals(closedPorts, List(closedPort))
  }

  test("server handles concurrent probes") {
    val openPort = findFreePort()
    val server = EchoServer("127.0.0.1", List(openPort), Transport.Tcp, IpVersion.V4)
    server.start()

    try
      val scans = Future.traverse(1 to 20)(_ =>
        Future(
          EchoClient.scanOpenPorts(
            "127.0.0.1",
            List(openPort),
            timeoutMs = 300,
            Transport.Tcp,
            IpVersion.V4
          )
        )
      )
      val results = Await.result(scans, 10.seconds)
      assert(results.forall { case Right(ScanResult(openPorts, _)) => openPorts == List(openPort) })
    finally server.stop()
  }

  test("client finds open udp echo port") {
    val openPort = findFreeUdpPort()
    val server = EchoServer("127.0.0.1", List(openPort), Transport.Udp, IpVersion.V4)
    val report = server.start()

    try
      assertEquals(report.startedPorts, List(openPort))
      val Right(ScanResult(openPorts, _)) = EchoClient.scanOpenPorts(
        "127.0.0.1",
        List(openPort),
        timeoutMs = 500,
        Transport.Udp,
        IpVersion.V4
      )
      assertEquals(openPorts, List(openPort))
    finally server.stop()
  }

  test("client excludes closed udp port") {
    val closedPort = findFreePort()
    val Right(ScanResult(openPorts, closedPorts)) = EchoClient.scanOpenPorts(
      "127.0.0.1",
      List(closedPort),
      timeoutMs = 500,
      Transport.Udp,
      IpVersion.V4
    )
    assertEquals(openPorts, Nil)
    assertEquals(closedPorts, List(closedPort))
  }

  test("server handles concurrent udp probes") {
    val openPort = findFreeUdpPort()
    val server = EchoServer("127.0.0.1", List(openPort), Transport.Udp, IpVersion.V4)
    server.start()

    try
      val scans = Future.traverse(1 to 20)(_ =>
        Future(
          EchoClient.scanOpenPorts(
            "127.0.0.1",
            List(openPort),
            timeoutMs = 500,
            Transport.Udp,
            IpVersion.V4
          )
        )
      )
      val results = Await.result(scans, 10.seconds)
      assert(results.forall { case Right(ScanResult(openPorts, _)) => openPorts == List(openPort) })
    finally server.stop()
  }

  private def findFreePort(): Int =
    val socket = ServerSocket(0)
    try socket.getLocalPort
    finally socket.close()

  private def findFreeUdpPort(): Int =
    val socket = new DatagramSocket(0)
    try socket.getLocalPort
    finally socket.close()

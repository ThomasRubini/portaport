package com.thomasrubini.scanner

import com.thomasrubini.scanner.client.EchoClient
import com.thomasrubini.scanner.server.EchoServer
import munit.FunSuite

import java.net.ServerSocket
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

final class EchoIntegrationSuite extends FunSuite:
  private given ExecutionContext = ExecutionContext.global

  test("client finds open echo port") {
    val openPort = findFreePort()
    val server = EchoServer("127.0.0.1", List(openPort))
    val report = server.start()

    try
      assertEquals(report.startedPorts, List(openPort))
      val detected = EchoClient.scanOpenPorts("127.0.0.1", List(openPort), timeoutMs = 300)
      assertEquals(detected, List(openPort))
    finally server.stop()
  }

  test("client excludes closed port") {
    val closedPort = findFreePort()
    val detected = EchoClient.scanOpenPorts("127.0.0.1", List(closedPort), timeoutMs = 200)
    assertEquals(detected, Nil)
  }

  test("server handles concurrent probes") {
    val openPort = findFreePort()
    val server = EchoServer("127.0.0.1", List(openPort))
    server.start()

    try
      val scans = Future.traverse(1 to 20)(_ =>
        Future(EchoClient.scanOpenPorts("127.0.0.1", List(openPort), timeoutMs = 300))
      )
      val results = Await.result(scans, 10.seconds)
      assert(results.forall(_ == List(openPort)))
    finally server.stop()
  }

  private def findFreePort(): Int =
    val socket = ServerSocket(0)
    try socket.getLocalPort
    finally socket.close()

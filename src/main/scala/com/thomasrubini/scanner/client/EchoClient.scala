package com.thomasrubini.scanner.client

import java.net.InetSocketAddress
import java.net.Socket
import com.thomasrubini.scanner.net.SocketIo
import scala.util.Using

object EchoClient:
  private val ProbePayload = "scala-scanner-probe".getBytes("UTF-8")

  def scanOpenPorts(host: String, ports: List[Int], timeoutMs: Int): List[Int] =
    ports.filter(port => isEchoOpen(host, port, timeoutMs)).sorted

  private def isEchoOpen(host: String, port: Int, timeoutMs: Int): Boolean =
    Using(new Socket()) { socket =>
      socket.connect(InetSocketAddress(host, port), timeoutMs)
      socket.setSoTimeout(timeoutMs)

      val output = socket.getOutputStream
      output.write(ProbePayload)
      output.flush()
      socket.shutdownOutput()

      val echoed = SocketIo.readAll(socket.getInputStream)
      echoed.sameElements(ProbePayload)
    }.getOrElse(false)

package com.thomasrubini.scanner

import com.thomasrubini.scanner.cli.CliParser
import com.thomasrubini.scanner.cli.Command
import com.thomasrubini.scanner.cli.Transport
import com.thomasrubini.scanner.client.EchoClient
import com.thomasrubini.scanner.server.EchoServer

object Main:
  def main(args: Array[String]): Unit =
    CliParser.parse(args) match
      case Left(error) =>
        Console.err.println(error)
        sys.exit(1)
      case Right(Command.Server(range, host, transport, ipVersion)) =>
        runServer(host, range.ports, transport, ipVersion)
      case Right(Command.Client(range, host, timeoutMs, transport, ipVersion)) =>
        runClient(host, range.map(_.ports), timeoutMs, transport, ipVersion)

  private def runServer(
      host: String,
      ports: List[Int],
      transport: com.thomasrubini.scanner.cli.Transport,
      ipVersion: com.thomasrubini.scanner.cli.IpVersion
  ): Unit =
    val server = EchoServer(host, ports, transport, ipVersion)
    val report = server.start()

    if report.startedPorts.isEmpty then
      Console.err.println("No ports were bound successfully.")
      report.failedPorts.toList.sortBy(_._1).foreach { case (port, reason) =>
        Console.err.println(s"$port: $reason")
      }
      sys.exit(1)

    println(s"Listening on ${report.startedPorts.mkString(",")} (${transport.toString.toLowerCase}, ${ipVersion.toString.toLowerCase})")
    report.failedPorts.toList.sortBy(_._1).foreach { case (port, reason) =>
      Console.err.println(s"Failed to bind $port: $reason")
    }

    Runtime.getRuntime.addShutdownHook(Thread(() => server.stop()))
    server.awaitTermination()

  private def runClient(
      host: String,
      ports: Option[List[Int]],
      timeoutMs: Int,
      transport: com.thomasrubini.scanner.cli.Transport,
      ipVersion: com.thomasrubini.scanner.cli.IpVersion
  ): Unit =
    transport match
      case Transport.Ip =>
        EchoClient.checkIpReachable(host, timeoutMs, ipVersion) match
          case Left(error) =>
            Console.err.println(error)
            sys.exit(1)
          case Right(isReachable) =>
            println(if isReachable then "reachable" else "unreachable")
      case _ =>
        val selectedPorts = ports.getOrElse(Nil)
        EchoClient.scanOpenPorts(host, selectedPorts, timeoutMs, transport, ipVersion) match
          case Left(error) =>
            Console.err.println(error)
            sys.exit(1)
          case Right(openPorts) =>
            openPorts.foreach(port => println(port.toString))

package com.thomasrubini.scanner

import com.thomasrubini.scanner.cli.CliParser
import com.thomasrubini.scanner.cli.Command
import com.thomasrubini.scanner.client.EchoClient
import com.thomasrubini.scanner.server.EchoServer

object Main:
  def main(args: Array[String]): Unit =
    CliParser.parse(args) match
      case Left(error) =>
        Console.err.println(error)
        sys.exit(1)
      case Right(Command.Server(range, host)) =>
        runServer(host, range.ports)
      case Right(Command.Client(range, host, timeoutMs)) =>
        runClient(host, range.ports, timeoutMs)

  private def runServer(host: String, ports: List[Int]): Unit =
    val server = EchoServer(host, ports)
    val report = server.start()

    if report.startedPorts.isEmpty then
      Console.err.println("No ports were bound successfully.")
      report.failedPorts.toList.sortBy(_._1).foreach { case (port, reason) =>
        Console.err.println(s"$port: $reason")
      }
      sys.exit(1)

    println(s"Listening on ${report.startedPorts.mkString(",")}")
    report.failedPorts.toList.sortBy(_._1).foreach { case (port, reason) =>
      Console.err.println(s"Failed to bind $port: $reason")
    }

    Runtime.getRuntime.addShutdownHook(Thread(() => server.stop()))
    server.awaitTermination()

  private def runClient(host: String, ports: List[Int], timeoutMs: Int): Unit =
    val openPorts = EchoClient.scanOpenPorts(host, ports, timeoutMs)
    openPorts.foreach(port => println(port.toString))

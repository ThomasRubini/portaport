package com.thomasrubini.scanner

import com.thomasrubini.scanner.cli.CliParser
import com.thomasrubini.scanner.cli.Command
import com.thomasrubini.scanner.cli.Transport
import com.thomasrubini.scanner.cli.IpVersion
import com.thomasrubini.scanner.cli.PortRange
import com.thomasrubini.scanner.client.EchoClient
import com.thomasrubini.scanner.client.ScanResult
import com.thomasrubini.scanner.server.EchoServer

object Main:
  private val useColor = System.getenv("NO_COLOR") == null

  private object Color:
    val Reset = if useColor then "\u001b[0m" else ""
    val Green = if useColor then "\u001b[32m" else ""
    val Red = if useColor then "\u001b[31m" else ""
    val Yellow = if useColor then "\u001b[33m" else ""
    val Cyan = if useColor then "\u001b[36m" else ""
    val Bold = if useColor then "\u001b[1m" else ""
    val Dim = if useColor then "\u001b[2m" else ""

  private def formatElapsed(nanos: Long): String =
    val ms = nanos / 1_000_000
    if ms < 1000 then s"${ms}ms"
    else f"${ms / 1000.0}%.1fs"

  private def transportStr(t: Transport): String = t.toString.toLowerCase

  private def ipVersionStr(v: IpVersion): String = v match
    case IpVersion.V4 => "ipv4"
    case IpVersion.V6 => "ipv6"

  def main(args: Array[String]): Unit =
    CliParser.parse(args) match
      case Left(error) =>
        Console.err.println(s"${Color.Red}Error: $error${Color.Reset}")
        sys.exit(1)
      case Right(Command.Server(range, host, transport, ipVersion)) =>
        runServer(host, range.ports, transport, ipVersion)
      case Right(Command.Client(range, host, timeoutMs, transport, ipVersion)) =>
        runClient(host, range.map(_.ports), timeoutMs, transport, ipVersion)

  private def runServer(
      host: String,
      ports: List[Int],
      transport: Transport,
      ipVersion: IpVersion
  ): Unit =
    println(
      s"\n${Color.Cyan}── Server — $host (${transportStr(transport)}, ${ipVersionStr(ipVersion)})${Color.Reset}"
    )

    val server = EchoServer(host, ports, transport, ipVersion)
    val report = server.start()

    report.startedPorts.foreach { port =>
      println(s"  ${Color.Green}✓${Color.Reset}  $port  ${Color.Dim}bound${Color.Reset}")
    }
    report.failedPorts.toList.sortBy(_._1).foreach { case (port, reason) =>
      println(s"  ${Color.Red}✗${Color.Reset}  $port  ${Color.Red}$reason${Color.Reset}")
    }

    if report.startedPorts.isEmpty then
      println(s"\n  ${Color.Red}No ports were bound successfully.${Color.Reset}")
      sys.exit(1)

    val boundStr = PortRange.formatRanges(report.startedPorts)
    println(
      s"\n  ${Color.Bold}Listening on:${Color.Reset} $boundStr  ${Color.Dim}(${transportStr(transport)}, ${ipVersionStr(ipVersion)})${Color.Reset}"
    )

    Runtime.getRuntime.addShutdownHook(Thread(() => server.stop()))
    server.awaitTermination()

  private def runClient(
      host: String,
      ports: Option[List[Int]],
      timeoutMs: Int,
      transport: Transport,
      ipVersion: IpVersion
  ): Unit =
    transport match
      case Transport.Ip =>
        println(s"\n${Color.Cyan}── Host Check — $host${Color.Reset}")
        val startTime = System.nanoTime()
        EchoClient.checkIpReachable(host, timeoutMs, ipVersion) match
          case Left(error) =>
            println(s"  ${Color.Red}✗${Color.Reset}  $error")
            sys.exit(1)
          case Right(isReachable) =>
            val elapsed = formatElapsed(System.nanoTime() - startTime)
            val status =
              if isReachable then s"${Color.Green}reachable${Color.Reset}"
              else s"${Color.Red}unreachable${Color.Reset}"
            println(s"  $status  ${Color.Dim}($elapsed)${Color.Reset}")

      case _ =>
        val selectedPorts = ports.getOrElse(Nil)
        if selectedPorts.isEmpty then
          Console.err.println(s"${Color.Red}Error: No ports specified.${Color.Reset}")
          sys.exit(1)

        val total = selectedPorts.size
        println(s"\n${Color.Cyan}── Scan — $host (${transportStr(transport)})${Color.Reset}")
        print(
          s"  ${Color.Dim}Scanning $total port${if total != 1 then "s" else ""}...${Color.Reset}"
        )
        Console.flush()

        val startTime = System.nanoTime()

        val result = EchoClient.scanOpenPorts(
          host,
          selectedPorts,
          timeoutMs,
          transport,
          ipVersion,
          onProgress = (port, done) =>
            if useColor then
              print(s"\r  ${Color.Dim}Scanning port $port... ($done/$total)${Color.Reset}")
              Console.flush()
        )

        val elapsed = formatElapsed(System.nanoTime() - startTime)

        if useColor then print("\r" + " " * 60 + "\r")

        result match
          case Left(error) =>
            println(s"\n  ${Color.Red}✗${Color.Reset}  $error")
            sys.exit(1)
          case Right(ScanResult(open, closed)) =>
            if open.nonEmpty then
              println(
                s"  ${Color.Green}${Color.Bold}OPEN${Color.Reset}    ${PortRange.formatRanges(open)}"
              )
            if closed.nonEmpty then
              println(
                s"  ${Color.Red}${Color.Bold}CLOSED${Color.Reset}  ${PortRange.formatRanges(closed)}"
              )
            println(
              s"\n  ${Color.Dim}Summary: ${Color.Green}${open.size} open${Color.Reset}${Color.Dim}, ${Color.Red}${closed.size} closed${Color.Reset}${Color.Dim}  ($elapsed)${Color.Reset}"
            )

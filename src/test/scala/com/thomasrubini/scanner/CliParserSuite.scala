package com.thomasrubini.scanner

import com.thomasrubini.scanner.cli.CliParser
import com.thomasrubini.scanner.cli.Command
import com.thomasrubini.scanner.cli.IpVersion
import com.thomasrubini.scanner.cli.PortRange
import com.thomasrubini.scanner.cli.Transport
import munit.FunSuite

final class CliParserSuite extends FunSuite:
  test("parse server command") {
    val parsed = CliParser.parse(Array("server", "--ports", "7000-7001", "--host", "127.0.0.1"))
    assertEquals(parsed, Right(Command.Server(PortRange(7000, 7001), "127.0.0.1", Transport.Tcp, IpVersion.V4)))
  }

  test("parse client command with defaults") {
    val parsed = CliParser.parse(Array("client", "--ports", "9000-9002"))
    assertEquals(parsed, Right(Command.Client(Some(PortRange(9000, 9002)), "127.0.0.1", 500, Transport.Tcp, IpVersion.V4)))
  }

  test("client requires valid timeout") {
    assert(CliParser.parse(Array("client", "--ports", "9000-9002", "--timeout-ms", "0")).isLeft)
  }

  test("missing required options returns usage") {
    val parsed = CliParser.parse(Array("server"))
    assert(parsed.isLeft)
    assert(parsed.swap.toOption.get.contains("Missing required option --ports"))
  }

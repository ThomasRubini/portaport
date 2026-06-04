package com.thomasrubini.scanner.cli

sealed trait Command

object Command:
  final case class Server(ports: PortRange, host: String, transport: Transport, ipVersion: IpVersion)
      extends Command
  final case class Client(
    ports: Option[PortRange],
      host: String,
      timeoutMs: Int,
      transport: Transport,
      ipVersion: IpVersion
  ) extends Command

enum Transport:
  case Tcp, Udp, Ip

object Transport:
  def parse(value: String): Either[String, Transport] =
    value.toLowerCase match
      case "tcp" => Right(Transport.Tcp)
      case "udp" => Right(Transport.Udp)
      case "ip" => Right(Transport.Ip)
      case _     => Left(s"Invalid protocol: '$value'. Expected one of: tcp, udp, ip")

enum IpVersion:
  case V4, V6

object IpVersion:
  def parse(value: String): Either[String, IpVersion] =
    value.toLowerCase match
      case "ipv4" | "4" => Right(IpVersion.V4)
      case "ipv6" | "6" => Right(IpVersion.V6)
      case _               => Left(s"Invalid IP version: '$value'. Expected one of: ipv4, ipv6")

object CliParser:
  private val DefaultHost = "127.0.0.1"
  private val DefaultTimeoutMs = 500
  private val DefaultTransport = Transport.Tcp
  private val DefaultIpVersion = IpVersion.V4

  def parse(args: Array[String]): Either[String, Command] =
    args.toList match
      case "server" :: tail => parseServer(tail)
      case "client" :: tail => parseClient(tail)
      case Nil              => Left(usage("Missing command"))
      case unknown :: _     => Left(usage(s"Unknown command: '$unknown'"))

  private def parseServer(args: List[String]): Either[String, Command] =
    parseKeyValueArgs(args).flatMap { opts =>
      for
        transport <- opts
          .get("--protocol")
          .map(Transport.parse)
          .getOrElse(Right(DefaultTransport))
        ipVersion <- opts
          .get("--ip")
          .map(IpVersion.parse)
          .getOrElse(Right(DefaultIpVersion))
        portsRaw <- opts.get("--ports").toRight("Missing required option --ports")
        ports <- PortRange.parse(portsRaw)
        _ <- Either.cond(
          transport != Transport.Ip,
          (),
          "server does not support --protocol ip (use --protocol tcp or udp)"
        )
      yield Command.Server(ports, opts.getOrElse("--host", DefaultHost), transport, ipVersion)
    }

  private def parseClient(args: List[String]): Either[String, Command] =
    parseKeyValueArgs(args).flatMap { opts =>
      for
        timeoutMs <- opts
          .get("--timeout-ms")
          .map(parsePositiveInt("--timeout-ms", _))
          .getOrElse(Right(DefaultTimeoutMs))
        transport <- opts
          .get("--protocol")
          .map(Transport.parse)
          .getOrElse(Right(DefaultTransport))
        ipVersion <- opts
          .get("--ip")
          .map(IpVersion.parse)
          .getOrElse(Right(DefaultIpVersion))
        ports <- transport match
          case Transport.Ip => Right(None)
          case _ =>
            opts.get("--ports") match
              case None => Left("Missing required option --ports for tcp/udp scan")
              case Some(portsRaw) => PortRange.parse(portsRaw).map(Some(_))
      yield Command.Client(
        ports = ports,
        host = opts.getOrElse("--host", DefaultHost),
        timeoutMs = timeoutMs,
        transport = transport,
        ipVersion = ipVersion
      )
    }

  private def parseKeyValueArgs(args: List[String]): Either[String, Map[String, String]] =
    @annotation.tailrec
    def loop(rest: List[String], acc: Map[String, String]): Either[String, Map[String, String]] =
      rest match
        case key :: value :: tail if key.startsWith("--") => loop(tail, acc.updated(key, value))
        case key :: Nil if key.startsWith("--") => Left(s"Missing value for option '$key'")
        case token :: _                         => Left(s"Unexpected token: '$token'")
        case Nil                                => Right(acc)

    loop(args, Map.empty)

  private def parsePositiveInt(name: String, value: String): Either[String, Int] =
    scala.util
      .Try(value.toInt)
      .toEither
      .left
      .map(_ => s"Invalid integer for $name: '$value'")
      .flatMap { parsed =>
        Either.cond(parsed > 0, parsed, s"$name must be > 0")
      }

  def usage(error: String): String =
    s"""$error
       |
       |Usage:
       |  server --ports START-END [--host HOST] [--protocol tcp|udp] [--ip ipv4|ipv6]
       |  client --ports START-END [--host HOST] [--timeout-ms MILLIS] [--protocol tcp|udp] [--ip ipv4|ipv6]
       |  client --host HOST [--timeout-ms MILLIS] --protocol ip [--ip ipv4|ipv6]
       |""".stripMargin

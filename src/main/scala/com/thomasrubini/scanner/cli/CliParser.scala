package com.thomasrubini.scanner.cli

sealed trait Command

object Command:
  final case class Server(ports: PortRange, host: String) extends Command
  final case class Client(ports: PortRange, host: String, timeoutMs: Int) extends Command

object CliParser:
  private val DefaultHost = "127.0.0.1"
  private val DefaultTimeoutMs = 500

  def parse(args: Array[String]): Either[String, Command] =
    args.toList match
      case "server" :: tail => parseServer(tail)
      case "client" :: tail => parseClient(tail)
      case Nil              => Left(usage("Missing command"))
      case unknown :: _     => Left(usage(s"Unknown command: '$unknown'"))

  private def parseServer(args: List[String]): Either[String, Command] =
    parseKeyValueArgs(args).flatMap { opts =>
      for
        portsRaw <- opts.get("--ports").toRight("Missing required option --ports")
        ports <- PortRange.parse(portsRaw)
      yield Command.Server(ports, opts.getOrElse("--host", DefaultHost))
    }

  private def parseClient(args: List[String]): Either[String, Command] =
    parseKeyValueArgs(args).flatMap { opts =>
      for
        portsRaw <- opts.get("--ports").toRight("Missing required option --ports")
        ports <- PortRange.parse(portsRaw)
        timeoutMs <- opts
          .get("--timeout-ms")
          .map(parsePositiveInt("--timeout-ms", _))
          .getOrElse(Right(DefaultTimeoutMs))
      yield Command.Client(ports, opts.getOrElse("--host", DefaultHost), timeoutMs)
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
       |  server --ports START-END [--host HOST]
       |  client --ports START-END [--host HOST] [--timeout-ms MILLIS]
       |""".stripMargin

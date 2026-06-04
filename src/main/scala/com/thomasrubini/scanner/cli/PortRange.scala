package com.thomasrubini.scanner.cli

final case class PortRange(start: Int, end: Int):
  require(start <= end, s"Invalid port range: $start-$end")

  def ports: List[Int] = (start to end).toList

object PortRange:
  private val PortBounds = 1 to 65535

  def parse(value: String): Either[String, PortRange] =
    value.split("-", -1).toList match
      case startRaw :: endRaw :: Nil =>
        for
          start <- parsePort(startRaw)
          end <- parsePort(endRaw)
          _ <- Either.cond(start <= end, (), s"Invalid range: $start-$end (start must be <= end)")
        yield PortRange(start, end)
      case _ => Left(s"Invalid range format: '$value'. Expected format: START-END")

  private def parsePort(raw: String): Either[String, Int] =
    scala.util
      .Try(raw.toInt)
      .toEither
      .left
      .map(_ => s"Invalid port: '$raw'")
      .flatMap { port =>
        Either.cond(PortBounds.contains(port), port, s"Port out of bounds: $port (valid 1-65535)")
      }

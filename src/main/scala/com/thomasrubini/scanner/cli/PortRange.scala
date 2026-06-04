package com.thomasrubini.scanner.cli

final case class PortRange(start: Int, end: Int):
  require(start <= end, s"Invalid port range: $start-$end")

  def ports: List[Int] = (start to end).toList

object PortRange:
  private val PortBounds = 1 to 65535

  /** Formats a list of ports into merged range strings. e.g. List(9000,9001,9002,9005) ->
    * "9000-9002, 9005"
    */
  def formatRanges(ports: List[Int]): String =
    if ports.isEmpty then ""
    else
      val sorted = ports.sorted
      val sb = new StringBuilder
      var i = 0
      while i < sorted.length do
        if sb.nonEmpty then sb.append(", ")
        val start = sorted(i)
        var end = start
        while i + 1 < sorted.length && sorted(i + 1) == end + 1 do
          i += 1
          end = sorted(i)
        if start == end then sb.append(start.toString)
        else sb.append(s"$start-$end")
        i += 1
      sb.result()

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

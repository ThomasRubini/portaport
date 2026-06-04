package com.thomasrubini.scanner.net

import com.thomasrubini.scanner.cli.IpVersion
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import scala.util.Try

object IpAddressResolver:
  /** Resolves the host to an IP address matching the requested IP version. */
  def resolve(host: String, ipVersion: IpVersion): Either[String, InetAddress] =
    Try(InetAddress.getAllByName(host).toList).toEither.left.map(_.getMessage).flatMap {
      addresses =>
        addresses
          .find(address => matches(address, ipVersion))
          .toRight(s"No ${ipVersion.toString.toLowerCase} address found for host '$host'")
    }

  /** Checks whether an InetAddress belongs to the requested IP family. */
  private def matches(address: InetAddress, ipVersion: IpVersion): Boolean =
    ipVersion match
      case IpVersion.V4 => address.isInstanceOf[Inet4Address]
      case IpVersion.V6 => address.isInstanceOf[Inet6Address]

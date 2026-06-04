package com.thomasrubini.scanner

import com.thomasrubini.scanner.cli.PortRange
import munit.FunSuite

final class PortRangeSuite extends FunSuite:
  test("parse valid range") {
    assertEquals(PortRange.parse("1000-1002"), Right(PortRange(1000, 1002)))
  }

  test("reject invalid format") {
    assert(PortRange.parse("1000").isLeft)
  }

  test("reject invalid bounds") {
    assert(PortRange.parse("0-10").isLeft)
    assert(PortRange.parse("10-65536").isLeft)
  }

  test("reject reverse ranges") {
    assert(PortRange.parse("10-1").isLeft)
  }

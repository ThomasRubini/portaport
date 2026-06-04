package com.thomasrubini.scanner.net

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.SocketTimeoutException

object SocketIo:
  def readAll(input: InputStream): Array[Byte] =
    val buffer = Array.ofDim[Byte](1024)
    val output = ByteArrayOutputStream()
    try
      var bytesRead = input.read(buffer)
      while bytesRead != -1 do
        output.write(buffer, 0, bytesRead)
        bytesRead = input.read(buffer)
    catch case _: SocketTimeoutException => ()
    output.toByteArray

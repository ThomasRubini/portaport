# scala-scanner

A port scanner and echo server built with Scala Native.

## Features

- **Echo server** — listens on TCP or UDP ports and echoes any received payload
- **Port scanner** — detects open ports by sending a probe and verifying the echo response
- **Protocols** — TCP and UDP scanning, plus IP-level reachability via `ping`
- **Dual stack** — full IPv4 and IPv6 support, with per-family address resolution
- **Cross-platform** — works on Linux, macOS, and Windows (ping adapted per OS)

---

## Build

Requirements:
- JDK 17 or later
- sbt 1.10 or later
- `clang` and `lld` (required for native build)

### Native build (recommended)

A native binary builds quickly and runs faster than JVM mode.

Install the toolchain:

On Linux:
```bash
sudo apt-get update
sudo apt-get install -y clang lld
```

On macOS, clang is included with Xcode command line tools:
```bash
xcode-select --install
```

Build:

```bash
sbt clean nativeLink
```

The binary will be at:
- `target/scala-3.3.3/scala-scanner` (Linux / macOS)
- `target/scala-3.3.3/scala-scanner.exe` (Windows)

For convenience, copy it to the project root:
```bash
cp target/scala-3.3.3/scala-scanner .
```

---

## Quickstart

Start a server:

```bash
./scala-scanner server --ports 9000-9002 --host 127.0.0.1
```

Scan from another terminal:

```bash
./scala-scanner client --ports 9000-9002 --host 127.0.0.1 --timeout-ms 500
```
---

## All Options

### Server
- `--host <addr>` - bind address (default: 127.0.0.1)
- `--ports <range>` - port range, e.g. `9000-9010` (required)
- `--protocol <tcp|udp>` - protocol (default: tcp)
- `--ip <ipv4|ipv6>` - IP version (default: ipv4)

### Client
- `--host <addr>` - target address (required for port scan)
- `--ports <range>` - port range, e.g. `9000-9010`
- `--protocol <tcp|udp|ip>` - protocol (default: tcp)
- `--ip <ipv4|ipv6>` - IP version (default: ipv4)
- `--timeout-ms <ms>` - timeout in milliseconds (default: 1000)

---

## Examples

### TCP scan (default)

```bash
./scala-scanner server --ports 9000-9002 --host 127.0.0.1
# or: ./target/scala-3.3.3/scala-scanner server --ports 9000-9002 --host 127.0.0.1
./scala-scanner client --ports 9000-9002 --host 127.0.0.1 --timeout-ms 500
```

### UDP scan

```bash
./scala-scanner server --ports 9000-9002 --host 127.0.0.1 --protocol udp
./scala-scanner client --ports 9000-9002 --host 127.0.0.1 --protocol udp --timeout-ms 500
```

### IPv6

```bash
./scala-scanner server --ports 9000-9002 --host ::1 --ip ipv6
./scala-scanner client --ports 9000-9002 --host ::1 --ip ipv6 --timeout-ms 500
```

### IP reachability check

```bash
./scala-scanner client --host 127.0.0.1 --protocol ip --timeout-ms 500 --ip ipv4
```

### Via sbt (no native build)

```bash
sbt "run server --ports 9000-9002 --host 127.0.0.1"
sbt "run client --ports 9000-9002 --host 127.0.0.1 --timeout-ms 500"
```

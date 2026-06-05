# portaport

A Scala CLI with two modes:

- server - listens on a TCP/UDP port range and echoes payloads
- client - scans a TCP/UDP port range and prints reachable ports

Supports IPv4 and IPv6. Client also supports IP reachability checks.

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
- `target/scala-3.3.3/portaport` (Linux / macOS)
- `target/scala-3.3.3/portaport.exe` (Windows)

For convenience, copy it to the project root:
```bash
cp target/scala-3.3.3/portaport .
```

---

## Quickstart

Start a server:

```bash
./portaport server --ports 9000-9002 --host 127.0.0.1
```

Scan from another terminal:

```bash
./portaport client --ports 9000-9002 --host 127.0.0.1 --timeout-ms 500
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
./portaport server --ports 9000-9002 --host 127.0.0.1
# or: ./target/scala-3.3.3/portaport server --ports 9000-9002 --host 127.0.0.1
./portaport client --ports 9000-9002 --host 127.0.0.1 --timeout-ms 500
```

### UDP scan

```bash
./portaport server --ports 9000-9002 --host 127.0.0.1 --protocol udp
./portaport client --ports 9000-9002 --host 127.0.0.1 --protocol udp --timeout-ms 500
```

### IPv6

```bash
./portaport server --ports 9000-9002 --host ::1 --ip ipv6
./portaport client --ports 9000-9002 --host ::1 --ip ipv6 --timeout-ms 500
```

### IP reachability check

```bash
./portaport client --host 127.0.0.1 --protocol ip --timeout-ms 500 --ip ipv4
```

### Via sbt (no native build)

```bash
sbt "run server --ports 9000-9002 --host 127.0.0.1"
sbt "run client --ports 9000-9002 --host 127.0.0.1 --timeout-ms 500"
```

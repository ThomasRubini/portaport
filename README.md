# scala-scanner

![CI](https://github.com/ThomasRubini/scala-scanner/actions/workflows/ci.yml/badge.svg)

A Scala 3 CLI with two modes:
- `server`: listens on a TCP/UDP port range and echoes payloads.
- `client`: scans a TCP/UDP port range and prints reachable ports.

Supports `ipv4` and `ipv6`. Client also supports IP reachability with `--protocol ip`.

# Usage

## Quickstart

1. Start server in one terminal:

```bash
sbt "run server --ports 9000-9002 --host 127.0.0.1"
```

2. Scan from another terminal:

```bash
sbt "run client --ports 9000-9002 --host 127.0.0.1 --timeout-ms 500"
```

Expected output (one open port per line):

```text
9000
9001
9002
```

## Advanced usage

UDP example:

```bash
sbt "run server --ports 9000-9002 --host 127.0.0.1 --protocol udp"
sbt "run client --ports 9000-9002 --host 127.0.0.1 --protocol udp --timeout-ms 500"
```

IPv6 example:

```bash
sbt "run server --ports 9000-9002 --host ::1 --ip ipv6"
sbt "run client --ports 9000-9002 --host ::1 --ip ipv6 --timeout-ms 500"
```

IP-level check (no ports):

```bash
sbt "run client --host 127.0.0.1 --protocol ip --timeout-ms 500 --ip ipv4"
```

Example output:

```text
reachable
```

```text
9000
9001
9002
```

# Build

## Requirements
- JDK 17+
- sbt 1.10+
- for native build (Linux): `clang` and `lld`


## Native build

Install toolchain (Linux):

```bash
sudo apt-get update
sudo apt-get install -y clang lld
```

Build native binary:

```bash
sbt clean nativeLink
```

Binary output:
- `target/scala-3.3.3/scala-scanner` (Linux/macOS)
- `target/scala-3.3.3/scala-scanner.exe` (Windows)

Run native binary:

```bash
./target/scala-3.3.3/scala-scanner client --host 127.0.0.1 --protocol ip --timeout-ms 500 --ip ipv4
```

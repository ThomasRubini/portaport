# scala-scanner

![CI](https://github.com/ThomasRubini/scala-scanner/actions/workflows/ci.yml/badge.svg)

A Scala 3 CLI with two modes:
- `server`: binds on a TCP or UDP port range and echoes back all received payloads.
- `client`: scans a TCP or UDP port range and reports ports that successfully complete an echo probe.

Both modes support selecting IP family (`ipv4` or `ipv6`).

Client mode also supports an IP-level reachability check via `--protocol ip`.

## Requirements
- JDK 17+
- sbt 1.10+

## Quickstart

Start server on localhost ports `9000-9002`:

```bash
sbt "run server --ports 9000-9002 --host 127.0.0.1"
```

Scan the same range:

```bash
sbt "run client --ports 9000-9002 --host 127.0.0.1 --timeout-ms 500"
```

Use UDP instead of TCP:

```bash
sbt "run server --ports 9000-9002 --host 127.0.0.1 --protocol udp"
sbt "run client --ports 9000-9002 --host 127.0.0.1 --protocol udp --timeout-ms 500"
```

Use IPv6:

```bash
sbt "run server --ports 9000-9002 --host ::1 --ip ipv6"
sbt "run client --ports 9000-9002 --host ::1 --ip ipv6 --timeout-ms 500"
```

Run an IP-level check (no ports):

```bash
sbt "run client --host 127.0.0.1 --protocol ip --timeout-ms 500 --ip ipv4"
```

Output:

```text
reachable
```

Expected output example (one open port per line):

```text
9000
9001
9002
```

## Common commands

```bash
sbt fmtCheck
sbt test
```

# scala-scanner

![CI](https://github.com/ThomasRubini/scala-scanner/actions/workflows/ci.yml/badge.svg)

A Scala 3 CLI with two modes:
- `server`: binds on a TCP port range and echoes back all received payloads.
- `client`: scans a TCP port range and reports ports that successfully complete an echo probe.

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

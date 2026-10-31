#!/usr/bin/env python3
"""Turn failed ctest suites into GitHub Actions annotations.

Reads Testing/Temporary/LastTest.log from a build dir and prints one
`::error::` (then `::warning::`, then `::notice::`) line per failed unit test,
so the failing test names and messages survive even when the job log itself
is not accessible. GitHub caps annotations at 10 per level per step.

Usage: ctest_failures_to_annotations.py <build-dir>
"""

import re
import sys
from pathlib import Path

PER_LEVEL = 10
LEVELS = ("error", "warning", "notice")
MAX_MESSAGE = 1500

TEST_HEADER = re.compile(r"^\d+/\d+ Test: (?P<name>\S+)")
RUNNING = re.compile(r"Running (?P<name>\S+)\s*$")


def parse(log: str):
    """Yield (suite, unit_test, message) for each failed unit test / crashed suite."""
    suite = None
    running = None
    tail = []  # output lines of the current ctest test
    for line in log.splitlines():
        header = TEST_HEADER.match(line)
        if header:
            suite = header.group("name")
            running = None
            tail = []
            continue
        if suite is None:
            continue
        if line.startswith("Test Passed."):
            suite = None
            continue
        if line.startswith("Test Failed.") or line.startswith("Test Timeout.") or line.startswith("Test Crashed."):
            # Collect per-unit-test failures from the suite output.
            failures = []
            for i, out in enumerate(tail):
                if "[FAILED]" in out:
                    detail = " ".join(s.strip() for s in tail[i + 1 : i + 3] if s.strip())
                    failures.append((suite, running_before(tail, i), detail or out.strip()))
            if not failures:
                # No framework-level failure: a crash, abort, timeout or ASSERT failure.
                detail = " | ".join(s.strip() for s in tail[-6:] if s.strip())
                failures.append((suite, running or "<startup>", line.strip() + " " + detail))
            yield from failures
            suite = None
            continue
        tail.append(line)
        m = RUNNING.search(line)
        if m:
            running = m.group("name")


def running_before(lines, index):
    for out in reversed(lines[:index]):
        m = RUNNING.search(out)
        if m:
            return m.group("name")
    return "<unknown>"


def escape(message: str) -> str:
    return message.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")[:MAX_MESSAGE]


def main() -> int:
    build_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("build")
    log_path = build_dir / "Testing" / "Temporary" / "LastTest.log"
    if not log_path.exists():
        print(f"::notice::No ctest log at {log_path}")
        return 0

    failures = list(parse(log_path.read_text(encoding="utf-8", errors="replace")))
    if not failures:
        print("::notice::ctest failed but no failing unit test was found in LastTest.log")
        return 0

    for i, (suite, test, message) in enumerate(failures):
        level = LEVELS[min(i // PER_LEVEL, len(LEVELS) - 1)]
        print(f"::{level} title={suite}::{escape(test + ': ' + message)}")
    print(f"::notice::{len(failures)} failing unit test(s) in {len({f[0] for f in failures})} suite(s)")
    return 0


if __name__ == "__main__":
    sys.exit(main())

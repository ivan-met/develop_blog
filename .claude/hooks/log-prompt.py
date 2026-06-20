#!/usr/bin/env python3
"""UserPromptSubmit hook: append each submitted prompt to docs/prompt_logs.

Reads the hook payload (JSON) from stdin and writes one timestamped entry per
prompt to a per-day log file. Failures are swallowed so the hook never blocks
prompt submission.
"""
import sys
import os
import json
import datetime

try:
    data = json.load(sys.stdin)
except Exception:
    data = {}

prompt = (data.get("prompt") or "").strip()
if not prompt:
    sys.exit(0)

# Hooks run from the project root; resolve relative to this file to be safe.
project_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
log_dir = os.path.join(project_root, "docs", "prompt_logs")
os.makedirs(log_dir, exist_ok=True)

now = datetime.datetime.now()
log_file = os.path.join(log_dir, now.strftime("%Y-%m-%d") + ".log")
session = (data.get("session_id") or "")[:8]
timestamp = now.isoformat(timespec="seconds")

entry = f"[{timestamp}] (session {session})\n{prompt}\n\n"

try:
    with open(log_file, "a", encoding="utf-8") as fh:
        fh.write(entry)
except Exception:
    pass

sys.exit(0)

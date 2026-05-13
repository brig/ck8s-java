#!/usr/bin/env python3
import sys

venv = sys.prefix != sys.base_prefix
print(f"venv: {venv}")
assert venv

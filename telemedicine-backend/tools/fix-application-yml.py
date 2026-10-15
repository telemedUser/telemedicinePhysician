#!/usr/bin/env python3
"""Fix scaffolded application.yml placeholder corruption."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

for yml in ROOT.rglob("application.yml"):
    text = yml.read_text(encoding="utf-8")
    import re
    text = re.sub(r"\$\{CONSUL_\d+:8500\}", "${CONSUL_PORT:8500}", text)
    text = re.sub(r"\$\{DB_\d+:5432\}", "${DB_PORT:5432}", text)
    if "profiles:" not in text and "spring:" in text:
        text = text.replace("spring:\n  application:", "spring:\n  profiles:\n    active: dev\n  application:")
    text = text.replace("ddl-auto: validate", "ddl-auto: update")
    yml.write_text(text, encoding="utf-8")
    print(f"fixed {yml.relative_to(ROOT)}")

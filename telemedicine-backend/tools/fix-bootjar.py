import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
pattern = re.compile(
    r"bootJar \{ archiveFileName = 'SERVICE\.jar'\.Replace\('SERVICE', '([^']+)'\) \}"
)
for path in root.rglob("build.gradle"):
    text = path.read_text(encoding="utf-8")
    updated = pattern.sub(r"bootJar { archiveFileName = '\1.jar' }", text)
    if updated != text:
        path.write_text(updated, encoding="utf-8")
        print(f"fixed {path}")

scaffold = root / "tools" / "scaffold-java-service.ps1"
st = scaffold.read_text(encoding="utf-8")
st = st.replace(
    "bootJar { archiveFileName = 'SERVICE.jar'.Replace('SERVICE', 'MODULE') }",
    "bootJar { archiveFileName = 'MODULE.jar' }",
)
scaffold.write_text(st, encoding="utf-8")
print("fixed scaffold")

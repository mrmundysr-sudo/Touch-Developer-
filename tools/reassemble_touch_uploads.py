#!/usr/bin/env python3
"""Reassemble Touch Developer REST-uploaded archives after checkout, without Git LFS."""
from pathlib import Path
import hashlib
import sys

root = Path.cwd().resolve()
for manifest in sorted(root.rglob("*.touchmanifest")):
    if ".git" in manifest.parts:
        continue
    data = manifest.read_text(encoding="utf-8").splitlines()
    if not data or data[0] != "TOUCH_CHUNKS_V1":
        raise RuntimeError(f"Unsupported Touch manifest: {manifest}")
    meta = dict(line.split("=", 1) for line in data[1:] if "=" in line)
    target = (root / meta["path"]).resolve()
    if not target.is_relative_to(root) or target.with_name(target.name + ".touchmanifest") != manifest:
        raise RuntimeError(f"Unsafe manifest target: {manifest}")
    digest = meta["sha256"]
    count = int(meta["parts"])
    expected_size = int(meta["size"])
    if not (1 <= count <= 200) or not (0 <= expected_size <= 100_000_000):
        raise RuntimeError(f"Invalid part count or size: {manifest}")
    folder = root / ".touch-uploads" / digest
    target.parent.mkdir(parents=True, exist_ok=True)
    temporary = target.with_name(target.name + ".rebuilding")
    sha = hashlib.sha256()
    size = 0
    with temporary.open("wb") as out:
        for i in range(count):
            part = folder / f"{i:05d}"
            with part.open("rb") as source:
                while True:
                    chunk = source.read(1024 * 1024)
                    if not chunk:
                        break
                    sha.update(chunk)
                    size += len(chunk)
                    out.write(chunk)
    if sha.hexdigest() != digest or size != expected_size:
        temporary.unlink(missing_ok=True)
        raise RuntimeError(f"Reassembly failed integrity check: {target}")
    temporary.replace(target)
    print(f"Restored {target.relative_to(root)} ({size} bytes; SHA256 verified)")

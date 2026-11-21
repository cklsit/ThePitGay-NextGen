import subprocess
import sys

blob = subprocess.run(
    ["git", "cat-file", "-p", "2978adaf712d63cd3187b86a55a0e378f9f71382"],
    capture_output=True,
).stdout
crlf = blob.count(b"\r\n")
lf = blob.count(b"\n")
print(f"blob bytes={len(blob)} LF={lf} CRLF={crlf} pure_lf={lf - crlf}")

for path in ["docker/entrypoint.sh", "docker/verify-image-layout.sh", "Dockerfile", "docker-compose.yml"]:
    data = subprocess.run(["git", "cat-file", "-p", f"HEAD:{path}"], capture_output=True).stdout
    print(f"{path}: LF={data.count(b'\\n')} CRLF={data.count(b'\\r\\n')}")

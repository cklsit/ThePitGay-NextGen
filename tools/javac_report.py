"""直接调用 javac 编译 base，输出可读的错误列表。

Gradle 的日志会把 javac 的中文输出变成 '?'，这里绕过 Gradle：
用与 Gradle 相同的 classpath（依赖 + Kotlin 编译产物）+ Lombok 处理器直接编译，
错误信息以 GBK 解码后按文件汇总。
"""
import collections
import os
import re
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JDK = os.path.expanduser(r"~/.gradle/jdks/eclipse_adoptium-21-amd64-windows/jdk-21.0.12.1+1")
LOMBOK = os.path.expanduser(
    r"~/.gradle/caches/modules-2/files-2.1/org.projectlombok/lombok/1.18.34/"
    r"ec547ef414ab1d2c040118fb9c1c265ada63af14/lombok-1.18.34.jar")
CP_FILE = os.path.join(ROOT, "build", "cp-base.txt")
CP_OVERRIDE = {"core": os.path.join(ROOT, "build", "cp-core.txt")}


def collect_sources(module):
    sources = []
    for bas, _dirs, names in os.walk(os.path.join(ROOT, module, "src", "main", "java")):
        for name in names:
            if name.endswith(".java"):
                sources.append(os.path.join(bas, name))
    return sources


def main():
    module = sys.argv[1] if len(sys.argv) > 1 else "base"
    cp = open(CP_OVERRIDE.get(module, CP_FILE), encoding="utf-8").read().strip()
    kotlin_out = os.path.join(ROOT, module, "build", "classes", "kotlin", "main")
    full_cp = cp + os.pathsep + kotlin_out
    sources = collect_sources(module)
    out_dir = os.path.join(ROOT, "build", "cli-" + module)
    os.makedirs(out_dir, exist_ok=True)
    arg_file = os.path.join(ROOT, "build", module + "-sources.txt")
    with open(arg_file, "w", encoding="utf-8") as fh:
        fh.write("\n".join(sources))

    cmd = [
        os.path.join(JDK, "bin", "javac.exe"),
        "-nowarn", "-encoding", "UTF-8", "-Xmaxerrs", "5000",
        "-processorpath", LOMBOK,
        "-cp", full_cp,
        "-d", out_dir,
        "@" + arg_file,
    ]
    result = subprocess.run(cmd, capture_output=True)
    text = result.stderr.decode("gbk", errors="replace")
    lines = text.splitlines()

    errors = []
    current = None
    for line in lines:
        m = re.match(r"^(.*?\.java):(\d+): 错误: (.*)$", line)
        if m:
            current = [os.path.relpath(m.group(1), ROOT).replace("\\", "/"), int(m.group(2)), m.group(3)]
            errors.append(current)
        elif current is not None and line.startswith("  "):
            current.append(line.strip())
        else:
            current = None

    print(f"总共 {len(errors)} 条错误\n")
    per_file = collections.Counter(e[0] for e in errors)
    for path, count in per_file.most_common(30):
        print(f"{count:4d}  {path}")
    print()
    limit = int(os.environ.get("SHOW", "40"))
    for err in errors[:limit]:
        print(f"{err[0]}:{err[1]}: {err[2]}")
        for extra in err[3:4]:
            print("    " + extra)
    return 0 if not errors else 1


if __name__ == "__main__":
    sys.exit(main())

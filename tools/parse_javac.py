"""解析 javac 输出，按文件/符号汇总错误。"""
import collections
import re
import sys

ROOT = "C:\\Users\\Jonson\\Documents\\ThePitUltimate\\"


def main(path):
    lines = open(path, encoding="utf-8", errors="ignore").read().splitlines()
    per_file = collections.Counter()
    symbols = collections.Counter()
    for line in lines:
        m = re.match(r"^(.*?\.java):(\d+): error", line)
        if m:
            p = m.group(1).replace(ROOT, "").replace("\\", "/")
            per_file[p] += 1
        s = re.match(r"^\s+symbol:\s+(.*)$", line)
        if s:
            symbols[s.group(1).strip()] += 1
    print("=== 按文件 ===")
    for p, c in per_file.most_common(45):
        print(f"{c:5d}  {p}")
    print(f"files={len(per_file)} errors={sum(per_file.values())}")
    print("=== 符号 ===")
    for p, c in symbols.most_common(25):
        print(f"{c:5d}  {p}")


if __name__ == "__main__":
    main(sys.argv[1])

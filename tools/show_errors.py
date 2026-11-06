"""按文件筛选 javac 错误。用法: python tools/show_errors.py <log> <文件名片段> [条数]"""
import sys

SEP = "ThePitUltimate" + chr(92)


def main():
    log, target = sys.argv[1], sys.argv[2]
    limit = int(sys.argv[3]) if len(sys.argv) > 3 else 40
    lines = open(log, encoding="utf-8", errors="ignore").read().splitlines()
    out = []
    for i, line in enumerate(lines):
        if target in line and "error" in line:
            out.append(line.split(SEP)[-1].replace(chr(92), "/"))
            for j in range(1, 4):
                if i + j < len(lines) and lines[i + j].startswith("  "):
                    out.append("    " + lines[i + j].strip())
    print("\n".join(out[:limit]))


if __name__ == "__main__":
    main()

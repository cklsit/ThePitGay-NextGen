"""把 Kotlin 中 runTask* 的 lambda 显式标注为 Runnable。

Paper 1.21 的 BukkitScheduler 为每个 runTask* 增加了 Consumer<BukkitTask> 重载，
Kotlin 对 `runTask(plugin) { ... }` 的 SAM 转换因此产生歧义（Runnable vs Consumer）。
本脚本在不改变语义的前提下，为 lambda 加上显式 Runnable 类型。
"""
import os
import re
import sys

NAMES = ("runTaskAsynchronously", "runTaskLaterAsynchronously", "runTaskTimerAsynchronously",
         "runTaskTimer", "runTaskLater", "runTask")
CALL_RE = re.compile(r"\.(" + "|".join(NAMES) + r")\(")


def skip_string(text, i):
    quote = text[i]
    i += 1
    while i < len(text):
        if text[i] == "\\":
            i += 2
            continue
        if text[i] == quote:
            return i + 1
        i += 1
    return i


def skip_comment(text, i):
    if text.startswith("//", i):
        j = text.find("\n", i)
        return len(text) if j < 0 else j
    if text.startswith("/*", i):
        j = text.find("*/", i)
        return len(text) if j < 0 else j + 2
    return i


def match_pair(text, i, open_ch, close_ch):
    depth = 0
    while i < len(text):
        c = text[i]
        if c in "\"'":
            i = skip_string(text, i)
            continue
        if text.startswith("//", i) or text.startswith("/*", i):
            i = skip_comment(text, i)
            continue
        if c == open_ch:
            depth += 1
        elif c == close_ch:
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return -1


def transform(text):
    out = []
    pos = 0
    count = 0
    while True:
        m = CALL_RE.search(text, pos)
        if not m:
            out.append(text[pos:])
            break
        open_paren = m.end() - 1
        close_paren = match_pair(text, open_paren, "(", ")")
        if close_paren < 0:
            out.append(text[pos:])
            break
        args = text[open_paren + 1:close_paren]
        new_args = args
        changed = False
        # 定位参数区间内深度为 0 的 lambda 起始位置
        i = 0
        depth = 0
        while i < len(args):
            c = args[i]
            if c in "\"'":
                i = skip_string(args, i)
                continue
            if args.startswith("//", i) or args.startswith("/*", i):
                i = skip_comment(args, i)
                continue
            if c in "([{":
                if c == "{" and depth == 0:
                    rest = args[i + 1:]
                    if rest.strip() == "":
                        # 尾随 lambda：改写成 runTask(plugin, Runnable { ... })
                        body_close = match_pair(args, i, "{", "}")
                        if body_close > 0:
                            body = args[i:body_close + 1]
                            head = args[:i].rstrip()
                            tail = args[body_close + 1:]
                            new_args = head + ", Runnable " + body + tail
                            changed = True
                        break
                    else:
                        # 位置参数 lambda：直接加类型前缀
                        new_args = args[:i] + "Runnable " + args[i:]
                        changed = True
                        break
                depth += 1
            elif c in ")]}":
                depth -= 1
            i += 1
        if changed:
            count += 1
            out.append(text[pos:m.end()])
            out.append(new_args)
            out.append(")")
            pos = close_paren + 1
        else:
            # 尾随 lambda 位于右括号之后：runTask(plugin) { ... } -> runTask(plugin, Runnable { ... })
            j = close_paren + 1
            while j < len(text) and text[j] in " \t\r\n":
                j += 1
            if j < len(text) and text[j] == "{":
                body_close = match_pair(text, j, "{", "}")
                if body_close > 0:
                    count += 1
                    out.append(text[pos:close_paren])
                    out.append(", Runnable ")
                    out.append(text[j:body_close + 1])
                    out.append(")")
                    pos = body_close + 1
                    continue
            out.append(text[pos:close_paren + 1])
            pos = close_paren + 1
    return "".join(out), count


def main(paths):
    total = 0
    for path in paths:
        with open(path, encoding="utf-8") as fh:
            original = fh.read()
        if not CALL_RE.search(original):
            continue
        updated, n = transform(original)
        if n:
            with open(path, "w", encoding="utf-8", newline="") as fh:
                fh.write(updated)
            total += n
            print(f"{n:3d}  {path}")
    print(f"total {total}")


if __name__ == "__main__":
    main(sys.argv[1:])

#!/usr/bin/env bash
# 本地校验：模拟容器入口的关键逻辑（无 Docker 环境可用）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

JAR="$ROOT/core/build/libs/ThePitGay-V.jar"
if [ ! -f "$JAR" ]; then
    echo "缺少构建产物: $JAR（先执行 ./gradlew :core:shadowJar）" >&2
    exit 1
fi

echo "== 1. 从插件 jar 提取内置地图 =="
mkdir -p "$TMP/x"
unzip -o -q "$JAR" "maps/pit_arena.zip" -d "$TMP/x"
test -f "$TMP/x/maps/pit_arena.zip"
mkdir -p "$TMP/map"
unzip -o -q "$TMP/x/maps/pit_arena.zip" -d "$TMP/map"
test -d "$TMP/map/world/region"
COUNT=$(ls "$TMP/map/world/region" | wc -l)
echo "   region 文件数: $COUNT"

echo "== 2. 模拟数据目录初始化 =="
DATA="$TMP/data"
mkdir -p "$DATA/pit_arena"
cp -a "$TMP/map/world/." "$DATA/pit_arena/"
rm -f "$DATA/pit_arena/uid.dat" "$DATA/pit_arena/session.lock"
test -f "$DATA/pit_arena/level.dat"
test -d "$DATA/pit_arena/region"
echo "   level.dat 存在，uid.dat/session.lock 已移除"

echo "== 3. 校验入口脚本语法 =="
bash -n "$ROOT/docker/entrypoint.sh"
echo "   entrypoint.sh 语法通过"

echo "== 4. 校验关键配置模板 =="
grep -q 'online-mode=false' "$ROOT/docker/entrypoint.sh"
grep -q 'white-list=false' "$ROOT/docker/entrypoint.sh"
grep -q 'allow-nether=false' "$ROOT/docker/entrypoint.sh"
grep -q 'allow-end: false' "$ROOT/docker/entrypoint.sh"
echo "   offline / 白名单关闭 / 下界关闭 / 末地关闭 均已写入模板"

echo "全部校验通过"

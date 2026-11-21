#!/usr/bin/env bash
# 用假的 java 模拟容器首次启动，验证 entrypoint 生成的配置与解包结果。
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

mkdir -p "$TMP/bin" "$TMP/data" "$TMP/server/plugins-src"
cat > "$TMP/bin/java" <<'FAKE'
#!/usr/bin/env bash
echo "FAKE_JAVA_ARGS: $*"
exit 0
FAKE
chmod +x "$TMP/bin/java"

cp "$ROOT/core/build/libs/ThePitGay-V.jar" "$TMP/server/plugins-src/ThePitGay-V.jar"
echo "fake luckperms" > "$TMP/server/plugins-src/LuckPerms-Bukkit.jar"
cp "$ROOT/core/build/libs/ThePitGay-V.jar" "$TMP/server/paper.jar"

PATH="$TMP/bin:$PATH" \
SERVER_JAR="$TMP/server/paper.jar" \
PLUGIN_SRC="$TMP/server/plugins-src" \
DATA_DIR="$TMP/data" \
ONLINE_MODE=false \
WHITE_LIST=false \
ALLOW_NETHER=false \
MEMORY=1G \
MOTD="Test & | Motd" \
bash "$ROOT/docker/entrypoint.sh" > "$TMP/out.log" 2>&1

echo "== entrypoint 输出 =="
cat "$TMP/out.log"

echo "== 断言 =="
grep -q '^online-mode=false$' "$TMP/data/server.properties"
grep -q '^white-list=false$' "$TMP/data/server.properties"
grep -q '^allow-nether=false$' "$TMP/data/server.properties"
grep -q '^level-name=pit_arena$' "$TMP/data/server.properties"
grep -q "allow-end: false" "$TMP/data/bukkit.yml"
grep -q '^eula=true$' "$TMP/data/eula.txt"
test -f "$TMP/data/plugins/ThePitGay-V.jar"
test -f "$TMP/data/plugins/LuckPerms-Bukkit.jar"
test -f "$TMP/data/pit_arena/level.dat"
test -d "$TMP/data/pit_arena/region"
test ! -f "$TMP/data/pit_arena/uid.dat"
grep -q 'FAKE_JAVA_ARGS: .*-Xms1G -Xmx1G' "$TMP/out.log"
if grep -q 'Test & | Motd' "$TMP/data/server.properties"; then
    echo "MOTD 含特殊字符仍正确写入"
else
    echo "MOTD 写入异常" >&2
    grep '^motd=' "$TMP/data/server.properties" >&2
    exit 1
fi
echo "首次启动模拟校验通过"

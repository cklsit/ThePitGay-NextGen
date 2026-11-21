#!/usr/bin/env bash
# 幂等性校验：第二次启动不得覆盖已有世界、插件与配置。
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

mkdir -p "$TMP/bin" "$TMP/data" "$TMP/server/plugins-src"
cat > "$TMP/bin/java" <<'FAKE'
#!/usr/bin/env bash
exit 0
FAKE
chmod +x "$TMP/bin/java"
cp "$ROOT/core/build/libs/ThePitGay-V.jar" "$TMP/server/plugins-src/ThePitGay-V.jar"
echo "fake luckperms" > "$TMP/server/plugins-src/LuckPerms-Bukkit.jar"

run() {
    PATH="$TMP/bin:$PATH" \
    DATA_DIR="$TMP/data" \
    SERVER_JAR="$TMP/server/paper.jar" \
    PLUGIN_SRC="$TMP/server/plugins-src" \
    ONLINE_MODE=false WHITE_LIST=false ALLOW_NETHER=false MEMORY=1G \
    bash "$ROOT/docker/entrypoint.sh" > /dev/null 2>&1
}
run

# 模拟玩家/管理员改动
echo "custom-motd=1" >> "$TMP/data/server.properties"
MARK="marker-$(date +%s)"
echo "$MARK" > "$TMP/data/marker.txt"
rm -f "$TMP/data/plugins/ThePitGay-V.jar"
echo "old-plugin" > "$TMP/data/plugins/ThePitGay-V.jar"

run
grep -q 'custom-motd=1' "$TMP/data/server.properties"
test -f "$TMP/data/marker.txt"
grep -q 'old-plugin' "$TMP/data/plugins/ThePitGay-V.jar"
echo "第二次启动未覆盖已有数据（配置/世界/插件均保留）"

# FORCE_PLUGIN_UPDATE=true 时应覆盖插件
PATH="$TMP/bin:$PATH" DATA_DIR="$TMP/data" SERVER_JAR="$TMP/server/paper.jar" \
PLUGIN_SRC="$TMP/server/plugins-src" FORCE_PLUGIN_UPDATE=true MEMORY=1G \
bash "$ROOT/docker/entrypoint.sh" > /dev/null 2>&1
if grep -q 'old-plugin' "$TMP/data/plugins/ThePitGay-V.jar"; then
    echo "FORCE_PLUGIN_UPDATE 未覆盖插件" >&2
    exit 1
fi
echo "FORCE_PLUGIN_UPDATE=true 时插件被更新"
echo "幂等性校验通过"

import re

# ---------- 1) ThePit：合并 onLoad，移除运行时依赖下载（改为 shade 内置） ----------
p = 'base/src/main/java/cn/charlotte/pit/ThePit.java'
t = open(p, encoding='utf-8').read()

# 删除我先前新增的 onLoad（保留后面的 onEnable）
new_onload = """    public void onLoad() {
        // PacketEvents 需在 onLoad 阶段完成通道注入；随插件 shade 内置，无需服务端另装
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings().checkForUpdates(false).bStats(false);
        PacketEvents.getAPI().load();
    }

"""
t = t.replace(new_onload, "")

# 定位原有 onLoad 并整体替换
start = t.index("    public final void onLoad() {")
i = t.index("{", start)
depth = 0
end = i
while end < len(t):
    if t[end] == "{":
        depth += 1
    elif t[end] == "}":
        depth -= 1
        if depth == 0:
            break
    end += 1

replacement = """    public final void onLoad() {
        instance = this;
        // 单 jar 部署：依赖已随 shadowJar 内置，不再在启动阶段联网下载
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings().checkForUpdates(false).bStats(false);
        PacketEvents.getAPI().load();
    }"""
t = t[:start] + replacement + t[end + 1:]
open(p, 'w', encoding='utf-8').write(t)

# ---------- 2) BotManager：限定 Bukkit Command 类型 ----------
p = 'base/src/main/java/cn/charlotte/pit/bot/BotManager.java'
t = open(p, encoding='utf-8').read()
t = t.replace("import org.bukkit.command.Command;\n", "")
t = t.replace("public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {",
              "public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {")
t = t.replace("public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {",
              "public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {")
open(p, 'w', encoding='utf-8').write(t)

print('ok')

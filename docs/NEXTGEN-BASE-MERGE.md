# NextGen 底座融合说明

> 底座：`ImMengXin0/ThePitGay-NextGen`（`core` 单模块，Paper 1.21.8）。
> 不再携带 `ShanguanLinG/ThePitUltimate` 的上游代码；只保留自建功能与 `ThePitAddon-MythicLand`。

## 保留内容

| 来源 | 内容 |
|---|---|
| NextGen | NPC/机器人（npclib，PacketEvents 发包）、事件/附魔/菜单/商店等全部核心逻辑 |
| 自建 | SQLite + MongoDB 双后端（`storage.type` 默认 sqlite）、`/botspawn` `/botlist` `/botclear`、神话之锤附魔（112 条 + 效果处理器 + `HammerBudget`）、`SeasonalOpenService` 锤子季节开放判定、`HammerOnly`/`IMythicHammer` |
| ThePitAddon-MythicLand | `cn.irina.thepitaddon.*`：78 条附魔、玩家/管理命令（含 `/lj` 垃圾箱）、过滤器、维护任务，由 `Loader.kt` 启动 |

## 已移除

旧主线恢复的类：`net/mizukilab/pit/runnable` 旧任务（Bounty/Protect/特殊玩家等）、旧全息监听与运行时、
`CloudUpdate`、`map/MapHandler`、`npc/type/custom` 的 AfkPool/SpacetimeMerchant、`LegacyColor`、
`classloaders`、以及旧 `PlayerListener`/`DataListener` 等与 NextGen 重复的实现。

## 1.21.8 相关修复

- **NPC 点击**：npclib 原用 ProtocolLib 的 `USE_ENTITY` 异步监听，ProtocolLib 5.4.0 在 Paper 上抛
  `FieldAccessException`，所有数据包 NPC 点不动。现改由插件内置 PacketEvents 监听 `INTERACT_ENTITY`，
  ProtocolLib 仅在 PacketEvents 不可用时兜底。
- **全息更新**：`PacketHologramRunnable` 由异步改主线程（内部要调用 `showEntity`，异步会触发
  `Asynchronous player tracker update`）。
- **药水加成事件**：`net.mizukilab.pit.listener.PacketListener` 改用 PacketEvents 解析 `ENTITY_EFFECT`，
  取代越界的 ProtocolLib 结构修改器。

## 构建与部署

```powershell
.\gradlew.bat :core:shadowJar
# 产物 core/build/libs/ThePitGay-V.jar
```

- Paper 1.21.8；`LuckPerms`、`PlayerPoints` 建议装；`ProtocolLib` 目前仅 `NpcDeathAnimationListener` 与
  npclib 兜底路径使用，建议 5.5.0-SNAPSHOT
- 存储：`storage.type` = `sqlite` / `mongodb`
- 资源：`addon_config.yml`、`addon_filter.yml`、23 个 `.nbs`（含 DJ 附魔用音乐）

## 实测

- 本机与线上 NAS：`Storage backend: SQLite (connected)` → 核心 272 附魔 → addon 注册后 **314** →
  `Powered by Irina, ShanguanLinG, MIYU` → `Done`，日志 0 ERROR
- 本机 MongoDB：`storage.type: mongodb` → `Storage backend: MongoDB (connected)`，同一 jar
- 机器人命令与 NPC 点击分发（`NPCInteractEvent` 触发）在冒烟环境验证通过

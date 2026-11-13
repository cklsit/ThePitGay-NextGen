# ThePitGay-NextGen

ThePitGay-NextGen 的 Paper 1.21.8 维护分支。以 NextGen 为底座，整合 ThePitUltimate 的玩法与 ThePitAddon 扩展，修复 1.21.8 下的大量兼容问题，并内置地图与配套配置，开箱即用。

- 上游：<https://github.com/ImMengXin0/ThePitGay-NextGen>
- 本仓库：<https://github.com/cklsit/ThePitGay-NextGen>
- 发行包：[Releases](https://github.com/cklsit/ThePitGay-NextGen/releases) 中的 `ThePitGay-V.jar`（构建产物路径 `core/build/libs/ThePitGay-V.jar`）
- 最新版本：v5.0.0（Paper 1.21.8）

## 环境要求

| 项目 | 要求 |
| --- | --- |
| 服务端 | Paper 1.21.8 |
| Java | 21 |
| 必需插件 | LuckPerms（`depend`） |
| 可选插件 | ProtocolLib、PlayerPoints、PlaceholderAPI、FastAsyncWorldEdit / WorldEdit、NPCView |
| 存储 | SQLite（默认）或 MongoDB，`config.yml` 中 `storage.type` 切换 |

## 快速开始

1. 把 `ThePitGay-V.jar` 放进 `plugins/`，启动服务器。
2. 首次启动会自动解压并加载内置地图 `pit_arena`，地图配置 `plugins/ThePitUltimate/worlds/pit_arena.yml` 与地图坐标一一对应。
3. 修改配置或地图后执行完整重启；插件停服时会把内存中的世界配置写回 `worlds/*.yml`，请先停服再改文件。

## 内置地图

- 地图打包在 jar 内：`maps/pit_arena.zip`（约 8.8 MB 压缩）。
- 开服流程：缺少配置则安装 `worlds/pit_arena.yml` → 缺少世界目录则解压到服务器根目录 `pit_arena/` → 删除重复的 `uid.dat` / `session.lock` → 加载世界并切换到该地图。
- 迁移或更换地图时可用 `/pit map` 系列命令；地图配置里记录了出生点、NPC、箱子等坐标。
- 内置地图作为默认世界：`server.properties` 的 `level-name` 需设为 `pit_arena`（本分支部署时已切换），这样玩家与 NPC 在同一世界。
- 如果 `worlds/` 下还有指向未加载世界（例如旧 `world`）的地图配置，请停服后把对应 `*.yml` 改名为 `*.yml.disabled`，否则启动时会出现 `unknown world` 警告。

## 本分支主要改动

- NPC 交互改用 PacketEvents，修复 ProtocolLib 5.4.0 在 1.21.8 上异步包处理失效导致 NPC 无法点击的问题。
- 神话之井：动画帧正常刷新，点击附魔后自动完成并展示结果，无需按 ESC 重新打开。
- 确认菜单：取消按钮固定红色，确认按钮倒计时结束后可正常点击。
- 下水道：鱼 NPC 与区域坐标可配置；箱子点位支持配置定点，右键直接发奖励且不会再打开箱子界面；玩家数据中无效的床/重生锚绑定会被清理。
- TAB：超级连封造极（uber_steak_plus）激活时按紫色名称 + 当前连杀数显示。
- DJ 附魔：修复音量为 0 导致无声、乐器音色全部回落到竖琴的问题。
- 已移除旧版神话之锤相关功能与 `/lj` 命令。
- 兼容内置的 ThePitAddon（`cn.irina.thepitaddon`）、NPCView；NPC 使用内置 MythicNPCLib，不依赖 Citizens。

## 构建

```bash
# JDK 21
./gradlew :core:shadowJar
# 产物：core/build/libs/ThePitGay-V.jar
```

Windows：`gradlew.bat :core:shadowJar`。

仓库内 `libs/`、`packLib/` 已包含构建所需的本地依赖 jar，克隆仓库即可直接构建。

## GitHub Actions

- `.github/workflows/build.yml`：push 到 `main`、PR 或手动触发时构建 jar，并上传为 Actions 产物（Artifact）。
- 推送 `v*` 标签（如 `v5.0.0`）时，构建完成后自动创建 GitHub Release 并附带 `ThePitGay-V.jar`。

## 致谢

基于 ThePitGay / ThePitUltimate / ThePitAddon 及其作者的工作：MIYU、ShanguanLinG、Irina、EmptyIrony、Araykal 等。上游代码与玩法设定版权归原作者所有。

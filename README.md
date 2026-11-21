# ThePitGay-NextGen

ThePitGay-NextGen 的 Paper 1.21.8 维护分支。以 NextGen 为底座，整合 ThePitUltimate 的玩法与 ThePitAddon 扩展，修复 1.21.8 下的大量兼容问题，并内置地图与配套配置，开箱即用，支持 Docker 一键部署。

- 上游：<https://github.com/ImMengXin0/ThePitGay-NextGen>
- 本仓库：<https://github.com/cklsit/ThePitGay-NextGen>
- 发行包：[Releases](https://github.com/cklsit/ThePitGay-NextGen/releases) 中的 `ThePitGay-V.jar`（构建产物路径 `core/build/libs/ThePitGay-V.jar`）
- 最新版本：[v5.0.5](https://github.com/cklsit/ThePitGay-NextGen/releases/tag/v5.0.5)（Paper 1.21.8）
- 容器镜像：`ghcr.io/cklsit/thepitgay-nextgen:latest`

## 环境要求

| 项目 | 要求 |
| --- | --- |
| 服务端 | Paper 1.21.8 |
| Java | 21 |
| 必需插件 | LuckPerms（`depend`） |
| 可选插件 | ProtocolLib、PlayerPoints、PlaceholderAPI、FastAsyncWorldEdit / WorldEdit、NPCView |
| 存储 | SQLite（默认）或 MongoDB，`config.yml` 中 `storage.type` 切换 |

## Docker 一键部署（推荐）

镜像内置 Paper 1.21.8、ThePitGay-NextGen 插件、LuckPerms 与内置地图 `pit_arena`，不需要手动准备任何文件。

### docker compose（最简单）

```yaml
# docker-compose.yml
services:
  thepit:
    image: ghcr.io/cklsit/thepitgay-nextgen:latest
    container_name: thepit
    restart: unless-stopped
    ports:
      - "25565:25565"
    environment:
      MEMORY: "4G"          # -Xms/-Xmx 同值
      ONLINE_MODE: "false"  # 默认关闭正版验证
      WHITE_LIST: "false"   # 默认关闭白名单
      ALLOW_NETHER: "false" # 默认关闭下界（末地固定关闭）
      LEVEL_NAME: "pit_arena"
      MAX_PLAYERS: "100"
    volumes:
      - thepit-data:/data
    stdin_open: true
    tty: true
    stop_grace_period: 60s

volumes:
  thepit-data:
```

```bash
docker compose up -d          # 启动
docker compose logs -f        # 看日志
docker compose down           # 停止（世界保留在卷 thepit-data 中）
```

仓库根目录已提供可直接使用的 `docker-compose.yml`。

### docker run

```bash
docker run -d --name thepit \
  -p 25565:25565 \
  -e MEMORY=4G \
  -e ONLINE_MODE=false \
  -e WHITE_LIST=false \
  -e ALLOW_NETHER=false \
  -v thepit-data:/data \
  --restart unless-stopped \
  ghcr.io/cklsit/thepitgay-nextgen:latest
```

### 环境变量

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `MEMORY` | `4G` | JVM 堆大小（`-Xms` 与 `-Xmx`） |
| `ONLINE_MODE` | `false` | 正版验证，`false` 为离线模式，客户端可直接连 |
| `WHITE_LIST` | `false` | 白名单 |
| `ALLOW_NETHER` | `false` | 下界；`false` 时同时由插件拦截传送门 |
| `LEVEL_NAME` | `pit_arena` | 主世界名，首次启动自动解包内置地图 |
| `SERVER_PORT` | `25565` | 服务端口 |
| `MAX_PLAYERS` | `100` | 最大人数 |
| `MOTD` | `ThePitGay-NextGen (Paper 1.21.8)` | 服务器列表描述 |
| `FORCE_PLUGIN_UPDATE` | `false` | 设为 `true` 时用镜像内新插件覆盖 `/data/plugins` 旧文件（升级用，一次即可） |

- 末地在镜像里固定关闭（`bukkit.yml` 的 `allow-end: false`），插件端还会拦截末地传送门与跨维度传送。
- 所有可变数据都在 `/data` 卷：世界、玩家数据、插件配置。删卷即重置。
- 升级镜像后若插件没更新，把 `FORCE_PLUGIN_UPDATE=true` 启动一次再改回 `false`。

### 自行构建镜像

```bash
git clone https://github.com/cklsit/ThePitGay-NextGen.git
cd ThePitGay-NextGen
docker build -t thepitgay-nextgen:local .
# 或：docker compose build && docker compose up -d
```

构建为两阶段：先编译 `ThePitGay-V.jar`（需要 Paper 1.21.8 NMS 类路径，已在 Dockerfile 内自动下载并校验 sha256），再组装运行时镜像；LuckPerms 与 Paper 均带哈希校验。

## 手动部署（不使用 Docker）

1. 把 `ThePitGay-V.jar` 与 LuckPerms 放进 `plugins/`，启动 Paper 1.21.8。
2. 首次启动会自动解压并加载内置地图 `pit_arena`，地图配置 `plugins/ThePitUltimate/worlds/pit_arena.yml` 与地图坐标一一对应。
3. `server.properties` 建议：`level-name=pit_arena`、`online-mode=false`、`white-list=false`、`allow-nether=false`；`bukkit.yml` 设 `allow-end: false`。
4. 修改配置或地图后执行完整重启；插件停服时会把内存中的世界配置写回 `worlds/*.yml`，请先停服再改文件。

## 内置地图

- 地图打包在 jar 内：`maps/pit_arena.zip`（约 8.8 MB 压缩）。
- 插件开服流程：缺少配置则安装 `worlds/pit_arena.yml` → 缺少世界目录则解压到服务器根目录 `pit_arena/` → 删除重复的 `uid.dat` / `session.lock` → 加载世界并切换到该地图。
- Docker 镜像在此基础上还会在首次启动时把地图直接解到 `/data/pit_arena`，避免 Paper 先自行生成空地图。
- 迁移或更换地图时可用 `/pit map` 系列命令；地图配置里记录了出生点、NPC、箱子等坐标。
- 内置地图作为默认世界：`level-name` 需设为 `pit_arena`（镜像已默认）。
- 如果 `worlds/` 下还有指向未加载世界（例如旧 `world`）的地图配置，请停服后把对应 `*.yml` 改名为 `*.yml.disabled`，否则启动时会出现 `unknown world` 警告。

## 本分支主要改动

- NPC 交互改用 PacketEvents，修复 ProtocolLib 5.4.0 在 1.21.8 上异步包处理失效导致 NPC 无法点击的问题。
- 神话之井：动画帧正常刷新，点击附魔后自动完成并展示结果，无需按 ESC 重新打开。
- 确认菜单：取消按钮固定红色，确认按钮倒计时结束后可正常点击。
- 下水道：鱼 NPC 与区域坐标可配置；箱子点位支持配置定点，右键直接发奖励且不会再打开箱子界面；玩家数据中无效的床/重生锚绑定会被清理。
- 红包：改用聊天栏输入金额（虚拟告示牌在 1.21.8 会让客户端闪退），并按距离加权分配金额，单人可拿全部。
- 暴君：激活连杀后无论被玩家击杀、环境死亡还是 `/kill`，重生时都会补发权杖。
- TAB：超级登封造极（uber_steak_plus）激活时按紫色名称 + 当前连杀数显示。
- DJ 附魔：修复音量为 0 导致无声、乐器音色全部回落到竖琴的问题。
- 维度：下界与末地关闭，传送门与跨维度传送均被拦截，已加载的下界/末地世界会被卸载。
- 已移除旧版神话之锤相关功能与 `/lj` 命令。
- 兼容内置的 ThePitAddon（`cn.irina.thepitaddon`）、NPCView；NPC 使用内置 MythicNPCLib，不依赖 Citizens。

## 构建

```bash
# JDK 21
./gradlew :core:shadowJar
# 产物：core/build/libs/ThePitGay-V.jar
```

Windows：`gradlew.bat :core:shadowJar`。

仓库内 `libs/`、`vendor/` 已包含构建所需的本地依赖，克隆仓库即可直接构建。

## GitHub Actions

- `.github/workflows/build.yml`：push 到 `main`、PR 或手动触发时构建 jar，并上传为 Actions 产物（Artifact）；推送 `v*` 标签时自动创建 GitHub Release 并附带 `ThePitGay-V.jar`。
- `.github/workflows/docker.yml`：push 到 `main`、推送 `v*` 标签或手动触发时构建并推送镜像到 GHCR（`ghcr.io/cklsit/thepitgay-nextgen`），标签包含 `latest`、`sha-<short>` 与版本标签。

首次使用 GHCR 镜像时，如仓库为私有需 `docker login ghcr.io`；公有镜像可直接拉取。

## 致谢

基于 ThePitGay / ThePitUltimate / ThePitAddon 及其作者的工作：MIYU、ShanguanLinG、Irina、EmptyIrony、Araykal 等。上游代码与玩法设定版权归原作者所有。

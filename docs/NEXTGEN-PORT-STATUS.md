# NextGen 1.21.8 迁移进度

> 本文件是跨会话交接文档。任何人（或 AI）接手时先读这份。

## 目标

把 `ImMengXin0/ThePitGay-NextGen`（1.8.8 代码库）迁移到 Paper 1.21.8，
合入本仓库后作为主线。

## 分支

- `main` @ `f09c15ce`：**主线，可编译可上线**，服务器运行的就是它，全程未受影响
- `feat/nextgen-1.21.8`：迁移工作分支（NextGen 代码基线 + 迁移改动）

## 当前状态

| 项 | 值 |
|---|---|
| Kotlin 编译 | **0 错误** |
| Bot 模块（`cn.mythicland.npclib.bot.*` + `BotManager`） | **0 错误**（独立 javac 验证：22 源文件 → 65 个 class） |
| Java 全量编译 | javac 自报 **1911 条**（只显示前 100 条），全部在 Bot 模块之外 |
| 可打包 | 否（Java 未过） |

Bot 模块的独立验证命令见本文末「验证方式」，改 Bot 相关代码后必须跑一遍。

## 本轮完成：Bot 模块重写（1.8 NMS → 1.21.8 真实登录）

### BotEntity（`bot/v1_8_R3/BotEntity.java`，379 → 约 250 行）

按既定方案 **A1-a-1**：走 `placeNewPlayer` 真实登录路径。

- `extends ServerPlayer`，构造 `ServerPlayer(MinecraftServer.getServer(), ServerLevel, GameProfile, ClientInformation.createDefault())`
- `spawn()`：建 `BotConnection`（`extends net.minecraft.network.Connection`），`new EmbeddedChannel(connection)` 挂通道 →
  `configurePacketHandler(pipeline)` → `PlayerList.placeNewPlayer(connection, this, CommonListenerCookie.createInitial(profile, false))`
- `BotConnection` 覆盖 `send(Packet)`、`send(Packet, ChannelFutureListener)` 为空实现：假连接不需要收包，
  不覆盖会让出站包堆在 EmbeddedChannel 的出站队列里无界增长
- `tick()` / `doTick()` 置空，由 Bot 模块每 tick 调 `tickState()`（内部 `super.tick()`）与 `step()` 驱动
- `step()`：`setSpeed` + `xxa`/`zza`/`jumping` + `jumpFromGround()` + `travel(Vec3)`
- `destroy()`：`PlayerList.remove(this)` → `discard()` → 关闭 channel；机器人是真实登录玩家，
  只 `discard()` 会把它留在在线列表/统计里
- 删除的 1.8 反射：`World.u`（worldAccesses）、`PlayerList.o`（统计缓存）、`PlayerManager.gamemode`、
  `IWorldAccess` 匿名类、`Packet` 假发送器

### BotVisibility（545 → 约 320 行）

机器人现在是服务端正常追踪的真实玩家，**发包层整体删除**，可见性落到 Bukkit API：

- 保留原有判定：世界/区块/眼高上限/距离/视线点积/挂机（`idleSeconds`）/战斗豁免，
  以及 `visibilityCheckTicks` 节流、`spawnDelayTicks` 延迟、`maxRevealsPerTick` 揭示预算
- 应用层：`player.showPlayer(plugin, bot)` / `hidePlayer(plugin, bot)`
- 删除：`Movement`/`Update`/`Display.equipment`/`PacketPlayOut*`/`ScoreboardTeam`/名字板实体（改用原版名字板：
  `BotEntity` 里已 `setDisplayName` + `setPlayerListName`）
- 不再使用的 `muteCombatSounds` / `restoreIdle` / `onIdleHidden` / `hasNameplate` / `equipment` 一并移除（全仓库无调用）

### 配套改动

| 文件 | 改动 |
|---|---|
| `BotManager.java` | `new BotVisibility(plugin)`；`BotEntity#getUniqueID()` → `getUUID()` |
| `BotMotion.java` | 直写 `entity.yaw/pitch` → `getYRot()/setYRot()/getXRot()/setXRot()` |
| `vendor/.../framework/bukkit/world/GridPathfinder.java` | `World#getBlockTypeIdAt` → `getBlockAt(x,y,z).getType()`；`Material.CARPET` → `Tag.CARPETS` |
| `vendor/.../framework/bukkit/world/BlockMaterials.java` | 同上 |
| `core/build.gradle.kts` | 加入 NMS 编译类路径、MythicFramework 源码目录、`packLib` 依赖 |

## 构建环境：NMS 编译类路径（新会话第一步）

`paper-api` **不含** NMS 与 CraftBukkit，Bot（以及剩余 NMS 文件）需要真实服务端 jar 才能编译。

1. 用 Paper 1.21.8 的 paperclip jar 解包（只需一次，产物在 `.gradle/` 下，已 gitignore）：

   ```powershell
   java "-Dpaperclip.patchonly=true" -jar paper-1.21.8-<build>.jar
   # 产出 versions/1.21.8/paper-1.21.8.jar 与 libraries/
   ```

2. 把产物放到 `<仓库>/.gradle/paper-nms/`（结构保持 `versions/…`、`libraries/…`）。
   可用 `-PpaperNmsRoot=<目录>` 覆盖。

3. `core/build.gradle.kts` 会把该目录下 `versions/**/*.jar`、`libraries/**/*.jar` 全部挂到 `compileOnly`。
   目录不存在时构建不报错，但 Bot 模块会以「package net.minecraft.server does not exist」失败。

MythicFramework 未作为模块参与构建，`core` 通过 `sourceSets` 直接编译其中四个包：
`framework/math`、`framework/path`、`framework/task`、`framework/bukkit/world`。

## 1.21.8 实测签名（本轮用 javap 核对过）

```java
ServerPlayer(MinecraftServer, ServerLevel, GameProfile, ClientInformation)
ClientInformation.createDefault()
PlayerList.placeNewPlayer(Connection, ServerPlayer, CommonListenerCookie)
CommonListenerCookie.createInitial(GameProfile, boolean)
Connection(PacketFlow)                       // 字段 channel / address 都是 public
new EmbeddedChannel(connection)              // 构造即触发 channelActive，自动填好 channel/address
LivingEntity#setSpeed(float) / getSpeed()
LivingEntity#travel(Vec3)                    // 输入向量 (xxa, yya, zza)
LivingEntity#jumpFromGround() / setJumping(boolean) / hurtTime
Entity#invulnerableTime / getId() / getUUID() / setPos(double,double,double)
Entity#setYRot / setXRot / getYRot / getXRot / setYHeadRot / setYBodyRot
Entity#teleportSetPosition(PositionMoveRotation, Set<Relative>)
PositionMoveRotation(Vec3, Vec3, float, float)
Attributes.MOVEMENT_SPEED                    // Holder<Attribute>，用 getAttribute(...) 取 AttributeInstance
Player#attack(Entity) / resetAttackStrengthTicker() / getGameProfile() / getDisplayName()
```

## 踩坑记录（必须保留）

1. **`Connection` 被遮蔽**：`LivingEntity` 实现 `WaypointTransmitter`，它有一个嵌套接口 `Connection`。
   在 `Entity` 子类体内写 `extends Connection` 会被解析成那个接口，javac 报
   `no interface expected here`。必须写全限定名 `extends net.minecraft.network.Connection`。
2. **`Nameable#getName()` 返回 adventure `Component`**，1.8 的 `public String getName()` 覆盖会直接编译失败；
   要改名字请覆盖 `getDisplayName()` 或走 Bukkit `setDisplayName`。
3. **`PlayerList.getPlayers()` 返回内部 list**（不是拷贝），可原地 `remove`。
4. **`ServerPlayer.doTick()` 依赖 `connection` 字段**，假连接没有真 channel 时必须把 `tick()/doTick()` 置空。
5. **`paper-api` 没有 NMS**；`compileOnly(fileTree("libs"))` 只扫 `core/libs`，仓库根 `libs/` 要单独声明。
6. **`packLib/magic-license-1.1.3.jar` 提供 `nya.Skip`**，不挂到编译类路径会连带放大几百条错误
   （包缺失时 javac 会跳过后续处理，错误数被放大，别被总量吓到）。
7. 编译输出按 Gradle 控制台宽度折行，日志里路径会被从中间截断；统计错误要用「按 `C:\` 开头归并」的解析，别直接正则匹配整行。
8. 改文件前先 `grep -c` 校验；批量替换必须带安全网（替换后重新编译，错误数上升就回滚）；
   不要在同一个文件里并发发多个编辑；`s.find()` 可能返回 -1，切片前必须断言。

## 剩余错误（首批可见，均在 Bot 模块之外）

```
PublicUtil 6 · FishingHandler 6 · ItemBuilder 5 · SignGui 5 · PlayerMarco 3 · ActionBarUtil 3
PetFactory 3 · ProtectListener 2 · BatUtil 2 · CombatListener 2 · ExpireListener 2
Scoreboard 1 · PlayerMoveHandler 1 · PlayerProfile 1 · EventFactory 1 · HemorrhageDeBuff 1 · HitTickGuard 1 · …（共 1911 条，级联放大）
```

主要根因：`net.minecraft.server.v1_8_R3` / `org.bukkit.craftbukkit.v1_8_R3` 残留、1.8 已移除的 Bukkit API
（`Block#getData`、`World#spawnFallingBlock(Material,byte)`、`Sound.BURP`、`EntityDismountEvent` 等）、
`net.mizukilab.pit.enchantment.runnable.AnimationRunnable` 缺文件。
`npclib` 的 `nms/v1_8_R3/*` 包（NPC 发包层，非 Bot）同样仍是 1.8 实现，共 7 个文件。

## 下一步（按顺序）

1. 逐个清掉 `npclib/nms/v1_8_R3/*`（NPC 发包层，7 个文件）——与 Bot 同源，可用同一套 javap 结论
2. `ItemBuilder` / `SignGui` / `PetFactory` / `PlayerMoveHandler` / `FishingHandler` 等 NMS 与 1.8 API 文件
3. 全量编译通过 → `shadowJar` 出包
4. 部署测试服验证机器人：出现 / 可被击杀 / 加连杀 / 重生 / 进服不同步崩服
   - 重点观察：keep-alive 超时（假连接不回包）、`PlayerJoinEvent`/`PlayerQuitEvent` 副作用、
     `Bukkit.getOnlinePlayers()` 现在**包含**机器人（与原 1.8 行为不同，接手者须知）

## 验证方式

Bot 模块独立编译（不依赖其它模块是否已修好，改完 Bot 必跑）：

```powershell
# 需要 .gradle/paper-nms 已就绪
.\tools\verify-bot.ps1        # javac 单独编译 bot 包 + BotManager，输出 class 数（本次：22 源文件 → 65 class）
```

全量编译：

```powershell
$env:JAVA_TOOL_OPTIONS='-Duser.language=en -Duser.country=US'
.\gradlew.bat :core:compileJava --console=plain --no-daemon
```

## 相关文档

- `docs/BUILDING.md`：构建说明（含 NMS 类路径）
- `docs/p2-2-replaced-files.txt`、`docs/p2-3-replaced-files.txt`：批量替换为迁移版的 42 个文件
- `docs/java-nms-files.txt`：仍含 `v1_8_R3` 的 Java 文件清单

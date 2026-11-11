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
| Java 全量编译（`:core:compileJava`） | **0 错误** ✅ |
| `:core:shadowJar` | **成功**，产出 `core/build/libs/ThePitGay-V.jar`（约 44MB，单 jar 自包含） |
| 本机 Paper 1.21.8 冒烟 | **已跑到数据库连接**：插件加载、PacketEvents 初始化、配置/世界配置、LuckPerms/ProtocolLib 集成均通过；本机无 MongoDB，卡在建库 |
| 可上线 | **待有 MongoDB 的测试服验证** |

验证命令：`tools/compile-check.ps1`（javac 直编，0 错误）+ `.\gradlew.bat :core:shadowJar`。

### 本机冒烟发现并修掉的 9 个真实部署阻塞

1. **`api-version: 1.8`** → Paper 1.21.8 拒绝加载，改 `'1.21'`。
2. **`packLib/magic-license-1.1.3.jar` 会让 Paper 的 PluginRemapper 崩溃**
   （`Duplicate key a` —— 混淆类同名字段）。改用内置 `nya.Skip` 注解，不再打包授权库。
3. **`ThePit.onLoad` 的 ImagineBreaker 依赖下载**在重定位后 URL 失效（`releasesnet/...`），
   启动时逐个库超时；改为单 jar 自带依赖（对齐 main 的做法）。
4. **gson 未打包但被重定位**：PacketEvents 初始化 `NoClassDefFoundError`；`com.google.code.gson` 改 `implementation`。
5. **kotlin 运行库被 shadowJar 排除**：服务端不提供 `kotlin/*`；停止排除，`kotlin` 3035 个类进包。
6. **fastutil / mongodb-driver-sync / mongojack / jedis / commons-lang 只在编译期**：
   运行到 `loadDatabase` 才 `NoClassDefFoundError`；全部改为 `implementation` 打包。
7. **`NametagThread` 继承 `BukkitRunnable` 却传给 `Bukkit#getScheduler().runTaskTimer(...)`**：
   Paper 1.21 抛 `UnsupportedOperationException`，插件 enable 直接失败；改用 `this.runTaskTimer(...)`。
8. **`ClearRunnable` 从未实例化**：`ItemSpawnEvent`/保护逻辑取 `getClearRunnable()` 得到 null 刷屏 NPE；
   在 `loadGame()` 里启动（对齐 main）。
9. **`PitHook.gitVersion` 硬编码旧版本并反射覆盖 plugin description**：日志显示 v4.4.9 与真实 jar 版本不符；
   改为读取当前插件版本。

### 冒烟环境与前置

- 本机测试服目录（仓库外）：`Documents/pit-testserver/`，Paper 1.21.8 + LuckPerms 5.5.91 +
  **ProtocolLib 5.5.0-SNAPSHOT（5.3.0 在 1.21.8 上加载失败，必须用 CI nightly）** + FAWE 2.16.2 + 本地 MongoDB 8.0。
- 插件首次启动会生成 `plugins/ThePitUltimate/worlds/world.yml` 并主动关服（设计如此），**第二次启动**才会继续。
- 启动链路需要 **MongoDB**（`service.mongodb.*`，默认 127.0.0.1:27017、账号需在 `thePit` 库里）。
- 冒烟脚本：`Documents/pit-testserver/run-smoke.ps1`（先杀残留进程→复制新 jar→改 plugin.yml→清 `.paper-remapped`
  →启动→等 `Done (`→空跑 N 秒→stop）。

### 冒烟结论（最近一次：Bot 实机验证）

`Enabling ThePitUltimate v4.4.11-20261028` → `Loaded configuration/mongodb/NPCFactory/CustomEntityNPCFactory/Events`
→ `Done (~30s)` → **空跑 90 秒 0 异常** → 正常 `Disabling v4.4.11-20261028`。

Bot（`NPCLib#createFighterRobot`，用临时冒烟插件调用）——四项验收全部在本机 Paper 1.21.8 上跑通：
- **出现**：世界实体 +1，Bukkit 实体是 `CraftPlayer`，进入 `Bukkit.getOnlinePlayers()`
- **可击杀**：`Player#damage(...)` → 血量 0 → `dead` → 实体移除（插件的限伤逻辑会削峰，
  冒烟脚本先把血降到 1 再补刀）
- **加连杀**：另一只 Bot 作为攻击者击杀后，击杀者档案 `kills` / `highestStreaks` 各 +1
  （原先一次击杀 +2：NPC 分支在结算后立刻清死亡守卫，同 tick 内的二次死亡触发重复结算；
  改为下一 tick 再清守卫后已修正，冒烟复测 kills=1 / streaks=1）
- **重生**：带 `respawnDelay(40)` 的 Bot 死亡后 2 秒以新 UUID 重新上线，在线数恢复

仍未验证：真人客户端看到的 NPC 外观/点击、道具与附魔台界面、事件刷地形、机器人战斗 AI 表现。

物品 PDC 数据层（同一冒烟插件调用）：
- `MythicSwordItem#toItemStack()` → `GOLDEN_SWORD`
- `Utils.getMythicItem(stack)` → 还原成 `MythicSwordItem`；`ItemUtil.getInternalName(stack)` → `mythic_sword`
- `stack.serializeAsBytes()` → `ItemStack.deserializeBytes()` 往返后内部名与神话类型都保持
- `stack.clone()` 保留 PDC；普通钻石剑 `getMythicItem` 返回 null（不会误判）

全息与 WorldEdit 7（同一冒烟插件调用）：
- `HologramAPI.createHologram(...)` 正常创建（世界实体 +1），`removeHologram` 后实体数回落
- `CakeEvent.onActive()` / `onInactive()` 无异常：`WorldEdit#newEditSession`、`BlockTypes`、
  `RandomPattern`、`setBlocks`、撤销流程在 1.21.8 + FAWE 2.16.2 上可用

菜单与命令（同一冒烟插件调用）：
- Bot 作为真实玩家执行 `/spawn`、`/settings` 均返回 true（命令注册正常；`/pi` 是 `/pitadmin pi` 的子命令，不是顶层命令）
- `AdminItemMenu` 可正常打开（物品渲染 + PDC 读取整条路径无异常）


### 本轮修掉的运行时问题

1. **`HologramAPI` 在 1.21.8 无法初始化**：静态初始化用 1.8 反射工具探测版本
   （`Minecraft.VERSION`），在 Paper 1.21.8 上解析不到 `net.minecraft.server.UNKNOWN.Entity`，
   类初始化直接失败，所有全息（14 处调用）全部不可用。改为固定 1.21 语义，删掉 1.8 发包与
   ProtocolSupport 分支。
2. **附加插件 hook 缺失时全息整体失效**：`HologramAPI.createHologram` 原本直接调
   `ThePit.api.createHologram`，未装附加插件时 NPE；现在回退到内置 `PacketHologram`。
3. **全息盔甲架跨重启累积**：真实实体随区块保存，强杀/停机不走 despawn 就残留；
   启动时按 scoreboard tag `pit_hologram` 清理已加载区块里的残留。
4. **PDC 读到错误类型会抛异常**：1.8 的 `tag.extra` 里同一个键可能先后写成字符串与数字，
   迁到强类型 PDC 后按错误类型读取会抛 `IllegalArgumentException`（Paper 直接断言），
   打断整条物品/菜单渲染。`ItemData` 的各 `get` 现在类型不匹配时返回 null。
   症状：管理菜单打开时抛 `The found tag instance (StringTag) cannot store Integer`，
   菜单实际没开（`getOpenInventory` 停在 Crafting）；修复后菜单正常打开。




本轮为此修掉的问题：
1. **PacketEvents 把机器人踢下线**（`failed to inject into a channel`）：`BotEntity.spawn` 把
   `EmbeddedChannel` 登记到 `ProtocolManager`，`destroy` 时移除，PE 认得 fake channel 就不再踢。
2. **`LevelUtil.bootCache` 整型溢出**：`highest-prestige` 默认 `Integer.MAX_VALUE` 时
   `limit * maxLevel` 溢出成负数 → `NegativeArraySizeException` 打断玩家存档写入；
   加了上限与溢出保护，配置默认值改为 35。
3. **不要重定位 `net.kyori`**：Paper API 用服务端自带的 adventure 类型，
   重定位后 `PlayerJoinEvent#joinMessage` 抛 `NoSuchMethodError`。
4. **PlaceholderAPI 可选化**：未安装时跳过变量替换（`ChatListener` / `TabHandle`）。
5. **`ProfileLoadRunnable` 未实例化**：退出收尾取到 null，已在 onEnable 末尾启动。




## 本轮完成：全量 Java 编译清零（1911 → 0）

1. **发包层统一 PacketEvents 2.14**：`cn/mythicland/npclib/nms/packetevents/NPC_PacketEvents` 取代
   `NPC_v1_8_R3` 与 6 个 `PacketPlayOut*Wrapper`；`SignGui` / `VisibleApi` / `PlayerMarco` / `PetFactory`
   全部改 PacketEvents；`ThePit` 补 `onLoad` 的 `setAPI/load` 与 `onEnable` 的 `init`、`onDisable` 的 `terminate`。
2. **物品与数据层**：`ItemBuilder`/`ItemUtil`/`InventoryUtil` 的 NBT 全改 PDC（`ItemData`），
   背包序列化改 `ItemStack#serializeAsBytes`；`PublicUtil.toNMStackQuick` 改返回 Bukkit `ItemStack`。
3. **1.8 常量批量迁移**：Sound / Material / PotionEffectType / Enchantment 全部换成 1.21 名
   （批量替换脚本处理了 100+ 文件；`STAINED_GLASS*`、`STAINED_CLAY` 按数据值映射到对应染色材质）。
4. **事件与 UI**：`CakeEvent` 蛋糕切片改方块状态 `Cake#getBites`；`EventWorldEdits`/`SpireEvent`/`HamburgerEvent`
   改 WorldEdit 7 API（`libs/` 换成 `FastAsyncWorldEdit-Paper-2.16.2.jar`，删掉 WE6 两个旧 jar）；
   `Hologram`/`PacketArmorStand`/`CustomEntityNPCFactory` 改真实实体 + `hideEntity/showEntity`。
5. **补齐缺失类**：`AnimationRunnable`/`ClearRunnable`/`ProfileLoadRunnable`（1.8.8 基线导入时丢失）、
   `ItemTypes`、`IMythicHammer` + `MythicHammerItem`（hammer 特性占位）、`PitItemButton` 增加 `Supplier` 构造。
6. **构建**：新增 `tools/compile-check.ps1`（javac 直编 + Lombok 强制生效 + 按文件汇总）、
   `tools/print-classpath.gradle`、`tools/verify-bot.ps1`、`gradle/libs.versions.toml` 补 paper/packetevents。

### 关键陷阱（本轮踩到的，务必保留）

- **Lombok 门禁**：只要任一编译单元有 enter 阶段错误（import 的包/类不存在），javac 会整体跳过注解处理，
  于是所有 `@Getter/@Data/@RequiredArgsConstructor` 生成物都变成 `cannot find symbol`，错误数虚高十倍以上。
  `tools/compile-check.ps1` 用 `-XDshould-stop.ifError=GENERATE`（必须写在命令行，不能放 @argfile）绕过。
- **批量替换要按「长串优先」**：`PotionEffectType.SLOW` 先于 `SLOW_DIGGING` 替换会得到
  `SLOWNESS_DIGGING`；`Material.BED` 会把 `BEDROCK` 变成 `RED_BEDROCK`；`Material.WOOD` 会污染
  `WOODEN_SWORD`。本轮已出现并修复 4 处此类反噬。
- **不要用 PowerShell `Get-Content`/`Set-Content` 批改中文源码**：默认按 ANSI 解码会把中文写成乱码，
  甚至破坏字符串字面量；用编辑工具或 `.NET + UTF8Encoding(false)` 显式读写。
- **`-XDshould-stop.ifError` 下 javac 一次只报 1-2 条错误**，需要「修一批 → 重跑」迭代；
  批量常量迁移要用脚本一次清完。


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

## 下一步（按顺序）

1. **测试服冒烟**（见下方清单），按反馈修运行时问题
2. 数据格式变化要在测试服确认：自定义数据 `NBT tag.extra` → PDC、背包序列化 `ItemStack#serializeAsBytes`
   （旧存档不能直读，main 也是新格式）
3. 视觉回查：染色玻璃/染色陶瓦按数据值映射，个别动态颜色（`ItemBuilder.durability(color)` 动态传值）
   先统一落到灰色系，需要按实际效果再调

## 测试服冒烟清单

**部署**：把 `core/build/libs/ThePitGay-V.jar` 放到 Paper 1.21.8 的 plugins/，启动看控制台。

启动阶段（重点看异常，不是看报错多少）：
- [ ] `onLoad` 不报 `PacketEvents` 相关异常；`PacketEvents.getAPI().init()` 成功
- [ ] `EventFactory` 反射注册事件无 `NoClassDefFoundError`（WorldEdit 7 / FAWE 2.16.2）
- [ ] 无 `NoSuchMethodError` / `NoClassDefFoundError`（说明 shade 的库版本对得上）
- [ ] `Done (xx.xxxs)` 正常

功能：
- [ ] NPC 正常显示：玩家皮肤 NPC / 村民 NPC、朝 player 转身、装备/动画、重复重启不堆叠
- [ ] 全息文字跟随 NPC 传送、NPC 销毁后全息实体不残留
- [ ] 物品：`/pi` 拿神话物品、附魔台附魔动画、仓库（寄存箱）存取、告示牌 GUI 打开可输入
- [ ] 战斗：平砍伤害数值合理、连杀/赏金/复仇结算、宠物死亡结算
- [ ] 计分板、tab、名牌、聊天 hover/click（`ChatComponentBuilder`）
- [ ] 事件：尖塔/蛋糕/汉堡等 WorldEdit 事件能正常刷地形并撤回
- [ ] **机器人**：能被创建、出现在世界里、可被攻击/击杀、触发连杀、按 respawnDelay 重生；
      注意机器人现在会出现在在线玩家列表里（`Bukkit.getOnlinePlayers()` 包含机器人），
      加入/退出事件也会触发（已屏蔽加入广播）

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

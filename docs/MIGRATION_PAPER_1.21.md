# ThePitUltimate：仓库结构与 Paper 1.21.8 适配记录

> 目标服务端：Paper 1.21.8（`paper-api:1.21.8-R0.1-SNAPSHOT`），放弃 1.8.8 运行兼容。  
> 本文分三部分：①仓库与玩法结构 ②1.21 变更清单与已实施改动 ③未完成项、风险与验证建议。

---

## 一、仓库结构

### 1.1 构建配置

| 位置                              | 内容                                                                                      |
| ------------------------------- | --------------------------------------------------------------------------------------- |
| `settings.gradle.kts`           | 多模块：`base`、`core`；启用 foojay toolchain 自动下载 JDK                                          |
| `build.gradle.kts`              | 根工程，`com.palantir.git-version` 以 `git describe` 生成版本号                                   |
| `gradle/libs.versions.toml`     | version catalog：Kotlin 2.1.20、shadow 8.1.7、adventure、ProtocolLib、mongodb 等              |
| `gradle/wrapper`                | Gradle 8.5                                                                              |
| `base/build.gradle.kts`         | 基础模块，`shadowJar` 产物 `ThePitUltimate-<ver>.jar`，重定位 hutool/kyori/panda 等第三方包             |
| `core/build.gradle.kts`         | 玩法模块，含 `injectGitVersion`/`lastFin` 双向注入 `%git_version%`、ProGuard 任务；默认 `devBuild=true` |
| `.github/workflows/release.yml` | tag `v*` 触发，JDK 21 构建并发布 Release                                                        |

编译目标：Java 21（`options.release=21`，Kotlin `JVM_21`）。基线（1.8.8）构建可通过。

### 1.2 模块与包

| 包                    | 文件数 | 职责                                                                                                                    |
| -------------------- | --- | --------------------------------------------------------------------------------------------------------------------- |
| `cn.charlotte.pit`   | 113 | 插件入口 `ThePit`、数据模型（`PlayerProfile`/`PlayerInv`/`Mail`/`CDK`）、自定义事件、buff、序列化器                                          |
| `net.mizukilab.pit`  | 867 | 玩法主体：`item`/`enchantment`/`perk`/`quest`/`events`/`menu`/`npc`/`map`/`listener`/`runnable`/`scoreboard`/`sound`/`tab` |
| `net.jitse.npclib`   | 32  | 内嵌 NPC 库（v1_8_R3 实现 + 发包 wrapper）                                                                                     |
| `io.irina.backports` | 5   | 高性能并发容器（`SWMRHashTable` 等）                                                                                            |
| `spg.lgdev`          | 2   | 自定义服务端 iSpigot 的 `MovementHandler` 垫片（仓库内自带实现）                                                                        |

入口：`cn.charlotte.pit.ThePit#onEnable` → `preLoad()`（配置/数据库/监听器/物品/菜单/NPC/游戏/勋章/buff/全息/音效/perk/附魔/任务/事件/移动）→ 下一 tick `startCoreLoader()` 反射调用 `net.mizukilab.pit.Loader#start`（注入 `PitInternalImpl` 作为 `PitInternalHook`）→ `postLoad()` 从 MongoDB 载入事件队列。  
核心模块化机制：`@AutoRegister` + `ClassUtil.getClassesInPackage` 扫描包内类，工厂类（`ItemFactor`/`PerkFactory`/`EnchantmentFactor`/`EventFactory`/`MedalFactory`/`NpcFactory`/`BuffFactory`/`QuestFactory`）反射实例化并注册。

### 1.3 监听器 / 命令 / 配置资源

- 监听器：`listener/` 下 `CombatListener`、`PlayerListener`、`ProtectListener`、`ChatListener`、`DataListener`、`EnderChestListener`、`WarehouseListener`、`GameEffectListener`、`PacketListener`（ProtocolLib 发包拦截）、`MythicMobListener`。
- 命令：`command/PitCommands.kt`（LiteCommands，约 1.1k 行），另有 `menu/` 内的管理命令入口。
- 资源：`base/src/main/resources` 下 `plugin.yml`、`config.yml`（Mongo/Redis/宵禁/调试）、`worlds/world.yml`（竞技场点位：spawn/hologram/npc/region/events）、`items.csv`、`js/`（Nashorn 脚本附魔）、`*.nbs`（音符盒音乐）、`*.schematic`；`core/src/main/resources` 下 `custom.yml`、`settings.yml`、`tab.yml`。

### 1.4 「天坑乱斗」玩法实现

天坑（The Pit）是**无局制、常驻竞技场**的 PvP 玩法，不是回合制小游戏：

| 模块     | 实现位置                                                                                                                                                                     | 说明                                           |
| ------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | -------------------------------------------- |
| 竞技场与轮换 | `map/MapSelector`、`ConfigManager(nextConfig/synchronizeLegacy)`、`worlds/world.yml`                                                                                       | 按 `startDate + duration` 计算当前地图 id，定时切图并重置区域 |
| 玩家管理   | `PlayerProfile`（`load/save/saveData`，Mongo 持久化）、`ProfileLoadRunnable`、`SafetyJoinListener`                                                                               | 登录异步加载档案，等级/声望/金币/背包/末影箱/仓库/邮件全部挂在档案上        |
| 出生与保护  | `runnable/ProtectRunnable`、`PitPlayerSpawnEvent`、`PitPlayerSpawnEvent` 回调、`iSpigot#onMove`                                                                               | 出生点保护期、禁止移动区域、死亡后自动重生                        |
| 战斗与伤害  | `listener/CombatListener`（1.4k 行）、`PitDamageEvent`、`PitDamagePlayerEvent`、`PlayerUtil#damage`                                                                            | 统一伤害管线：事件 → 护甲/附魔/perk 修正 → 击退与结算            |
| 天坑机制   | `runnable/GoldDropRunnable`（金币掉落）、`runnable/BountyRunnable`（赏金）、击杀连杀 `perk/type/streak/*`、`PitStreakKillChangeEvent`                                                     | 死亡掉落金币、连杀奖励、赏金与通缉                            |
| 计分与结算  | `PlayerProfile#getLevel/addExperience`、`PitGainCoinsEvent`、`PitGainRenownEvent`、`PitKillEvent`、`scoreboard/Scoreboard`、`runnable/LeaderBoardRunnable`、`LeaderBoardEntry` | 经验→等级→转生（prestige），金币/声望分别结算并推送侧边栏与排行榜       |
| 玩法事件   | `events/EventFactory`、`events/impl/*`（Spire、Squads、RagePit、RedVSBlue、Hamburger、DragonEggs…）、`events/trigger/*`                                                           | 定时开启的全场事件，含独立计分与奖励                           |
| 成长系统   | `perk/`（perk 与连杀）、`enchantment/`（附魔，含 JS 脚本附魔）、`quest/`、`medal/`、`buff/`                                                                                                 | 天赋树、附魔池、任务、勋章、药水 buff                        |
| 展示层    | `hologram/`、`nametag/`、`tab/`、`actionbar/`、`sound/`、`pet/`                                                                                                               | 全息、头顶称号、Tab、动作栏、音效、宠物                        |

---

## 二、1.21 变更清单

### 2.1 已实施（本次改动）

| 类别        | 位置                                                                                                                  | 做法                                                                                                                                                                                                                    |
| --------- | ------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 构建依赖      | `gradle/libs.versions.toml`、`base/build.gradle.kts`、`core/build.gradle.kts`                                         | 新增 `paper_api = 1.21.8-R0.1-SNAPSHOT` 与 `repo.papermc.io`；`compileOnly(libs.spigot.get8())` → `compileOnly(libs.paper.api)`；移除 `adventure-platform-bukkit`                                                            |
| 插件元数据     | `base/src/main/resources/plugin.yml`                                                                                | 待改：`api-version: 1.8` → `1.21`（见 3.1）                                                                                                                                                                                 |
| 物品数据（NBT） | `util/item/ItemUtil`、`util/item/ItemBuilder`、新增 `compat/item/ItemData`                                              | `tag.extra`（`NBTTagCompound`）→ `PersistentDataContainer`，键名加 `x.` 前缀并统一小写；字符串列表用 `\0` 拼接                                                                                                                              |
| 物品属性      | `ItemBuilder#itemDamage`                                                                                            | `AttributeModifiers` NBT → `ItemMeta#addAttributeModifier(Attribute.ATTACK_DAMAGE, …, EquipmentSlotGroup.MAINHAND)`                                                                                                   |
| 不可破坏      | `ItemBuilder#buildWithUnbreakable`                                                                                  | `tag.setBoolean("Unbreakable")` → `ItemMeta#setUnbreakable`                                                                                                                                                           |
| 头颅纹理      | `ItemBuilder#setSkullProperty`                                                                                      | `GameProfile`+`Property` 反射 → 解析 base64 纹理 JSON 取 URL → `Bukkit.createPlayerProfile().getTextures().setSkin()` → `SkullMeta#setOwnerProfile`                                                                          |
| 染色材质      | 新增 `compat/item/LegacyColor`，接入 `ItemBuilder#durability`                                                            | 1.8「基础材质 + data」→ 1.13 拆分后的具体颜色变体（羊毛/陶瓦/染色玻璃/玻璃板/地毯/旗帜/床/染料/头颅）                                                                                                                                                       |
| 枚举常量      | 全仓库                                                                                                                 | `Material`/`Sound`/`PotionEffectType`/`Enchantment`/`EntityType` 共 370 处重命名（`SKULL_ITEM→PLAYER_HEAD`、`STAINED_CLAY→TERRACOTTA`、`CLICK→UI_BUTTON_CLICK`、`DURABILITY→UNBREAKING`、`CONFUSION→NAUSEA`、`PRIMED_TNT→TNT` 等） |
| 粒子        | `util/ParticleBuilder` + 8 个调用文件                                                                                    | `EnumParticle` + `PacketPlayOutWorldParticles` → `Particle` + `World#spawnParticle`；`REDSTONE/SPELL_MOB` 改 `DUST`(`DustOptions`)/`ENTITY_EFFECT`                                                                      |
| 全息        | `hologram/packet/PacketArmorStand`、`Hologram`、`ArmorStandHelper.kt`、`PacketHologram.kt`、`PacketHologramRunnable.kt` | NMS 幽灵实体 + 手写 spawn/metadata/teleport 包 → 真实 `ArmorStand`（`marker`/`invisible`/`setPersistent(false)`/`pit_hologram` 计分板标签）+ `Player#hideEntity/showEntity`；`attachFakeEntity` → `addPassenger`                       |
| 聊天/标题/动作栏 | `ThePit`、`Util.kt`、`chat/TitleUtil`、`chat/ActionBarUtil`                                                            | `BukkitAudiences` → Paper 原生 `Audience`；`PacketPlayOutChat(byte 2)` → `Player#sendActionBar`；`PacketPlayOutTitle` → Adventure `Title`                                                                                 |
| Kotlin 调度 | 19 处 `runTask*` 调用                                                                                                  | Paper 为调度器新增 `Consumer<BukkitTask>` 重载，Kotlin 尾随 lambda 产生歧义，统一显式标注 `Runnable`                                                                                                                                        |
| 其他        | `util/PublicUtil`                                                                                                   | 删除 `CraftItemStack.handle` 反射与「从区块摘除实体」逻辑；`toNMStackQuick` 降级为直接返回 Bukkit 物品                                                                                                                                          |

辅助脚本（可复用于后续迁移）：`tools/codemod_enums.py`、`tools/codemod_particle.py`、`tools/codemod_kotlin_runnable.py`、`tools/parse_javac.py`、`tools/show_errors.py`。

### 2.2 待改（未完成）

**A. 插件元数据与配置**

- `plugin.yml`：`api-version` 1.8 → 1.21；`depend: [LuckPerms, ProtocolLib]` 需按 ProtocolLib 去留调整；版本号 4.4.11 → 4.5.0。
- `config.yml`：`==: org.bukkit.Location` 的 YAML 反序列化在 1.21 仍可用，但 `world:` 键需与 1.21 世界名一致。

**B. 发包层（84 个文件，base 27 / core 57）**

| 原依赖                                                                                                                   | 文件                                                                   | 替换方案                                                                                 |
| --------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------- | ------------------------------------------------------------------------------------ |
| `CraftPlayer#getHandle().playerConnection.sendPacket`                                                                 | 41 处（`VisibleApi`、`NametagBoard`、`PacketListener`、`RedVSBlueEvent`…） | 按包类型改为对应 Bukkit API；无法 API 化的（装备伪装）改用 ProtocolLib `WrapperPlayServerEntityEquipment` |
| `PacketPlayInFlying`                                                                                                  | 14 处（`MovementHandler` 系列、`PlayerMoveHandler`）                       | `PlayerMoveEvent`；`iSpigot` 垫片已用事件实现，接口第 4 参数改为 `Object`                             |
| `NBTTagCompound`/`NBTTagList`                                                                                         | 12 文件                                                                | 已给 `ItemData`，剩余 `IMythicItem`、`HamburgerEvent`、`Utils`、`JewelSword.kt` 等按同法改写       |
| `ItemArmor`/`ItemSword`/`ItemBow`                                                                                     | 9 处（附魔判定）                                                            | `ItemStack#getType()` + `Material` 分类（`Tag.ITEMS_SWORDS` 等）                          |
| `EnumParticle`                                                                                                        | 已处理                                                                  | —                                                                                    |
| `MathHelper`                                                                                                          | 25 处                                                                 | 用 `org.bukkit.util.Vector`/`Math` 内联实现（floor/角度换算）                                   |
| `PacketPlayOutEntityEquipment`/`EntityMetadata`/`EntityHeadRotation`/`NamedEntitySpawn`/`PlayerInfo`/`ScoreboardTeam` | `net/jitse/npclib` 6 个 wrapper                                       | NPC 层整体替换（见 C）                                                                       |
| `WorldServer`/`World`/`Chunk`/`MinecraftServer`                                                                       | `BatUtil`、`FishingHandler`、`CustomEntityNPCFactory`                  | Bukkit `World`/`Entity` API                                                          |
| `IChatBaseComponent`/`ChatComponentText`                                                                              | `chat/CC`                                                            | Adventure `Component`                                                                |
| `DataWatcher`                                                                                                         | `VisibleApi`、`PetFactory`                                            | `Entity#setInvisible/setGlowing` 等元数据 API                                            |
| `EntityArmorStand`/`EntityBat`/`EntityItem`/`EntityLiving`                                                            | `BatUtil`、`PetFactory`、`HologramRunnable`                            | 真实实体 + `BukkitRunnable` 驱动                                                           |

**C. NPC 与客户端实体**

- `net/jitse/npclib`（32 文件）整体不可用：`nms/v1_8_R3/*` 需删除，`NPCBase`/`NPCLib` 改为基于 ProtocolLib 1.21 wrapper 的玩家 NPC（`WrapperPlayServerPlayerInfoUpdate` + `SpawnEntity` + `PlayerInfoRemove`），或直接依赖 Citizens 2.0.8+。
- `CustomEntityNPCFactory`、`PetFactory`、`HologramRunnable` 中的自定义实体（蝙蝠/盔甲架/掉落物）需改为真实实体。

**D. 界面与输入**

- `util/sign/SignGui`：1.8 用 `PacketPlayOutOpenSignEditor` 打开虚拟告示牌；1.21 用 Paper `Player#openSign(Sign)`，需要放置临时告示牌方块并在关闭后移除。
- `PacketListener`（ProtocolLib）：拦截 `PacketPlayInWindowClick`/`PacketPlayOutEntityEquipment` 做菜单与装备伪装，需按 1.21 包名重写（`WrapperPlayServerWindowItems`、`WrapperPlayServerSetSlot`）。

**E. 其他**

- `util/music/SongPlayer`（NBS 播放）：`PacketPlayOutWorldEvent`/`CustomSound` → `Player#playSound(Location, String, SoundCategory, vol, pitch)`。
- `enchantment/AbstractEnchantment`：附魔判定链依赖 NMS `ItemStack` 与 `Enchantment` 反射，改为 `ItemMeta#getEnchantmentLevel`。
- `core` 模块 57 个 NMS 文件，主要集中在 `enchantment/type/*`（粒子/击退/药水）、`events/impl/major/*`（队伍装备伪装）、`item/type/*`（自定义物品效果）、`util/Utils`。

---

## 三、当前状态、风险与验证

### 3.1 状态

- 分支：`feat/paper-1.21.8`（`master` 仍为 1.8.8 可构建状态）。
- `:base:compileJava` 未通过：报错 100 条（javac 默认截断），集中在 base 剩余 27 个 NMS/发包文件；`core` 尚未开始编译。
- 已完成部分（构建、物品数据、全息、聊天、粒子、枚举、Kotlin 调度）本身无编译错误。

> **重要现象**：只要编译单元里存在任一「包不存在」类错误，javac 就会跳过 Lombok 注解处理，
> 导致 `@Getter`/`@RequiredArgsConstructor` 生成的成员全部「cannot find symbol」，把真实错误数放大数倍。
> 因此当前日志里的 `getMongoDB()`/`getGlobalConfig()`/`getParker()` 一类报错**不是真实缺陷**，
> 修完所有 NMS 引用后会自行消失。排查时应只关注 `package ... does not exist` 与其直接消费点。
> 另注：`-Xmaxerrs` 会改变该行为，迁移期间不要加此参数。

### 3.2 已知不兼容点与取舍

| 不兼容点                 | 处理                                        | 风险                                                                                                          |
| -------------------- | ----------------------------------------- | ----------------------------------------------------------------------------------------------------------- |
| 物品数据由 NBT 改为 PDC     | 键语义不变，前缀 `x.`                             | **存量数据不兼容**：数据库里 1.8 时代的物品没有 PDC 数据，`ItemUtil#isIllegalItem` 会判为非法物品，需要一次性迁移脚本（读旧 NBT → 写 PDC），否则玩家背包物品会被清理 |
| 物品属性修饰符              | 改为 `Attribute.ATTACK_DAMAGE` + `MAINHAND` | 旧 NBT 用 UUID 去重，现用固定 `NamespacedKey`，重复调用会覆盖而非叠加（`removeAttributeModifier` 已处理）                             |
| 全息由幽灵实体改真实实体         | 服务端追踪替代手工发包                               | 实体数量增加（每行一个盔甲架）；`bootstrapWorld` 启动时清理盔甲架会误删，已用 `pit_hologram` 标签区分，需确认 1.8 遗留代码路径不再无条件 `remove()`          |
| 头颅纹理                 | base64 → URL → `PlayerTextures`           | 无法离线解析签名纹理；若纹理串不含 `url` 字段则退化为普通头颅                                                                          |
| 染色材质                 | `LegacyColor` 运行时映射                       | `durability()` 传入非颜色 data（如 96/1560 真实耐久）不受影响；但个别把 data 当颜色又当耐久的写法需人工确认                                     |
| ProtocolLib          | 仍列为 `depend`                              | 1.8 的 wrapper 类已不存在，`PacketListener` 未重写前**启动即抛 `NoClassDefFoundError`/`ClassCastException`**               |
| 自定义服务端垫片 `spg.lgdev` | 保留（本就是事件实现）                               | 仅接口签名需把 `PacketPlayInFlying` 改为 `Object`                                                                    |

### 3.3 验证建议

1. `./gradlew :base:compileJava` → `:core:compileJava` → `clean build`，逐模块收敛。
2. 启动 Paper 1.21.8 + LuckPerms + ProtocolLib 2.9.x，确认 `onEnable` 无异常、`/pit` 命令注册、`plugin.yml` 版本与 `api-version` 生效。


1. 数据兼容：先用测试库导入一份 1.8 存档，验证 `PlayerProfile#load`、`InventoryUtil#playerInvToString/stringToPlayerInv`（依赖 NMS 序列化的部分需改 `ItemStack#serializeAsBytes/deserializeBytes`）。
2. 玩法回归清单：加入/重生保护 → 击杀结算（金币/经验/连杀）→ 死亡掉落 → 转生 → 商店购买 → 附魔/神话物品词条 → 事件开启（Spire/Squads）→ 排行榜与侧边栏 → 全息/NPC/告示牌/音效。
3. 性能：`AsyncTickHandler`/`TickHandler` 与全息实体数量，建议 100 假人压测对比 1.8 基线 TPS。


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
| Kotlin 编译 | **0 错误**（起点 472） |
| Java 编译 | **100 条**（其中 54 条在 Bot 模块） |
| 可打包 | 否（Java 未过） |

## 错误分布（实测）

```
BotEntity.java        28    extends EntityPlayer（1.8 NMS）
BotVisibility.java    16    发包控制实体可见性
BotPatrol.java        10    巡逻 AI，依赖 BotEntity
ItemBuilder.java       7    物品构建，NMS 写入
SignGui.java           5    告示牌 GUI 发包
PlayerMoveHandler.java 4    移动处理
其余                  30    零散
```

## 已完成的迁移（要点）

1. **构建链**：`paper-api:1.21.8` + Java 21 toolchain；移除 spigot-1.8.8 / PandaSpigot / vendor 子模块
2. **Hologram.java 重写**：1.8 发包 → 真实 ArmorStand 实体（marker + 不可见），
   用 `player.showEntity/hideEntity` 做按玩家显隐
3. **AbstractEnchantment.canApply**：NMS `ItemSword/ItemBow/...` 判定 → `Material` 判定
4. **物品类 NBT → PDC**：AngelChestplate / ArmageddonBoots / GoldenHelmet / LuckyChestplate
   用 `cn.charlotte.pit.compat.item.ItemData`
5. **枚举批量迁移**：Sound（ANVIL_USE/ENDERDRAGON_*/CHEST_OPEN…）、
   Material（GOLD_SWORD→GOLDEN_SWORD、RAW_BEEF→BEEF、LEASH→LEAD、STAINED_GLASS_PANE）、
   EnumParticle→Particle、PotionEffectType.CONFUSION→NAUSEA
6. **调度歧义**：`runTask(plugin) {}` → `runTask(plugin, Runnable {})`（有 delay 参数时不要补右括号）
7. **兼容层补齐**：`cn.charlotte.pit.compat.item.ItemData`、`compat.math.MathHelper`、
   `ThePit.getApi()` 返回 `PitInternalImpl.INSTANCE`

## Bot 重写：已勘察完毕的 1.21.8 签名

从服务端 jar（NAS `versions/1.21.8/paper-1.21.8.jar`）用 javap 实测：

```java
ServerPlayer(MinecraftServer, ServerLevel, GameProfile, ClientInformation)
ClientInformation.createDefault()
void ServerPlayer#die(DamageSource)
PlayerList.placeNewPlayer(Connection, ServerPlayer, CommonListenerCookie)
Connection(PacketFlow)                                  // PacketFlow.SERVERBOUND
CommonListenerCookie(GameProfile, int, ClientInformation, boolean, String, Set<String>, KeepAlive)
```

### 1.8 → 1.21 重构对照

| 1.8 写法 | 1.21 写法 |
|---|---|
| `extends EntityPlayer` | `extends ServerPlayer` |
| 构造传 `PlayerInteractManager` | 传 `ClientInformation.createDefault()` |
| 反射 `World.u`（worldAccesses） | 删除，`ServerLevel` 自管 |
| 反射 `PlayerList.o`（统计） | 删除，`ServerPlayer` 自带 |
| 反射改 `gamemode` | `player.gameMode.changeGameModeForPlayer(...)` |
| `IWorldAccess` 匿名类（十余个空实现） | 删除，1.21 无此接口 |
| `Packet` 匿名发送器 | 由 `placeNewPlayer` 建真实 `Connection` |

预计 1.21 版 `BotEntity` 约 120 行（原 379 行）。

### 已确定的方案

用户选择 **A1-a-1**：走 `placeNewPlayer` 真实登录路径。
已知副作用：会为每个 Bot 生成 `playerdata/<uuid>.dat`（60 个文件），
并触发 `PlayerJoinEvent`（注意与 LuckPerms 等插件的交互）。

## 下一步（按顺序）

1. 重写 `BotEntity.java`（按上表，替换 379 行 1.8 实现）
2. 重写 `BotVisibility.java`（改用 `player.showEntity/hideEntity`）
3. 重写 `BotPatrol.java`
4. 全量编译 → 应降至个位数错误
5. 打包部署到测试服，验证 Bot 出现 / 可被击杀 / 加连杀 / 重生

## 操作注意事项（血泪教训）

- **改文件前先 `grep -c` 校验**：曾有脚本把文件写坏（方法体重复插入），
  而 javac 因语法错误提前中止，导致错误数从 100 "降到" 32 的假象
- **批量替换必须带安全网**：替换后重新编译，错误数上升就自动回滚
- **不要在同一个文件里并发发多个编辑**：会互相覆盖
- **`s.find()` 可能返回 -1**：切片前必须断言，否则产生垃圾内容

## 相关文档

- `docs/p2-2-replaced-files.txt`：批量替换为迁移版的 28 个文件清单
- `docs/p2-3-replaced-files.txt`：另 14 个
- `docs/java-nms-files.txt`：仍含 `v1_8_R3` 的 45 个 Java 文件

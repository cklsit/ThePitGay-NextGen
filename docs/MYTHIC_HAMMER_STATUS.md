# 神话之锤：状态、冲突规则与展示（定稿）

> 状态：**定稿**，补齐 `MYTHIC_HAMMER_ENCHANTS_ANSWERS.md` Q7 / Q24 与 `MYTHIC_HAMMER_RULINGS.md` R7 / R8 的实现口径。
> 关联：`MYTHIC_HAMMER_ITEM.md`、`MYTHIC_HAMMER_TERMS.md`、`MYTHIC_HAMMER_RULINGS.md`。
> 本文分五部分：①状态注册中心 ②互斥 / 叠加封顶 / 优先级规则 ③季节失效的 lore 呈现 ④抽取集成点 ⑤校验与口径。

---

## 一、状态注册中心

**复用基础**：`base/.../cn/charlotte/pit/buff/`（`BuffData`、`AbstractPitBuff`、`BuffFactory` 及 `CoagulationBuff` / `HealPoisonDeBuff` / `HemorrhageDeBuff` / `BountySolventBuff` / `SiltedUpBuff` / `RottenDeBuff`）+ `PotionEffect` + `PlayerProfile` 连杀数据。新增状态挂在同一包下，由统一注册表分配 id、时长、叠层与清除规则；各附魔只声明 id 与参数，不各自持有 `HashMap<UUID, Cooldown>`。

| 状态 id | 语义 | 现状 | 依据 |
| --- | --- | --- | --- |
| `stun` | 眩晕：移速与攻击伤害大幅降低 | 复用 | `Unpredictably.kt:50-51`（2 秒，20 秒冷却） |
| `shield_layer` | 护盾层：抵消 1 次玩家伤害 | 复用 | `AegisEnchant.java:68,80,100` |
| `absorption` | 生命吸收 / 金心 | 复用 | `PlayerListener.java:481,498,513,542,557,584`；`addon_config.yml:11` |
| `potion_debuff` / `potion_buff` | 减速、失明、虚弱、黑暗等 | 复用 | `PotionEffect` + `buff` 包 |
| `streak` | 连杀计数 | 复用 | `PitStreakKillChangeEvent`、`WitherCraftKillStreak.java:100-111` |
| `critical` | 暴击判定（跳跃下落） | 复用判定 | `GameEffectListener.java:429-432` |
| `sprint_lock` | 禁止冲刺 | **新造** | `PlayerMoveEvent` + `setSprinting(false)`，或高等级缓慢近似 |
| `hotbar_lock` | 锁定物品栏切换 | **新造** | 监听 `PlayerItemHeldEvent` 回滚 |
| `crit_suppress` | 压制暴击 | **新造** | 伤害管线加暴击抑制标记 |
| `damage_down` | 对他人造成的伤害降低 | **新造**（语义复用） | `Winter.java:64,116` 的「霜寒」伤害修正；用于 55 钝鸣 |
| `vision_block` | 视野遮蔽 | **新造**（纯表现） | 粒子 + `DARKNESS`；用于 68 落砂，不进命中判定 |
| `hammer_mark` | 锤印 | **新造** | 上限 3 层、8 秒、切目标清空；16 号触发阈值第 3 / 2 / 1 层 |
| `rivet_mark` | 铆钉标记 | **新造** | 独立于锤印的钝器易伤标记，10 号使用 |

**统一规则**：每个 id 声明 `maxStack`、`durationTicks`、`clearOn`（目标切换 / 死亡 / 脱战 / 手动）、`tickHandler`；注册中心提供 `apply / has / clear / tickAll`。季节开关（R8）与触发链短路共用同一入口。

> 注意：`SEASONAL` 附魔**不得实现 `ILimit`**。见 `EnchantButton.java:836-838`（`isBlackList` = `instanceof ILimit`），否则会被从所有抽取候选中剔除。

---

## 二、互斥 / 叠加封顶 / 优先级规则

**依据**：`EnchantButton.java:836-838` 的 `isBlackList` 只按类型排除，不做词条间互斥；清单中仅 6 号「与 5 号互斥」一个孤例。故新增本节，数据驱动，不在 100 条附魔里逐条硬编码。

### 2.1 互斥组（不可共存，抽候选时过滤）

| 组 | 条目 | 理由 |
| --- | --- | --- |
| M1 | 5 沉肩 ↔ 6 惯性 | 一个要求站定、一个要求移动，条件互斥 |
| M2 | 22 砧座 ↔ 36 击桩 | 一个要求自身静止未出手、一个要求命中静止目标，同一次挥击不能同时结算 |

> 22 砧座与 5 沉肩**不是互斥**：两者可共存，归入叠加封顶（2.2）。

### 2.2 叠加封顶组（可共存，总和受限）

| 组 | 条目 | 上限 |
| --- | --- | --- |
| C1 自身减伤 | 5 / 8 / 22 / 26 / 34 / 40 / 60 / 82 | 合计 **35%** |
| C2 自身攻速 | 23 / 51 / 65 / 73 / 95 | 净修正 clamp 到 **±30%**，五条折算到同一个 `ATTACK_SPEED` modifier |
| C3 `live` 消耗 | 11 / 16 / 20 / 27 / 29 / 32 / 52 / 53 / 66 / 68 / 71 / 72 / 75 / 83 / 84 / 86 / 89 / 94 | 同一 tick 合计最多 **2 点**，超出部分丢弃且不重复触发冷却 |
| C4 增伤 | 1 / 3 / 4 / 6 / 14 / 15 / 17 / 18 / 25 / 28 / 32 / 36 / 38 / 42 / 48 / 61 / 66 / 70 / 71 / 74 / 81 / 88 | 单条命中合计 **+60%** |

**不纳入封顶组的条目**：63 压舱（击退抗性，非减伤）；85 秤衡（伤害按时间分摊，总量不变）；98 霜砧（降低**目标**攻速，作用于对方而非自身 modifier）；97 收锭 / 100 归砧（保护 `live`，不消耗）。

### 2.3 优先级（同时触发时的结算顺序）

| 顺序 | 规则 |
| --- | --- |
| 1 | 53 砧垫（致命伤保留） > 74 崩砧（绝境一击） > 其它保命 / 免死 |
| 2 | 48 砧裂生效期间**屏蔽**所有 C3「消耗 `live`」条目（与条目原文一致） |
| 3 | 89 沉星霸体 > 26 铁律 / 54 锻隙韧性（免疫优先于缩短 / 减伤） |
| 4 | 互斥组 M1 / M2 在抽取阶段即过滤，不进入触发链 |
| 5 | 季节未开放的 `SEASONAL` 条目在触发链入口短路，优先于以上所有规则 |

**落地**：一张配置表（组 id / 规则类型 / 成员 / 上限 / 优先级）+ `EnchantButton` 抽候选时过滤 M 组 + 触发链统一读取 C 组与优先级。不在每条附魔里各写 `if`。

---

## 三、季节失效的 lore 呈现（R8）

**依据**：`base/.../item/AbstractPitItem.java:55-75` 的 `getEnchantLore` 在每次 `toItemStack()` 时动态生成 lore；`:60` 已对 `DISABLED` / `REMOVED` 加 `&m` 删除线，`:71-73` 追加「此附魔暂时被管理员停用」。PDC 只存 `ench`，lore 不落盘。

| 呈现 | 未开放的 `SEASONAL` 条目 |
| --- | --- |
| 名称行 | `&8&m` + 前缀 + 名称（复用 `DISABLED` 删除线） |
| 描述行 | 整体 `&8`，不显示 `&7` 数值 |
| 追加行 | `&7当前季度未开放 &8| <watermark>` |
| 不改 | PDC `ench` / `live` / `tier`、物品名称、颜色、材质 |

- 「失效」由触发链入口短路（Q13），「置灰」只做展示；两侧共用 `isSeasonOpen(enchant)` 纯函数（`Asia/Shanghai`、自然月边界、`seasonal-override`），避免判定漂移。
- 不改物品数据的理由：见提交 `10fd865a`（附魔神话物品载入时被销毁）与 `MIGRATION_PAPER_1.21.md:142`（PDC 存量数据兼容风险）。

---

## 四、抽取集成点（`EnchantButton`）

> 完整裁定见 `MYTHIC_HAMMER_RULINGS.md` R6，此处只列与状态注册中心的接口。

| 接口 | 位置 | 作用 |
| --- | --- | --- |
| `seasonalResults` 独立构建 | `EnchantButton.java:319` 之后、`:326` 之前 | 从 `list` 筛 `SEASONAL` + 当前季度开放 + 通过 `isBlackList` |
| 覆盖式季节判定 | `:350-400` switch 抽取完成后 | `seasonal-chance` = 0% / 5% / 10%（物品阶 tier 1 / 2 / 3），不改动既有随机序列 |
| 去重后写入 | `:509-511` `removeExistingEnchantments` | 避免重复词条；等级按阶 1 / 2 / 3 |
| 卷轴路径 | `getBookRareResults` (`:475-495`) 旁新增 `getBookSeasonalResults` | 只在当前季度开放集合内产出；跨季卷轴按 Q13 失效 |
| 广播 | `:456` 同级 | 季节条目产出走稀有附魔同级公告 |

- 获率不进 `custom.yml rate`；`seasonal-chance` 为独立配置节。
- 互斥组 M1 / M2 的过滤也挂在 `:347-348` 的候选过滤处，与 `isBlackList` 并列。

---

## 五、校验与口径

| 校验项 | 结果 |
| --- | --- |
| 状态是否全部有 id 与清除规则 | 是（14 个 id） |
| 新造状态数 | 7（其余 7 项复用现成实现或语义） |
| 互斥是否只在抽取阶段过滤 | 是（M1 / M2） |
| 攻速是否只有一个 modifier | 是（C2 求和后 clamp） |
| `live` 单 tick 消耗是否封顶 | 是（2 点） |
| 季节置灰是否改写 PDC | 否 |
| `SEASONAL` 是否实现 `ILimit` | 否 |
| 是否含实现代码 | 否 |

- 本文件为设计定稿，不含实现代码与代码片段；引用的类名与行号仅用于定位，落地时对照 Paper 1.21.8 API 复核。
- 文档仅留本地工作区，未提交、未推送。

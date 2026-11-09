# 《神话之锤：自定义附魔设计（100 条）》审查疑问答复

> 答复对象：`docs/MYTHIC_HAMMER_ENCHANTS_QUESTIONS.md`（共 27 条）。
> 核对基准：当前磁盘版 `docs/MYTHIC_HAMMER_ENCHANTS.md`（含格式化痕迹：列宽对齐、HTML 标签混用、转义残留；本文行号以该版本为准）与仓库当前代码。
> 性质：设计口径裁定与事实核对，不含实现，不含代码片段。
> 本文分五部分：①结论摘要 ②逐条答复 ③原文档回写清单 ④落地依赖与风险 ⑤口径说明。

---

## 一、结论摘要

| 分组 | 条数 | 成立 | 部分成立 / 确认 | 不成立 |
| --- | --- | --- | --- | --- |
| A 文档与代码不符 | 4 | 4（Q1 含数据更正与口径澄清） | 0 | 0 |
| B 缺失定义 | 6 | 5 | 1（Q8） | 0 |
| C 季节性机制 | 3 | 2 | 0 | 1（Q12） |
| D 数据 / 一致性 | 6 | 5 | 1（Q19 确认口径） | 0 |
| E 机制可行性 | 5 | 5 | 0 | 0 |
| F 格式 / 渲染 | 3 | 2 | 1（Q27） | 0 |
| **合计** | **27** | **23** | **3** | **1** |

**优先裁定顺序**（采纳审查建议并调整）：

1. Q1 → 季节性枚举与绿色前缀落地方式。
2. Q4 + Q10 → 锤的类型识别链。核对发现比提问更严重：`WitherCraftKillStreak` 硬编码了剑 / 弓 / 护腿，锤目前**连修复都走不通**。
3. Q5 → 锤本体规格。
4. Q11 → 季节判定口径。
5. Q12 已由需求原文裁定为设计意图，不再列为待议项（详见 Q12）。

**四条总裁定**

- 文档中的「普通 / 稀有! / 季节性!」是**文档展示标签**（输出约束），不是 `EnchantmentRarity` 枚举的镜像。§1.2 把父类型挂到枚举名下是笔误，须回写。
- 「池」是文档侧抽象，`custom.yml` 的 `rate` 只有 dark / normal / rage / sewers 四条场景权重；稀有度由 `EnchantmentRarity` 决定，季节性走独立候选集过滤，两者都不新增 `rate` 池。
- 本次文档范围仅为 100 条附魔 + 统计 + 开放表 + 设计说明；锤本体、术语表、状态系统属实现前置定义，另立文档，不回塞进清单。
- 10—12 月全 10 条是需求硬性指定的年度复刻窗口，不是笔误。

---

## 二、逐条答复

### A. 文档与代码不符

**Q1 — 类别标签与 `EnchantmentRarity`** ｜ **成立（附 2 处更正）**

- 依据：`base/.../enchantment/rarity/EnchantmentRarity.java:7-38` 实际 **21 个**枚举值（脚本枚举计数：`NORMAL`…`NOSTALGIA_RARE`），审查意见的「23 个」多计 2 个；`RarityType` 才是 `NORMAL / RARE / LIMITED` 三值（同文件 `:62-66`）。枚举内确无季节性类别与绿色前缀；限时前缀为 `OP("&c限时! ", LIMITED, 14)`（`:28`）。
- 口径澄清：§1.3「类别标签仅三种」是本文档的输出约束，与代码枚举并非同一层面，本身不构成错误。错误在 §1.2（L24）把 `RarityType` 的三值写成 `EnchantmentRarity` 的属性。
- 裁定：**新增枚举 `SEASONAL("&a季节性! ", RarityType.LIMITED, …)`，不复用 `OP`**。理由：`OP` 前缀为红色 `&c限时! `，语义为运营限时，与季度轮换不同；复用会让客户端展示与开放判定共用错误语义。`itemColor` 取未占用值。
- 映射：普通 → `NORMAL` 系列（含 `DARK_NORMAL` / `SEWER_NORMAL` / `FISH_NORMAL` 等按场景），稀有! → `RARE` 系列，季节性! → 新增 `SEASONAL`，归 `LIMITED`。

**Q2 — `rate` 无 `rare` 池** ｜ **成立**

- 依据：`core/src/main/resources/custom.yml:129-217`，结构为 `rate.{1,2,3}.{dark,normal,rage,sewers}.{vip,default}.{permission,value}`，无 `rare`。§1.2（L24）写成「normal / rare / dark / sewers」有误，`rage` 被写成 `rare`。
- 裁定：**不新增 `rare` 池**。稀有度由 `AbstractEnchantment#getRarity()` 决定，抽取时按枚举过滤——见 `core/.../enchantment/menu/button/EnchantButton.java:327-344`（`NORMAL` / `RARE` / `DARK_RARE` / `RAGE_RARE` / `SEWER_RARE` 分流）与 `:482-494`（卷轴稀有池）。`rate` 只控制按阶 / 场景的产出权重。
- 若确需按阶调节稀有权重，另开独立配置节（如 `rare-weight`），不改 `rate` 语义。

**Q3 — 「限时池」不存在** ｜ **成立**

- 依据：同 Q2，`rate` 无该 key。§1.2（L24）、§2（L43）的「限时池」是文档侧抽象表述。
- 裁定：季节性 10 条**走独立发放逻辑**：在 `EnchantButton` 构建候选集前，按当前季度过滤 `SEASONAL` 条目（未开放者直接不入池）；已持有物品的使用侧在同一判定函数上做开关。不挂 `rate` 任何池，避免与场景权重耦合。

**Q4 — 无 `HammerOnly`** ｜ **成立**

- 依据：`base/.../enchantment/param/item/` 下仅 `ArmorOnly` / `BowOnly` / `ItemOnly` / `RodOnly` / `WeaponOnly` 五个；`base/.../compat/item/ItemTypes.java:19,23,28` 只有 `isSword` / `isBow` / `isArmor`；`base/.../enchantment/AbstractEnchantment.java:69-83` 的 `canApply` 按这三类 + `FISHING_ROD` 分支。
- 裁定：**新增 `HammerOnly`**，不用 `WeaponOnly` 凑合（会把 100 条锤附魔暴露给剑）。配套改动清单：`ItemTypes#isHammer`（用 `Material` 分类 / `Tag.ITEMS_AXES`，与 `MIGRATION_PAPER_1.21.md:97` 的 1.21 方案一致）、`AbstractEnchantment#canApply` 增加分支、以及 Q10 列出的硬编码物品类型处。

### B. 缺失定义

**Q5 — 锤本体未定义** ｜ **成立（属范围外，非遗漏）**

- 原始需求只要求 100 条附魔、统计表、开放表与设计说明；仓库内目前**没有**锤本体定义文档。本次仍不实现，建议另立 `docs/MYTHIC_HAMMER_ITEM.md`。
- 规格草案（供另立文档采纳，非承诺）：

| 项 | 建议 | 参照 |
| --- | --- | --- |
| Material | `GOLDEN_AXE` | `MythicSwordItem` 用 `GOLDEN_SWORD`；1.21 斧自带攻击冷却，符合「重」 |
| 基础伤害 / 攻速 | 8.0 / `ATTACK_SPEED` 0.7 | 剑为 6.5（`MythicSwordItem.java:30-32`）；攻速需扩 `ItemBuilder`（见 Q23） |
| 双手武器 | 1.21 无原生双手机制；用「副手有物时禁止锤附魔触发」的自定义约束 | — |
| I / II / III 阶 | 复用 `IMythicItem.tier` + `RomanUtil.convert(tier)` | `IMythicItem.java:104-108` |
| 生命属性 | 复用 `live / maxLive` 与 60% / 30% 阈值 | `IMythicItem.java:178-179` |
| 获取 | 击杀掉落，沿用 `mythicDropChance`（0.01 / 0.012） | `custom.yml:218-224`、`MythicEnchantingTable.java:32` 的物品 lore |
| 附魔 | 神话之井 | `MythicWellMenu` / `EnchantButton` |

**Q6 — 攻击术语未定义** ｜ **成立**

- 补一份术语表，实现前写入锤本体文档：

| 术语 | 语义 | 1.21 实现口径 |
| --- | --- | --- |
| 重击 | 攻击冷却已满（进度 1.0）时的一次近战命中 | 读取攻击冷却进度；不得用「蓄力」字面实现 |
| 命中 | 一次近战攻击判定到至少一个实体 | `EntityDamageByEntityEvent` 且 damager 为主手锤 |
| 收锤 | 自上次攻击到冷却重新满的间隔 | 冷却进度 0→1 的时长，按 `ATTACK_SPEED` 换算 |
| 挥空 | 一次攻击发起但未命中任何实体 | 攻击事件与伤害事件配对差集；需自定义计数 |
| 前摇 | 发起到伤害判定的间隔 | 1.21 无该属性；**合并进攻击冷却，不自造第二套计时**（见 Q23） |
| 攻速 | 每秒可完成的满蓄力攻击次数 | `Attribute.ATTACK_SPEED` + `EquipmentSlotGroup.MAINHAND` |
| 冲刺 | 疾跑状态 | `Player#isSprinting()`，与 1.9+ 的「暴击 / 冲刺攻击」区分 |

- 具体 API 名以 Paper 1.21.8 javadoc 为准，落地时逐项验证；本表只固定语义边界。

**Q7 — 状态 / 机制是否已有实现** ｜ **成立**

| 机制 | 现状 | 可验证依据 |
| --- | --- | --- |
| 眩晕 | 复用 | `core/.../enchantment/type/recode/rare/Unpredictably.kt:50-51`，眩晕 = 移速与攻击伤害大幅降低，2 秒，20 秒冷却 |
| 护盾层 | 复用 | `core/.../enchantment/type/sewer_normal/AegisEnchant.java:68,80,100`，每 9 秒 1 层，抵消 1 次玩家伤害 |
| 生命吸收 / 金心 | 复用 | `PlayerListener.java:481,498,513,542,557,584` 的 `setAbsorptionAmount`；`addon_config.yml:11` 的 `LimitAbsorptionHearts` |
| 减益 / 增益 | 复用 | `base/.../cn/charlotte/pit/buff/` 的 `BuffData` / `AbstractPitBuff` / `BuffFactory` 与 `HealPoisonDeBuff`、`HemorrhageDeBuff` 等实现；另有 `PotionEffect` |
| 连杀 | 复用 | `PitStreakKillChangeEvent`、`WitherCraftKillStreak.java:100-111` |
| 暴击判定 | 复用判定，新增抑制钩子 | `GameEffectListener.java:429-432`、`PitHook.kt:422-423` |
| 禁止冲刺 | 本次核对未发现现成实现 | 新造：`PlayerMoveEvent` + `setSprinting(false)`，或用高等级缓慢近似 |
| 锁定物品栏切换 | 本次核对未发现现成实现 | 新造：监听 `PlayerItemHeldEvent` 回滚 |
| 压制暴击 | 本次核对未发现现成实现 | 新造：伤害管线加暴击抑制标记 |
| 命中率下降 / 视野遮蔽 | 本次核对未发现现成实现 | 降级为粒子与 `DARKNESS`，不改客户端判定（见 Q20） |

**Q8 — 「锤印」未定义** ｜ **部分成立**

- 更正：当前文档 L71 的第 10 条用的是「**铆钉标记**」，不是锤印；引用锤印的只有 L77（16 号）与 L172（100 号）。
- 裁定：锤印定义为**通用标记 debuff**，统一参数：上限 3 层、持续 8 秒、切换目标清空、第 3 层触发后清空；由 16 号定义并在 §1.3 增设「公共机制」小节说明。100 号引用同一 id，不另造层数。
- 10 号「铆钉标记」保留为**独立 id 的另一标记**（效果是钝器易伤），与锤印同挂一个标记注册中心，避免各附魔自造。
- 16 号阶适配「1 / 2 / 3 层」需明确为**触发阈值**（I / II / III 阶分别在第 3 / 2 / 1 层触发），否则与「上限 3 层」冲突；列入回写清单。

**Q9 — 「神话之井」是否即现有附魔台** ｜ **成立（且 §1.1 措辞需修正）**

- 确认为现有同一入口，但类名不是 `MythicEnchantTable`：`core/.../item/type/MythicEnchantingTable.java:14`（附魔台物品，显示名 `&d神话之井`）、`core/.../enchantment/menu/MythicWellMenu.java:45,83`（菜单标题「神话之井」）、`core/.../enchantment/menu/button/EnchantButton.java`（抽取逻辑）。另有 `core/.../menu/admin/item/button/EnchantButton.java:32` 是管理端按钮，勿混淆。
- 措辞问题：§1.1（L4）写「产出入口：神话之井」，把「附魔入口」与「物品产出」混为一谈。神话物品本体由击杀掉落（`MythicEnchantingTable.java:32` lore、`custom.yml:218`），神话之井只负责附魔。应回写。

**Q10 — 凋灵工艺修复 `live` 是否现成** ｜ **成立，且发现更严重缺口**

- 现成机制共三套：`WitherCraftKillStreak.java:140-146`（消耗 1 暗聚块，+1 生命）、`GrandFinaleMegaStreak.java:150`（+4 生命）、`MythicRepairKit.java:90`（直接 `setLive(maxLive)`）；addon 侧另有 `FreeCE.kt:51-64` 的暗聚块修复。常规消耗见 `Utils.java:298`。
- **关键缺口**：`WitherCraftKillStreak.java:122-128` 硬编码 `isSword` → `MythicSwordItem`、`isBow` → `MythicBowItem`、`isArmor && mythic_leggings` → `MythicLeggingsItem`，`mythicItem == null` 直接 return（`:130-132`）。**锤放不进这条修复链**。
- 裁定：锤的 `live` 走同一套修复语义，但必须补齐类型识别分支（与 Q4 的 `ItemTypes#isHammer` 一并改）。43「回炉」、49「补锭」、64「铸缝」、80「铸魂」等补 `live` 的条目是**附魔侧补充**，不能替代修复链；两者并存，且都需 clamp 到 `maxLive`。

### C. 季节性机制

**Q11 — 季节性如何判定开放** ｜ **成立（缺失）**

- 裁定：**服务端真实日期自动判定**，不读客户端时间。
- 时区：固定 `Asia/Shanghai`（与 `custom.yml:8` 的 `dateFormat` 同为服务器本地口径），在配置中显式声明，避免跨区服漂移。
- 边界：按自然月，每季度首日 00:00:00 生效、末日 23:59:59 后失效；判定用「当前月份 → 季度 → 开放编号集合」的纯函数，不依赖重启。
- 手动开关：新增 `seasonal-override`（`auto / force-open / force-close` + 编号白名单），仅用于测试与活动，生产默认 `auto`。可复用 `ConfigManager` 已有的 `startDate + duration` 轮转思路（`MIGRATION_PAPER_1.21.md:49`），但不改 `rate`。

**Q12 — Q4 与前三季度重叠** ｜ **不成立（需求原文，非笔误）**

- 依据：需求明确写「10—12 月为全部 10 个」；当前文档 L187 即按此落地，L195 校验「10—12 月为全部 10 条」，L193 只校验「**前三季度**子集两两无交集」——措辞已把 Q4 排除在互斥约束之外。
- 语义：前三季度是**首放窗口**，10—12 月是**年度复刻窗口**。开放表的每一行是「该季度的可用编号集合」，逐季度独立声明，不要求跨季度互斥。因此 91–93 在 Q1 与 Q4 各开放一次不构成矛盾。
- 「清单无重复」指的是 100 条清单内编号唯一，与跨季度重复开放无关；「月份安排无矛盾」指月份区间不重叠（L197），当前满足。
- 「若笔误 Q4 应开哪 3 条」不适用：10 条为硬性要求，无替代子集。

**Q13 — 未开放季节对已持有条目的处理** ｜ **成立（缺失定义）**

| 项 | 裁定 |
| --- | --- |
| 词条本身 | **保留但置灰**，不隐藏、不删除、不改写 PDC 数据 |
| 效果 | 失效：在附魔触发链入口统一按「`SEASONAL` 且当前季度未开放」短路，不进入伤害 / 状态 / 结算 |
| `live` | 不变：失效期间不消耗、不回复；已扣的不追溯返还 |
| 产出 | 不入候选池（`EnchantButton` 过滤），附魔卷轴同样不可获得 |
| 理由 | 避免季节切换时批量改写物品数据。仓库有「附魔神话物品在载入时被销毁」的历史修复（提交 `10fd865a`），此类数据改写风险高 |

### D. 数据 / 一致性

**Q14 — 「稀有! 以 II / III 阶为主」与数据不符** ｜ **成立**

- 依据：L42 写「以 II / III 阶为主」，但 L138–156（71–89）全部给出 I / II / III 三档，仅 L157（90 绝契）为「仅 III 阶」。
- 裁定：**允许 I 阶**。回写为「I / II / III 阶均可，其中 90 号仅 III 阶」。

**Q15 — 24「锭心」两字段** ｜ **成立（定性为语义混用，非单纯冗余）**

- 依据：L85 效果写「自身生命值满时增伤，掉血即失效」，适配列写「依赖 `live = maxLive`，掉血后需修复才能恢复」。§1.3（L32）已明确「生命值 = 玩家血量、`live` = 物品耐久」是两套概念，此处把两者等同，属语义混淆。
- 裁定：统一到物品 `live` 语义。效果改为「`live = maxLive` 时增伤，`live` 低于上限即失效」；适配列改为「不消耗 `live`；`live` 下降后需修复才能重新生效」。

**Q16 — 45「欠火」高阶反向收益** ｜ **成立**

- 依据：L106 伤害惩罚 -5% / -8% / -11% 递增，回复固定 0.25 ❤。
- 裁定：回复随阶递增为 **0.25 / 0.5 / 0.75 ❤**；惩罚与收益同阶线性，III 阶净收益不劣于 I 阶。

**Q17 — 51「缓锤」同理** ｜ **成立**

- 依据：L112 攻速惩罚 -4% / -6% / -8% 递增，伤害 +6% 固定。
- 裁定：伤害增益改为 **+6% / +9% / +12%**，与惩罚同阶递增。

**Q18 — 56「铁账」数值异常** ｜ **成立**

- 依据：L117 为「1 / 2 / 3 枚」+「少量金币」，单位含糊、量级过低。
- 裁定：单位统一为**硬币**（仓库口径见 `PitGainCoinsEvent`、`addon_config.yml:47` 的「%coin%硬币」），效果改为「每 10 次命中额外获得 30 / 60 / 90 硬币」。此为暂定值，落地前须对照 `GoldDropRunnable` 与击杀收益做一次经济回归；审查建议的 100 / 200 / 300 量级偏高，先按保守值上线。

**Q19 — 校验口径** ｜ **确认：是，去重编号数**

- L52 的「开放表涉及编号数」= 去重后的 91–100 共 10 个。3+3+4+10 = 20 是「季度 × 条目」出现次数，属另一口径，不参与该校验。
- 建议把 L52 文案改为「去重编号数」以消除歧义（列入回写清单）。

### E. 机制可行性

**Q20 — 视野 / 渲染类** ｜ **成立**

| 条目 | 呈现方式 | 依据 |
| --- | --- | --- |
| 50 秤星（`live/maxLive`） | ActionBar 文本，命中后显示 2 / 3 / 4 秒 | `MIGRATION_PAPER_1.21.md:77`，1.21 已迁 `Player#sendActionBar` |
| 57 落点尺 | 自身可见的粒子圆环，半径按阶 2 / 3 / 4 格 | `MIGRATION_PAPER_1.21.md:75`，粒子已迁 `Particle` + `World#spawnParticle` |
| 68 落砂 | 脚下粒子簇 + 短时 `DARKNESS`；**不改客户端渲染距离** | 同上；避免发包改视野 |

- 一律不用记分板 / 聊天栏刷屏；BossBar 仅在需要持续指示时备用。

**Q21 — 地形 / 方块类** ｜ **成立**

- 裁定：**全部用「粒子 + 服务端逻辑区域」，不放真方块**。区域用中心点 + 半径 + 到期时间记录，按 tick 判定归属，重启即失效。
- 逐条：72 地脉 → 地面粒子环 + 区域内增伤标记；84 断层 → 逻辑区域 + `PlayerMoveEvent` 拦截通行（或高等级缓慢 + 击退），不生成方块；92 抽芽 → 草地粒子 + 区域内回复速度标记；94 骤雨 → 柱状粒子 + 区域内「下一击附带冲击」标记。
- 理由：真方块会污染主世界、与地图轮换 / FAWE 清理冲突；`MIGRATION_PAPER_1.21.md:144` 已记录真实实体与启动清理的冲突风险，方块同理。ProtocolLib 发包层按 `MIGRATION_PAPER_1.21.md:114, :147` 尚未重写、启动即有异常风险，不宜新增依赖。

**Q22 — 35「范线」命中范围** ｜ **成立**

- 客户端近战 3 格判定服务端改不了，ProtocolLib 拦截 `InteractEntity` 不能放宽客户端侧判定。
- 裁定：降级为**服务端补偿判定**——在 3.0–(3.0+阶档) 格内对视线方向做一次射线目标判定并补一次伤害结算；若该方案与反作弊冲突，直接改条目为「对 2.5 格内目标伤害 +X%」。不保留「扩大客户端命中范围」的字面描述。

**Q23 — 攻速 / 前摇 / 攻击间隔** ｜ **成立**

- 依据：`base/.../util/item/ItemBuilder.java:393-397` 目前只写 `Attribute.ATTACK_DAMAGE` + `EquipmentSlotGroup.MAINHAND`，无 `ATTACK_SPEED`；`MIGRATION_PAPER_1.21.md:70` 记录属性迁移方案，`:143` 记录「重复 `addAttributeModifier` 会覆盖」风险。
- 裁定：23 摇臂 / 51 缓锤 / 65 短砧**统一走 `Attribute.ATTACK_SPEED` 的 `AttributeModifier`**，扩展 `ItemBuilder`；用固定 `NamespacedKey` + 先 `removeAttributeModifier` 再写入，避免覆盖。
- 65「前摇」在 1.21 无对应属性，合并进同一套攻击冷却，不自造第二套计时；若视觉上仍要区分，用动画 / 粒子而非独立计时器。

**Q24 — 状态系统依赖** ｜ **成立**

- 有基础：`base/.../cn/charlotte/pit/buff/`（`BuffData`、`AbstractPitBuff`、`BuffFactory`、6 个 Buff / DeBuff 实现）+ `PotionEffect` 包装 + `PlayerProfile` 连杀数据。
- 裁定：**新建一个统一状态注册中心**（可挂在 `buff` 包下），为眩晕、禁冲刺、锁物品栏、压制暴击、护盾层、锤印 / 铆钉标记分配统一 id、时长、叠层与清除规则；每条附魔只声明 id 与参数，不各自持有 `HashMap<UUID, Cooldown>`。
- 收益：Q7 中新造的 3 项状态（禁冲刺、锁物品栏、压制暴击）收敛为一次性基础设施工作；第 4 项（命中率 / 视野）按 Q20 降级为粒子表现，不进状态系统。季节开关（Q13）也能在同一入口短路。

### F. 格式 / 渲染

**Q25 — 转义符残留** ｜ **成立**

- 依据：当前 L5 为 `④\*\*<span …>季节性!</span>\** 附魔开放表`，含字面 `\*\*` 与 `\**`。
- 裁定：清理，与 Q26 一并统一处理。

**Q26 — HTML 标签混用** ｜ **成立**

- 依据：脚本统计 `<span` 出现 4 次（L4、L5、L43、L52，均为 `style="color:rgb(69, 176, 118)"`）、`<font` 出现 13 次（L30、L159、L163–172、L176 等，均为 `color="green"`）；L159 标题里 `### 3.3 **<font …>**` 为 HTML + 粗体嵌套。
- 裁定：**统一为 `<font color="green"><b>季节性!</b></font>`**，全文替换（含标题与表格）。理由：`<font color>` 在 GitHub / Obsidian / Typora 的兼容性强于 `style` 属性（GitHub 会剥离 `style`）；`<b>` 比 `**` 包裹 HTML 标签更可靠地产生加粗。
- 降级规则：若目标预览器剥离颜色，退化为 `**季节性!**`，并在 §1.3 注明「颜色为呈现层，不影响标签文本 `季节性!`」。
- 标题建议改为纯文本 `### 3.3 季节性!（91–100）`，标题内不放 HTML。

**Q27 — 表格对齐与空格** ｜ **部分成立**

- 不成立部分：全文**行尾空格**只有 L3、L4 两处，且是 Markdown 硬换行所需的两个空格，属正常；表格行没有行尾空格。
- 成立部分：L21–25、L39–52、L62–131 存在**列宽对齐填充空格**导致的单元格宽度不一致，后半段（L136 起）为紧凑写法，两种风格混排。
- 裁定：统一清洗为紧凑 Markdown 表格（不做列宽对齐），并关闭编辑器的 Markdown 表格自动格式化，避免回写后再次引入。

---

## 三、原文档回写清单

> 以下为建议替换文本，本次答复**不直接修改** `MYTHIC_HAMMER_ENCHANTS.md`，待裁定确认后统一回写。

| 位置 | 现状 | 替换为 | 关联 |
| --- | --- | --- | --- |
| L4 文首引用块 | 「产出入口：神话之井」 | 「附魔入口：神话之井（`MythicEnchantingTable` + `MythicWellMenu` + `EnchantButton`）；物品本体由击杀掉落」 | Q9 |
| L4、L5、L30、L43、L52、L159、L163–172、L176 | `<span style=…>` 与 `<font>` 混用 | 统一 `<font color="green"><b>季节性!</b></font>`；L159 标题改纯文本 | Q25、Q26 |
| L5 | `\*\*` 与 `\**` | 删除反斜杠，按上一行统一写法 | Q25 |
| L17 §1.1 | 「可由凋灵工艺一类机制修复」 | 补注「修复链按物品类型硬编码，锤需新增 `isHammer` 分支」 | Q10 |
| L24 §1.2 | 「`rate`（1 / 2 / 3 阶 × normal / rare / dark / sewers 权重）、`EnchantmentRarity`（NORMAL / RARE / LIMITED）」 | 「`custom.yml rate.{1,2,3}.{dark,normal,rage,sewers}.{vip,default}` 的场景权重；`RarityType` 为 NORMAL / RARE / LIMITED，`EnchantmentRarity` 共 21 个枚举值」 | Q1、Q2 |
| L24 §1.2 右列 / L41–43 §2 池说明 | 「70 / 20 / 10 对应普通池 / 稀有池 / 限时池」 | 「70 / 20 / 10 为文档展示标签；稀有度由 `EnchantmentRarity` 决定，季节性走独立候选集过滤，均不占 `rate` 池」 | Q2、Q3 |
| L42 §2 稀有! 池说明 | 「以 II / III 阶为主」 | 「I / II / III 阶均可，90 号仅 III 阶」 | Q14 |
| L52 §2 校验项 | 「开放表涉及编号数」 | 「开放表去重编号数」 | Q19 |
| §1.3 新增 | — | 增「公共机制」小节：锤印（3 层 / 8 秒 / 切目标清空 / 第 3 层清空）、铆钉标记为独立 id、季节开放判定与失效口径 | Q8、Q11、Q13 |
| L85（24 锭心） | 「自身生命值满时增伤，掉血即失效」 | 「`live = maxLive` 时增伤，`live` 低于上限即失效」；适配列改「不消耗 `live`，下降后需修复才能重新生效」 | Q15 |
| L106（45 欠火） | 回复固定 0.25 ❤ | 回复 0.25 / 0.5 / 0.75 ❤ | Q16 |
| L112（51 缓锤） | 伤害 +6% 固定 | 伤害 +6% / +9% / +12% | Q17 |
| L117（56 铁账） | 「1 / 2 / 3 枚…少量金币」 | 「每 10 次命中额外获得 30 / 60 / 90 硬币（暂定，需经济回归）」 | Q18 |
| 全文表格 | 列宽对齐填充空格 | 紧凑 Markdown 表格，关闭自动格式化 | Q27 |
| L77（16 锤印） | 阶适配「1 / 2 / 3 层」 | 「触发阈值：第 3 / 2 / 1 层」，另在公共机制小节写明上限 3 层 / 8 秒 | Q8 |

**不回写项**：开放表 L184–187（Q12 判定为设计意图）、100 条清单的编号与名称（保持不变）。

---

## 四、落地依赖与风险

| 优先级 | 依赖 | 说明 |
| --- | --- | --- |
| P0 阻塞 | Q1 枚举与前缀 | 不新增 `SEASONAL` 则季节性条目无展示与判定载体 |
| P0 阻塞 | Q4 + Q10 类型识别链 | `ItemTypes#isHammer`、`AbstractEnchantment#canApply`、`WitherCraftKillStreak:122-128`、`MythicRepairKit` 需一并改；否则锤既不能正确附魔也不能修复 |
| P0 阻塞 | Q5 锤本体规格 | 无 Material / 伤害 / 攻速则 Q6 术语表与所有数值无法验证 |
| P0 阻塞 | Q11 季节判定 | 自动判定 + 时区边界 + `seasonal-override` 配置 |
| P1 | Q24 状态注册中心 | 收敛 Q7 的 4 项新造状态，避免 100 条附魔各自持有冷却表 |
| P1 | Q23 攻速属性 | 扩 `ItemBuilder`，处理 `AttributeModifier` 覆盖风险 |
| P1 | Q20–Q22 降级方案 | 粒子 / 逻辑区域 / 服务端补偿判定，不引入发包依赖 |
| P2 | Q14–Q18 数值回写 | 纯文档修订，不影响结构 |
| P2 | Q25–Q27 格式清洗 | 纯文档修订 |

**已有风险（沿用 `MIGRATION_PAPER_1.21.md` 的判定，不重复展开）**

- `:142` 存量 1.8 物品无 PDC 数据，`ItemUtil#isIllegalItem` 会判非法——新增锤若读入旧存档数据需一并验证。
- `:147` ProtocolLib 1.8 wrapper 已不存在，`PacketListener` 未重写前启动即异常；Q21 / Q22 的降级方案刻意不新增发包依赖。
- `:143` 重复 `addAttributeModifier` 覆盖问题，见 Q23。
- 提交 `10fd865a`（附魔神话物品载入时被销毁）提示：Q13 的「保留词条、只做触发短路」比「季节切换时改写物品数据」更安全。

---

## 五、口径说明

- 「成立」= 与当前磁盘版文档或仓库代码一致；「部分成立」= 核心结论对但含事实偏差；「不成立」= 与需求原文或设计意图冲突。
- 核对方式为逐文件 / 逐行比对，行号以当前磁盘版为准；代码引用只列类名、配置键与方法名，不含实现代码。
- 本答复只裁定设计口径与实现路线，不构成实现承诺；Q5 的规格草案与 Q18 的数值待另立文档 / 经济回归确认。
- 文档仅留本地工作区，未提交、未推送。

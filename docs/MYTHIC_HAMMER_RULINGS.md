# 神话之锤附魔设计：R1–R8 最终裁定

> 对象：`MYTHIC_HAMMER_ENCHANTS_ANSWERS.md` 遗留的 8 个开放项（R1–R8）。
> 基准：当前磁盘版三份文档与仓库代码；所有结论均附文件与行号依据。
> 本次为**定稿**，不再保留「草案 / 暂定 / 待后续」表述。同时产出三份前置规格文档：`MYTHIC_HAMMER_ITEM.md`（锤本体）、`MYTHIC_HAMMER_TERMS.md`（术语表）、`MYTHIC_HAMMER_STATUS.md`（状态与冲突规则）。
> 本文分六部分：①裁定总表 ②逐条最终裁定 ③对上一轮答复的更正 ④定稿参数汇总 ⑤阻塞项状态 ⑥口径说明。

---

## 一、裁定总表

| 编号 | 结论 | 一句话裁定 |
| --- | --- | --- |
| R1 | 成立，且上一轮答复有误 | `isHammer` 判 **PDC `internal` = `mythic_hammer`**，不是材质家族；普通斧不吃这 100 条 |
| R2 | 成立 | 55 钝鸣改为「对他人造成的伤害降低」，68 落砂保留视野遮蔽；两者拆为不同状态 id |
| R3 | 成立，本次定稿 | 锤本体、术语表、状态注册中心三份文档同时产出，数值不再悬空 |
| R4 | 成立 | `SEASONAL("&a季节性! ", LIMITED, **10**)`，`itemColor = 10` |
| R5 | 成立，本次完成回归 | 56 铁账终值 **5 / 10 / 15 硬币**（每 10 击），经济回归本次裁定即完成 |
| R6 | 成立 | `seasonalResults` 独立构建、覆盖式挂载、I/II/III 阶 0% / 5% / 10%；**SEASONAL 不实现 `ILimit`** |
| R7 | 成立 | 新增数据驱动的「互斥 / 叠加封顶 / 优先级」规则节，不逐条硬编码 |
| R8 | 成立 | 复用 `getEnchantLore` 的 `DISABLED` 置灰模式动态生成 lore，**不改写 PDC** |

---

## 二、逐条最终裁定

### R1 — `isHammer` 判定口径

**依据**

- `base/.../compat/item/ItemTypes.java:44-46`：`isAxe(stack)` = `name.endsWith("_AXE")`，**所有斧都会命中**；该类所有方法均为材质名判定。
- `core/.../enchantment/menu/MythicWellMenu.java:291-292`：神话之井放入校验已是 **PDC internal name 白名单**（`mythic_sword` / `mythic_bow` / `mythic_leggings` / `mythic_reel`）。
- `base/.../util/item/ItemUtil.java:161-163`：`getInternalName` = `ItemData.getString(item, KEY_INTERNAL)`，即 PDC。
- `core/.../enchantment/menu/button/EnchantButton.java:314-319`：附魔候选集 `list` 由 `canApply(item)` 过滤。
- `AbstractEnchantment.java:69-83`：`canApply` 目前只走材质分类 + 钓竿。

**裁定：判「神话之锤物品（PDC 标记）」，不判材质家族。**

| 改动点 | 内容 |
| --- | --- |
| `ItemTypes` | 新增 `isMythicHammer(ItemStack)`，精确匹配 `mythic_hammer`；`isAxe` 保留为通用工具方法，**不用于附魔判定** |
| `AbstractEnchantment#canApply` | 首判 `isMythicHammer` → 只放行带 `HammerOnly` 的附魔；未命中再回退现有材质分支 |
| `MythicWellMenu.java:292` | 白名单加 `mythic_hammer`（**P0**：不加则锤放不进神话之井） |
| `menu/admin/item/button/EnchantButton.java:55-56` | 管理端类型校验按 internal name 补锤 |
| `WitherCraftKillStreak.java:122-128`、`MythicRepairKit`、`ItemFactory` | 按 internal name 补锤分支（Q10 缺口一并解决） |

- 普通斧：**不能**吃这 100 条。既被 `canApply` 的 internal name 分支拦截，也没有 PDC `ench` 数据供 `IMythicItem` 解析出词条。
- 撤回上一轮 Q4 的「用 `Tag.ITEMS_AXES`」：仓库已有 `isAxe` 且为材质名后缀判定，该建议既与现状不符，也会引入原版斧混入。

### R2 — 55「钝鸣」与 68「落砂」拆分

**依据**：`core/.../enchantment/type/limit/Winter.java:64` 已有「霜寒：移速大幅降低，**对他人造成的伤害 -20%**」，`:116` 以 `event.setDamage(event.getDamage() * 0.8)` 实现。Minecraft 无 miss 机制。

| 条目 | 定稿效果 | 实现口径 |
| --- | --- | --- |
| 55 钝鸣 | 命中时对目标周围至多 1 / 2 / 3 名敌人施加 2 / 3 / 4 秒「**对他人造成的伤害 -8% / -12% / -16%**」，不造成伤害 | 复用「霜寒」的伤害修正语义（`EntityDamageByEntityEvent` 按攻击者修正）；**不引入 miss 判定、不用 DARKNESS** |
| 68 落砂 | 命中在目标脚下扬起砂尘，短暂遮蔽其远距离视野 | 纯表现：脚下粒子簇 + 短时 `DARKNESS`；**不改客户端渲染距离、不改命中判定** |

- 两者在状态注册中心为**不同 id**（见 `MYTHIC_HAMMER_STATUS.md`）。
- 撤回上一轮 Q7 把二者合并「降级为粒子 + DARKNESS」的处理。

### R3 — 锤本体与三份前置文档

**裁定：现在定稿，三份文档同时产出，不再是草案。**

- `MYTHIC_HAMMER_ITEM.md`：锤本体规格（`mythic_hammer` / **1.21 原生重锤 `Material.MACE`** / 8.0 / 0.6 攻速 / **常驻高生命 100 / 120 / 135** / 阶 / 获取）。
- `MYTHIC_HAMMER_TERMS.md`：术语表（重击、命中、收锤、挥空、前摇、攻速、冲刺、满蓄力、生命值与 `live`）。
- `MYTHIC_HAMMER_STATUS.md`：状态注册中心、互斥 / 叠加封顶 / 优先级规则、季节失效 lore 呈现。

**上一轮草案遗漏的实现缺口（本次补上）**：`core/.../item/IMythicItem.java:240-259` 只有 `instanceof IMythicSword` 分支调用 `.itemDamage(...)`；非剑分支（`:260-278`）**不设置任何攻击伤害**。故锤必须新增 `IMythicHammer` 接口（与 `IMythicSword.java:7-10` 同构，仅含 `getItemDamage()`），并把该分支条件扩展为 `IMythicSword || IMythicHammer`，否则 8.0 不生效。

### R4 — `SEASONAL` 的 `itemColor`

**依据**

- 唯一消费点：`core/.../menu/admin/item/button/EnchantButton.java:62-65`，base 为 `Material.WHITE_WOOL`，`.durability(rarity.getItemColor())`。
- `base/.../compat/item/LegacyColor.java:13-16, 32-40`：`COLORS` 索引 0–15，`data > 15` 返回 `null`；索引 10 = `PURPLE` → `PURPLE_WOOL`。
- 已占用集合：`{0,1,2,3,4,5,7,8,9,14,15}`；未占用：`{6,10,11,12,13}`。

**裁定：`itemColor = 10`。**

- 枚举定稿：`SEASONAL("&a季节性! ", RarityType.LIMITED, 10)`。
- 理由：未占用；`PURPLE_WOOL` 与 `NORMAL`(5 → `LIME_WOOL`)、`RARE`(2 → `MAGENTA_WOOL`) 三色互异，管理端按钮可直接区分；绿色语义由 `&a` 前缀承担，不与按钮羊毛颜色绑定。
- 6 未采用，保留给后续扩展。

### R5 — 56「铁账」经济回归

**依据（本次即完成回归，不再留待后续）**

| 来源 | 量级 |
| --- | --- |
| `GoldDropRunnable.java:28-33` + `PitHook.kt:267`（每 20 tick 一次） | 全场每秒掉 2 个金锭，每个 3–5 硬币 → **6–10 硬币 / 秒（全场）** |
| `PlayerListener.java:424` | 拾取单个金锭 **+3–5 硬币** |
| `HedgeFundEnchant.java:51,58` | 现成经济附魔「击杀获得 **+6 硬币**」，对神话甲目标额外 +66 |
| `QuickMathEvent.java:107` | 事件奖励 520 硬币（大型事件，非日常基准） |

**结论**：上一轮的 30 / 60 / 90 每 10 击 ≈ 5–15 次击杀的附加收益，**明显过高**。

**裁定：终值 5 / 10 / 15 硬币**（每 10 次命中，物品阶 tier 1 / 2 / 3），单位「硬币」，走 `PitGainCoinsEvent`。与「击杀 +6 硬币」同量级，tier 3 约等于 2–3 次金锭拾取。责任与时点：**本次裁定即为回归完成**，写入原文档回写清单，落地前无需再做二次回归。

### R6 — `SEASONAL` 在 `EnchantButton` 的挂载

**依据**：`:314-319` 构建 `list`；`:326-345` 按 rarity 分流 `results` / `rareResults`（且 DARK / RAGE / DARK_GREEN 分支会**整体覆盖**）；`:347-348` `isBlackList` 过滤；`:350-400` switch 抽取；`:475-495` 卷轴 `getBookRareResults`；`:509-511` `removeExistingEnchantments` 去重；`:836-838` **`isBlackList` = `enchantment instanceof ILimit`**。

| 步骤 | 定稿 |
| --- | --- |
| 0（最易踩坑） | **SEASONAL 不得实现 `ILimit`**，否则被 `isBlackList` 从所有候选中剔除，永远抽不到 |
| 1 | 在 `:319` 之后、`:326` 之前构建**独立** `seasonalResults`：`list` 中 `rarity == SEASONAL` 且编号 ∈ 当前季度开放集合且通过 `isBlackList`。**不并入 `results` / `rareResults`**，避免被 `:332-345` 覆盖、避免污染 `helpMeToChooseOne` 的既有掉率分布 |
| 2 | 新增配置 `seasonal-chance`：物品阶 tier 1 / 2 / 3 = **0% / 5% / 10%**。在 `:350-400` switch 抽取**完成之后**做一次覆盖式判定，保持既有随机序列不变 |
| 3 | 命中且 `seasonalResults` 非空：用 `removeExistingEnchantments`（`:509-511`）去掉已持有项，随机取一条写入（等级按阶 1 / 2 / 3），并走 `:456` 同级的稀有广播 |
| 4 | 卷轴：新增 `getBookSeasonalResults`（与 `getBookRareResults:475-495` 同构），只在当前季度开放集合内产出；跨季持有的卷轴按 Q13 失效 |
| 5 | 未开放条目在获得侧不进池、使用侧在触发链入口短路，两侧共用同一个 `isSeasonOpen(enchant)` 纯函数 |

- 权重不进 `custom.yml rate`，不改现有 NORMAL / RARE 的概率分布。

### R7 — 互斥 / 冲突规则节

**依据**：`isBlackList:836-838` 只判 `ILimit`（按类型排除），**不做词条间互斥**；`EnchantButton` 无互斥表；文档中仅 6 号「与 5 号互斥」一个孤例。

**裁定：需要独立规则节，数据驱动，不逐条硬编码。** 分三类：互斥（不可共存）、叠加封顶（可共存但总和受限）、优先级（同时触发的结算顺序）。完整清单见 `MYTHIC_HAMMER_STATUS.md` 第二章，要点：

- **互斥**：5 沉肩 ↔ 6 惯性（站定 vs 移动，条件互斥）。
- **叠加封顶**：自身减伤类（5 / 8 / 22 / 26 / 34 / 40 / 60 / 82）合计上限 **35%**（63 为击退抗性、85 只分摊不减总量，均不纳入）；自身攻速类（23 / 51 / 65 / 73 / 95）净修正 clamp 到 **±30%**（98 作用于目标，单独判定）；同一 tick 内所有「消耗 `live`」条目合计最多 **2 点**。
- **优先级**：53 砧垫 > 74 崩砧 > 其他保命；48 砧裂生效期间**屏蔽**所有消耗 `live` 的条目；89 沉星霸体 > 26 铁律 / 54 锻隙韧性。
- 51 缓锤 ↔ 23 摇臂 / 65 短砧**不是互斥**：三者全部折算到同一个 `ATTACK_SPEED` modifier 后求和 clamp，不各写一套计时。
- 落地：一张配置表（id 组 / 规则类型 / 上限 / 优先级）+ `EnchantButton` 抽候选时过滤互斥组 + 触发链统一读取封顶与优先级。

### R8 — 「保留置灰」的呈现

**依据**：`base/.../item/AbstractPitItem.java:55-75` 的 `getEnchantLore` 在每次 `toItemStack()` 时**动态生成** lore，PDC 只存 `ench`；`:60` 已对 `DISABLED` / `REMOVED` 加 `&m` 删除线，`:71-73` 追加「此附魔暂时被管理员停用」——**现成的置灰模式**。

**裁定：不改 PDC，复用 `DISABLED` 分支扩展一个判定条件。**

| 呈现 | 做法 |
| --- | --- |
| 名称行 | `&8&m` 前缀 + 名称（与 `DISABLED` 同款删除线） |
| 描述行 | 整体降为 `&8`，不再显示 `&7` 数值 |
| 追加行 | `&7当前季度未开放 &8| <watermark>` |
| 不改 | 物品名称、颜色、材质、PDC `ench` 数据（避免与 `MythicColor` / 染色体系冲突） |

- 「失效」由触发链短路保证（Q13），「置灰」只做展示；两者共用 `isSeasonOpen(enchant)`，避免判定漂移。

---

## 三、对上一轮答复的更正

| 位置 | 原答复 | 更正为 | 关联 |
| --- | --- | --- | --- |
| Q1 | 「`itemColor` 取未占用值」 | `itemColor = 10` | R4 |
| Q4 | 「`ItemTypes#isHammer` 用 `Tag.ITEMS_AXES`」 | 撤回；按 PDC `mythic_hammer` 判定 | R1 |
| Q5 | 「建议草案，另立文档」 | 已定稿，见 `MYTHIC_HAMMER_ITEM.md`；另补 `IMythicHammer` 伤害分支 | R3 |
| Q7 | 「命中率下降 / 视野遮蔽」合并降级 | 拆为 55 减输出、68 视野遮蔽 | R2 |
| Q18 | 「30 / 60 / 90 硬币，暂定，待经济回归」 | **5 / 10 / 15 硬币**，回归本次完成 | R5 |
| Q5 / R3 的材质与生命 | 「`GOLDEN_AXE` / 攻速 0.7 / `maxLive` 20–28」 | **1.21 原生重锤 `Material.MACE` / 攻速 0.6 / 常驻高生命 100 / 120 / 135**；另需跳过 `IMythicItem.java:141-147` 的高生命改名、猛击封顶 +50%、排除原版专属附魔 | 本次修订 |

另：上一轮第三节「原文档回写清单」中 L24、L85、L106、L112、L117 等条目仍待执行；执行时数值以本文件第四章为准（尤其 L117 由 30 / 60 / 90 改为 5 / 10 / 15）。

---

## 四、定稿参数汇总

| 参数 | 定稿值 | 依据 |
| --- | --- | --- |
| 锤 internal name | `mythic_hammer` | `MythicSwordItem` 命名法 |
| Material / 显示名 / 品类 | **`Material.MACE`（1.21 原生重锤）** / 「神话之锤」 / 重锤 | 1.21 官方物品资料；不用斧或其它材质伪装 |
| 基础伤害 / 攻速 | 8.0 / `ATTACK_SPEED` **0.6** | 原生重锤 6 / 0.6；插件覆盖伤害、对齐攻速 |
| 初始生命（高） | **tier 1 = 100 / tier 2 = 120 / tier 3 = 135**（常驻，非 1% 稀有） | `EnchantButton.java:273,293` 的 135 / 100 档位；`IMythicItem.java:141-161` 高生命前缀体系 |
| 猛击额外加成上限 | **+50%** | 原生重锤高度加成无上限，天坑 PvP 需封顶 |
| 物品阶 / 生命 | `tier` 1 / 2 / 3、`live / maxLive` 60% / 30% | `IMythicItem.java:104-108, 178-179` |
| 掉落率 | 0.01（默认）/ 0.012（VIP） | `custom.yml:218-224` |
| `SEASONAL` 枚举 | `"&a季节性! "`, `LIMITED`, `10` | `EnchantmentRarity.java` / `LegacyColor.java` |
| 季节判定时区 / 边界 | `Asia/Shanghai` / 自然月首日 00:00 | Q11 |
| `seasonal-chance` | 物品阶 tier 1 / 2 / 3 = 0% / 5% / 10% | R6 |
| 季度开放集合 | Q1 91–93、Q2 94–96、Q3 97–100、Q4 91–100 | 需求原文 |
| 55 钝鸣 | 对他人伤害 -8% / -12% / -16%，2 / 3 / 4 秒，1 / 2 / 3 名 | R2 |
| 56 铁账 | 每 10 击 5 / 10 / 15 硬币 | R5 |
| 减伤 / 攻速 / 单 tick `live` 消耗上限 | 35% / ±30% / 2 点 | R7 |
| 锤印 | 上限 3 层、8 秒、切目标清空；16 号触发阈值第 3 / 2 / 1 层 | Q8 |

---

## 五、阻塞项状态

| 原 P0 阻塞项 | 状态 |
| --- | --- |
| Q1 枚举与前缀 | **已清**：`SEASONAL("&a季节性! ", LIMITED, 10)` |
| Q4 + Q10 类型识别链 | **已定口径**：PDC `mythic_hammer`；改动点见 R1 与 `MYTHIC_HAMMER_ITEM.md` |
| Q5 锤本体规格 | **已清**：`MYTHIC_HAMMER_ITEM.md` 定稿 |
| Q11 季节判定 | **已定**：服务端日期 + `Asia/Shanghai` + `seasonal-override`（Q11），抽取挂载见 R6 |
| 术语表 / 状态注册中心 | **已产出**：`MYTHIC_HAMMER_TERMS.md` / `MYTHIC_HAMMER_STATUS.md` |
| 原文档回写清单 | **待执行**（本次未改 `MYTHIC_HAMMER_ENCHANTS.md`） |

设计侧 P0 全部清零；剩余为实现工作与文档回写。

---

## 六、口径说明

- 本文件与三份规格文档均为设计定稿，含明确数值与代码集成点，但不含实现代码、不含代码片段。
- 引用的类名、配置键与方法名用于定位，行号以当前磁盘版本为准；落地时须对照 Paper 1.21.8 API 复核属性与事件名。
- 沿用 `MIGRATION_PAPER_1.21.md` 的既有风险项（PDC 存量数据、ProtocolLib 未重写、属性修饰符覆盖）不变。
- 文档仅留本地工作区，未提交、未推送。

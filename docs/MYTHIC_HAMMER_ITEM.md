# 神话之锤：物品本体规格（定稿）

> 状态：**定稿**，取代 `MYTHIC_HAMMER_ENCHANTS_ANSWERS.md` 中 Q5 的「建议草案」。
> **本次修订**：物品本体改为 1.21 原生重锤 `Material.MACE`（不再用 `GOLDEN_AXE` 伪装），`maxLive` 提高为常驻高生命 100 / 120 / 135，攻速对齐原生 0.6。
> 关联：`MYTHIC_HAMMER_RULINGS.md`（R1 / R3）、`MYTHIC_HAMMER_ENCHANTS.md`（100 条附魔清单）。
> 本文分六部分：①定稿规格 ②「重锤」口径 ③1.21 原生重锤的集成点 ④对照与改动清单 ⑤Lore 与展示 ⑥校验与口径。

---

## 一、定稿规格

| 项 | 定稿值 | 依据 / 说明 |
| --- | --- | --- |
| internal name | `mythic_hammer` | 与 `MythicSwordItem.java:15-16` 的 `mythic_sword` 命名法一致；写入 PDC `KEY_INTERNAL`（`ItemUtil.java:161-163`） |
| 显示名 | 神话之锤 | 与「神话之剑 / 神话之弓 / 神话之甲」同构 |
| 物品品类 | **重锤** | Minecraft 1.21（棘巧试炼）加入的近战武器，物品 ID `mace`，中文官方名「重锤」 |
| Material | **`Material.MACE`** | 直接用原生重锤，**不用斧 / 其它材质伪装**；枚举常量名以 Paper 1.21.8 API 为准 |
| 基础攻击伤害 | **8.0**（插件覆盖） | 原生重锤为 6；神话之剑 6.5、商店钻剑 8、玻璃镐 8.5。神话物品高于原版，定位高伤低速 |
| 基础攻速 | **0.6** | 对齐原生重锤（攻击冷却约 1.67 秒），比剑明显慢，符合「重」；通过 `ATTACK_SPEED` modifier 设定 |
| 双手约束 | 副手持有物品时禁用锤附魔触发 | 自定义判定放触发链入口；不占用副手槽、不改原版规则 |
| 物品阶 tier | 复用 `IMythicItem.tier`，取值 1 / 2 / 3，物品名以 `RomanUtil.convert(tier)` 显示为 I / II / III 阶 | `IMythicItem.java:104-108`；`EnchantButton.java:310-312` 的局部变量 `level` 实际写入 `setTier` |
| 生命属性 | 复用 `live / maxLive`，60% / 30% 阈值变色 | `IMythicItem.java:178-179` |
| **初始生命（高）** | **tier 1 = 100 / tier 2 = 120 / tier 3 = 135**（常驻，非 1% 稀有） | 仓库已有同档位：`EnchantButton.java:293` 的 100 生命（1% 稀有）、`:273` 的 135 生命（1% 稀有）、`AuctionEvent.java:200` 的 100；`IMythicItem.java:141-161` 为 `maxLive >= 100` 准备了「精制的 / 万里挑一的 / 奇迹般的 / 强大的」前缀体系 |
| 获取 | 击杀掉落，默认 0.01 / VIP 0.012 | `custom.yml:218-224`；`MythicEnchantingTable.java:32` 的物品 lore |
| 附魔入口 | 神话之井（`MythicEnchantingTable` + `MythicWellMenu` + `EnchantButton`） | 附魔 ≠ 物品产出，见 Q9 更正 |
| 修复 | 凋灵工艺（+1）、终章连杀（+4）、神话工匠包（满）、addon 暗聚块修复（+1） | `WitherCraftKillStreak.java:144`、`GrandFinaleMegaStreak.java:150`、`MythicRepairKit.java:90`、`FreeCE.kt:64`；**四处均需补锤分支**。高生命下不改动修复数值，长寿命本身即「生命多」的定位 |
| 耐久 | **不可破坏**，原版 500 耐久不参与 | 寿命只由 `live` 管理（`ItemMeta#setUnbreakable`，见 `MIGRATION_PAPER_1.21.md:71`） |
| 可交易 / 末影箱 / 死亡保留 | 与剑一致 | `IMythicItem.java:244-250` |

**双手约束细则**：仅在「副手 `ItemStack` 非 AIR」时短路锤附魔触发；物品本身仍可正常造成伤害。不作为互斥词条处理，不写入 PDC。

**生命多的换算**：60% 阈值在 tier 1 / 2 / 3 分别约为 60 / 72 / 81 点，30% 阈值约为 30 / 36 / 40 点。所有 `live` 消耗类附魔仍按 1–2 点计，C3 单 tick 合计封顶 2 点不变（见 `MYTHIC_HAMMER_STATUS.md`）。

---

## 二、「重锤」口径

- **物品本体就是重锤**：`Material.MACE`，不是「用斧头改显示名」。1.21 原生重锤自带攻击冷却 0.6、下落攻击（猛击）与三种专属附魔，本设计在这个物品上叠加神话体系。
- **显示名保留「神话之锤」**，与系列命名一致；品类 / lore 标注为「重锤」。若要求物品显示名直接为「重锤」，只需改 `getItemDisplayName()` 一行。
- **对 R1 判定口径的影响**：`ItemTypes.java:44-46` 的 `isAxe` 按 `_AXE` 后缀判定，`MACE` **不匹配**，因此不会把普通斧误判为锤；同时 `MACE` 也不落入 `isSword` / `isBow` / `isArmor` 任何分支，`AbstractEnchantment#canApply` 的材质分支会全部返回 `false`——**必须由新增的 `isMythicHammer`（PDC `mythic_hammer`）分支接管**，否则锤一条附魔都拿不到。
- **普通 `MACE` 物品**：同样不能吃这 100 条（无 PDC `ench`、被 `canApply` 拦截）。

---

## 三、1.21 原生重锤的集成点

| 集成点 | 原版行为 | 定稿处理 |
| --- | --- | --- |
| 下落攻击（猛击） | 下落 > 1.5 格命中时按高度附加伤害（无上限），附带范围击退、取消摔落伤害 | **保留手感，但伤害必须进 `CombatListener` 统一结算并封顶**：猛击额外加成上限 **+50%**，范围击退按现有击退管线处理；避免高处落下秒杀破坏天坑 PvP 平衡 |
| 原版专属附魔 | 致密 / 破甲 / 风爆 | **禁止**。100 条自定义附魔是唯一词条来源；神话之井只接受未附魔神话物品（`MythicWellMenu.java:291-300`）；与「全部排除原版附魔」的约束一致 |
| 原版耐久 500 | 原生耐久损耗 | 锤设为不可破坏，寿命只由 `live` 决定，原版耐久不参与 |
| `maxLive >= 100` 的命名逻辑 | `IMythicItem.java:141-147`：DARK 色且 `maxLive >= 100` 时把名字改成「**恶魔之甲**」 | **锤必须跳过该改名分支**（按 `instanceof IMythicHammer` 判定），仅保留前缀逻辑（`:145` 的「精制的」、`:149-161` 的「万里挑一的 / 奇迹般的 / 强大的」）。否则高生命 DARK 锤会被显示成护甲 |
| 附魔时补 `live` | `EnchantButton.java:304-308` 按 `maxLive` 变化补差额 | 常驻 100 / 120 / 135 后该逻辑仍适用，不改 |
| 攻击冷却与术语 | 原生 1.67 秒 | 与 `MYTHIC_HAMMER_TERMS.md` 的「满蓄力 / 收锤」口径一致；「重击」仍指满蓄力的一击，不与猛击混用 |

> 猛击封顶是本次新增的平衡裁定：原生重锤的高度加成无上限，而天坑是常驻竞技场，允许无限叠加会直接破坏 PvP。

---

## 四、与现有神话物品对照及改动清单

| 物品 | internal name | Material | 自定义伤害 | 伤害写入 |
| --- | --- | --- | --- | --- |
| 神话之剑 | `mythic_sword` | `GOLDEN_SWORD` | 6.5 | `IMythicSword` 分支（`IMythicItem.java:240-259`） |
| 神话之弓 | `mythic_bow` | `BOW` | 无 | 非剑分支（`:260-278`） |
| 神话之甲 | `mythic_leggings` | 护腿 | 无 | 非剑分支 |
| 魔法钓竿 | `mythic_reel` | 钓竿 | 无 | 非剑分支 |
| **神话之锤** | `mythic_hammer` | **`MACE`** | **8.0** | **新增 `IMythicHammer` 分支** |

> 关键：`IMythicItem.java:240` 的条件是 `instanceof IMythicSword`，非剑分支（`:260-278`）**完全不写攻击伤害**。锤若只实现 `getItemDamage()` 而不扩展该条件，8.0 不生效。

| 文件 | 改动 | 优先级 |
| --- | --- | --- |
| `core/.../item/IMythicSword.java`（旁） | 新增 `IMythicHammer` 接口，仅含 `double getItemDamage()` | P0 |
| `core/.../item/type/mythic/MythicHammerItem.java` | 新增，实现 `IMythicItem` + `IMythicHammer`；`getItemDisplayMaterial()` 返回 `MACE`；`maxLive` 按阶 100 / 120 / 135 | P0 |
| `core/.../item/IMythicItem.java:240` | 伤害分支条件扩为 `IMythicSword \|\| IMythicHammer`；两分支都写 `.itemDamage(...)`、`.maxLive/.live/.tier`、不可破坏 | P0 |
| `core/.../item/IMythicItem.java:141-147` | 高生命改名分支跳过锤，避免显示成「恶魔之甲」 | P0 |
| `base/.../compat/item/ItemTypes.java` | 新增 `isMythicHammer(ItemStack)`，按 `ItemUtil.getInternalName` 精确匹配；`MACE` 不落任何材质分支 | P0 |
| `base/.../enchantment/AbstractEnchantment.java:69-83` | `canApply` 首判 `isMythicHammer` → 只放行 `HammerOnly` | P0 |
| `base/.../enchantment/param/item/` | 新增 `HammerOnly` 注解 | P0 |
| `core/.../enchantment/menu/MythicWellMenu.java:292` | 白名单加 `mythic_hammer` | P0 |
| `core/.../enchantment/menu/button/EnchantButton.java:260-303` | 锤的 `maxLive` 赋值走独立分支（100 / 120 / 135），不参与随机低生命档位 | P0 |
| `core/.../listener/CombatListener.java` | 猛击额外伤害进统一管线并封顶 +50% | P0 |
| `core/.../perk/type/streak/highlander/WitherCraftKillStreak.java:122-128` | 按 internal name 补锤分支（否则锤无法修复） | P0 |
| `core/.../item/type/MythicRepairKit.java`、`core/.../item/factory/ItemFactory.java` | 按 internal name 补锤 | P0 |
| `core/.../menu/admin/item/button/EnchantButton.java:55-56, 76-80` | 管理端类型校验与 `setMaxLive` 分支补锤 | P1 |
| `base/.../util/item/ItemBuilder.java:388-401` | 新增 `attackSpeed(double)`：固定 `NamespacedKey` + 先 `removeAttributeModifier` 再写入 `ATTACK_SPEED` / `MAINHAND` | P1 |
| `core/.../enchantment/menu/button/EnchantButton.java` | 挂 `SEASONAL` 抽取，见 `MYTHIC_HAMMER_RULINGS.md` R6 | P1 |

> `ItemBuilder#itemDamage` 已按 `MIGRATION_PAPER_1.21.md:70` 迁移到官方属性 API；`attackSpeed` 必须同法处理，避免 `:143` 记录的「重复 `addAttributeModifier` 覆盖」风险。

---

## 五、Lore 与展示

| 行 | 内容 |
| --- | --- |
| 物品名 | `<颜色>` + `RomanUtil.convert(tier)` + ` 阶神话之锤`；高生命时按 `IMythicItem.java:141-161` 加「精制的 / 万里挑一的」等前缀（**不改名**） |
| 品类行 | `&7重锤`（标注原生重锤品类） |
| 生命行 | `&7生命: <按 60% / 30% 阈值着色> live&7/maxLive`（100 / 120 / 135），附宝石 / 卷轴标记 |
| 附魔词条 | 由 `AbstractPitItem#getEnchantLore`（`:55-75`）动态生成；`SEASONAL` 未开放时按 R8 置灰 |
| 未附魔时 | `&7死亡后保留` / `&7在神话之井中附魔 &8| <watermark>`（`IMythicItem.java:212-220`） |
| 水印 | `NewConfiguration.INSTANCE.getWatermarks()` |

- 不改 `MythicColor` 体系；锤沿用 RED / ORANGE / BLUE / GREEN / YELLOW 五色与 `mythic_color` PDC 键。
- 附魔展示前缀由 `EnchantmentRarity` 决定：`SEASONAL` 用 `&a季节性! `（绿色）。

---

## 六、校验与口径

| 校验项 | 结果 |
| --- | --- |
| 物品本体是否为原生重锤 | 是（`Material.MACE`，非斧伪装） |
| 普通 `MACE` / 普通斧能否吃锤附魔 | 不能（`canApply` 按 internal name 拦截 + 无 PDC `ench`） |
| 锤能否放入神话之井 | 能（白名单已补） |
| 8.0 伤害与 0.6 攻速能否写入 | 能（`IMythicHammer` 分支 + `attackSpeed` modifier） |
| 生命是否显著高于其它神话物品 | 是（100 / 120 / 135 vs 剑常见 3–60） |
| 高生命 DARK 锤是否误显示为「恶魔之甲」 | 否（改名分支已跳过） |
| 猛击是否封顶、原版专属附魔是否排除 | 是（+50% 封顶 / 禁止） |
| 锤能否被凋灵工艺 / 工匠包修复 | 能（四处类型分支已补） |
| 物品阶 tier 1 / 2 / 3 与 `live` 是否复用现有体系 | 是 |
| 掉落率是否与其它神话物品一致 | 是（0.01 / 0.012） |

- 1.21 重锤的物品属性取自该版本的官方物品资料；`Material.MACE` 等枚举常量名落地时对照 Paper 1.21.8 API 复核。
- 本文件为设计定稿，不含实现代码与代码片段；引用的类名与行号仅用于定位。
- 文档仅留本地工作区，未提交、未推送。

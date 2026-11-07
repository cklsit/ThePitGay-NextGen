import os
import re

ITEM_DATA = 'cn.charlotte.pit.compat.item.ItemData'

# ---------- A. item/type/*.kt：NBT -> PDC ----------
for name in ('ArmageddonBoots', 'GoldenHelmet', 'LuckyChestplate', 'AngelChestplate'):
    p = f'core/src/main/java/net/mizukilab/pit/item/type/{name}.kt'
    t = open(p, encoding='utf-8').read()
    t = t.replace("""        val nmsItem = Utils.toNMStackQuick(item)
        val tag = nmsItem?.tag ?: return
        val extra = tag.getCompound("extra") ?: return""",
                  f"""        if (!{ITEM_DATA}.hasAny(item)) return""")
    t = re.sub(r'extra\.getInt\("([^"]+)"\)', r'(' + ITEM_DATA + r'.getInt(item, "\1") ?: 0)', t)
    t = re.sub(r'extra\.getBoolean\("([^"]+)"\)', ITEM_DATA + r'.getBoolean(item, "\1")', t)
    t = re.sub(r'extra\.getString\("([^"]+)"\)', ITEM_DATA + r'.getString(item, "\1")', t)
    t = re.sub(r'extra\.hasKey\("([^"]+)"\)', ITEM_DATA + r'.has(item, "\1")', t)
    open(p, 'w', encoding='utf-8').write(t)

# ---------- B. JewelSword：物品展示悬停改文本 ----------
p = 'core/src/main/java/net/mizukilab/pit/item/type/JewelSword.kt'
t = open(p, encoding='utf-8').read()
t = t.replace("""                val nms = Utils.toNMStackQuick(itemStack)
                val tag = NBTTagCompound()
                nms?.save(tag)
                val hoverEventComponents = arrayOf<BaseComponent>(
                    TextComponent(tag.toString())
                )""",
              """                // 1.21：SHOW_ITEM 悬停需要真实物品数据，这里退化为文本悬停
                val hoverEventComponents = arrayOf<BaseComponent>(
                    TextComponent(itemStack.type.name)
                )""")
open(p, 'w', encoding='utf-8').write(t)

# ---------- C. Volley_A：移除已删除字段的残留引用 ----------
p = 'core/src/main/java/net/mizukilab/pit/enchantment/type/custom/Volley_A.kt'
t = open(p, encoding='utf-8').read()
t = t.replace("""    init {
        playerUsingFiled.isAccessible = true
    }

""", "")
open(p, 'w', encoding='utf-8').write(t)

# ---------- D. SpeedEggs：MONSTER_EGG ----------
p = 'core/src/main/java/net/mizukilab/pit/item/type/egg/SpeedEggs.kt'
t = open(p, encoding='utf-8').read()
t = t.replace('Material.MONSTER_EGG', 'Material.ZOMBIE_SPAWN_EGG')
open(p, 'w', encoding='utf-8').write(t)

# ---------- E. 1.8 音符粒子与可空玩家 ----------
for p in ('core/src/main/java/net/mizukilab/pit/enchantment/type/limit/love/Limit24520Ench.kt',
          'core/src/main/java/net/mizukilab/pit/enchantment/type/limit/xzq/LimitXZQ1Ench.kt'):
    t = open(p, encoding='utf-8').read()
    t = t.replace('EnumParticle.NOTE', 'org.bukkit.Particle.NOTE')
    t = t.replace(', NOTE,', ', org.bukkit.Particle.NOTE,')
    t = re.sub(r'\bNOTE\b(?!\w)', 'org.bukkit.Particle.NOTE', t)
    open(p, 'w', encoding='utf-8').write(t)

print('batch7 ok')

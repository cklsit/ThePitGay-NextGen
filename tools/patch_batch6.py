import re


def read(p):
    return open(p, encoding='utf-8').read()


def write(p, t):
    open(p, 'w', encoding='utf-8').write(t)


# 1) 声音名前缀污染
p = 'base/src/main/java/net/mizukilab/pit/menu/workshop/button/DyeButton.java'
t = read(p).replace('BLOCK_LAVA_AMBIENT_POP', 'BLOCK_LAVA_POP')
write(p, t)

# 2) PlayerUtil.damage 重载消除歧义
p = 'base/src/main/java/net/mizukilab/pit/util/PlayerUtil.java'
t = read(p)
t = t.replace('public static boolean damage(Player victim, DamageType damageType, Double damage, boolean canImmune) {',
              'public static boolean damage(Player victim, DamageType damageType, double damage, boolean canImmune) {')
t = t.replace('public static void damage(Entity victim, DamageType damageType, Double damage, Boolean canImmune) {',
              'public static void damage(Entity victim, DamageType damageType, double damage, Boolean canImmune) {')
write(p, t)

# 3) PublicUtil: 吸收伤害改 Bukkit API
p = 'base/src/main/java/net/mizukilab/pit/util/PublicUtil.java'
t = read(p)
t = t.replace('int absorptionHearts1 = (int) ((CraftPlayer) victim).getHandle().getAbsorptionHearts();',
              'int absorptionHearts1 = (int) victim.getAbsorptionAmount();')
t = t.replace('import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;\n', '')
write(p, t)

# 4) VisibleApi: PacketEvents 的槽位名
p = 'base/src/main/java/net/mizukilab/pit/util/VisibleApi.java'
t = read(p).replace('EquipmentSlot.CHESTPLATE', 'EquipmentSlot.CHEST_PLATE')
write(p, t)

# 5) InventoryUtil: CraftItemStack -> clone
p = 'base/src/main/java/net/mizukilab/pit/util/inventory/InventoryUtil.java'
t = read(p)
t = t.replace('ItemStack filteredItem = CraftItemStack.asCraftCopy(item);', 'ItemStack filteredItem = item.clone();')
t = t.replace('CraftItemStack stack = CraftItemStack.asCraftCopy(item);', 'ItemStack stack = item.clone();')
write(p, t)

# 6) TradeManager: 补 sendItemHover
p = 'base/src/main/java/net/mizukilab/pit/menu/trade/TradeManager.java'
t = read(p)
helper = """
    /**
     * 1.21：SHOW_ITEM 悬停改为 Adventure 的 ItemStack 悬停。
     */
    private static void sendItemHover(org.bukkit.entity.Player player, String message, ItemStack item) {
        net.kyori.adventure.text.Component component = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection()
                .deserialize(CC.translate(message))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showItem(item));
        player.sendMessage(component);
    }
"""
if 'private static void sendItemHover' not in t:
    idx = t.rstrip().rfind('}')
    t = t[:idx] + helper + '}' + t[idx + 1:]
write(p, t)

print('batch6 ok')

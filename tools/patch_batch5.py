import os
import re


def read(p):
    return open(p, encoding='utf-8').read()


def write(p, t):
    open(p, 'w', encoding='utf-8').write(t)


# 0) 前缀污染：Material.BED -> RED_BED 误伤 BEDROCK
for root in ('base', 'core'):
    for bas, _dirs, names in os.walk(root):
        for name in names:
            if not name.endswith(('.java', '.kt')):
                continue
            p = os.path.join(bas, name)
            t = read(p)
            if 'RED_BEDROCK' in t:
                write(p, t.replace('RED_BEDROCK', 'BEDROCK'))

# 1) EventFactory: 裸 BURP
p = 'base/src/main/java/cn/charlotte/pit/events/EventFactory.java'
t = read(p)
t = t.replace('player.playSound(player.getLocation(), BURP, 1, 0.72F)',
              'player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_BURP, 1, 0.72F)')
write(p, t)

# 2) PlayerMoveHandler: EnumParticle 类型声明
p = 'base/src/main/java/net/mizukilab/pit/movement/PlayerMoveHandler.java'
t = read(p)
t = t.replace('EnumParticle particleType = isDescending ? Particle.DRIPPING_WATER : Particle.CLOUD;',
              'org.bukkit.Particle particleType = isDescending ? Particle.DRIPPING_WATER : Particle.CLOUD;')
write(p, t)

# 3) AbstractEnchantment: ItemFishingRod
p = 'base/src/main/java/net/mizukilab/pit/enchantment/AbstractEnchantment.java'
t = read(p)
t = t.replace("""        if (nmsItem.getItem() instanceof ItemFishingRod) {""",
              """        if (item.getType() == org.bukkit.Material.FISHING_ROD) {""")
t = t.replace("""        net.minecraft.server.v1_8_R3.ItemStack nmsItem = PublicUtil.toNMStackQuick(item);
""", "")
write(p, t)

# 4) BossBar: getAudiences -> 直接操作
p = 'base/src/main/java/net/mizukilab/pit/util/bossbar/BossBar.java'
t = read(p)
t = t.replace('ThePit.getInstance().getAudiences().player(player).hideBossBar(bar);', 'player.hideBossBar(bar);')
t = t.replace('ThePit.getInstance().getAudiences().player(player).showBossBar(bar);', 'player.showBossBar(bar);')
write(p, t)

# 5) Menu: getTitle() 已移除
p = 'base/src/main/java/net/mizukilab/pit/util/menu/Menu.java'
t = read(p)
t = t.replace('player.getOpenInventory().getTopInventory().getTitle().equals(title)',
              'player.getOpenInventory().getTitle().equals(title)')
write(p, t)

# 6) NPC_Entity: runTask 歧义
p = 'base/src/main/java/net/jitse/npclib/internal/NPC_Entity.java'
t = read(p)
t = t.replace('Bukkit.getScheduler().runTask(getPlugin(), this::createPackets);',
              'Bukkit.getScheduler().runTask(getPlugin(), (Runnable) this::createPackets);')
write(p, t)

print('batch5 ok')

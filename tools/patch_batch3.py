def read(p):
    return open(p, encoding='utf-8').read()


def write(p, t):
    open(p, 'w', encoding='utf-8').write(t)


# 1) SongPlayer：服务端运行状态判断改走插件状态
p = 'base/src/main/java/net/mizukilab/pit/util/music/SongPlayer.java'
t = read(p)
t = t.replace("import net.minecraft.server.v1_8_R3.MinecraftServer;\n", "")
t = t.replace("while (!destroyed && MinecraftServer.getServer().isRunning()) {",
              "while (!destroyed && cn.charlotte.pit.ThePit.getInstance() != null && cn.charlotte.pit.ThePit.getInstance().isEnabled()) {")
write(p, t)

# 2) PetFactory：NMS killer 字段 -> metadata 记录击杀者
p = 'base/src/main/java/net/mizukilab/pit/pet/PetFactory.java'
t = read(p)
t = t.replace("import net.minecraft.server.v1_8_R3.EntityLiving;\n", "")
t = t.replace("import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;\n", "")
t = t.replace("import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;\n", "")
t = t.replace("""                if (entity.getHealth() < event.getFinalDamage()) {
                    final EntityLiving player = ((CraftLivingEntity) entity).getHandle();
                    player.killer = ((CraftPlayer) killer).getHandle();
                }""",
              """                if (entity.getHealth() < event.getFinalDamage() && killer != null) {
                    // 1.21：击杀者归属改用 metadata 记录，供死亡结算读取
                    entity.setMetadata("pit_killer", new org.bukkit.metadata.FixedMetadataValue(
                            cn.charlotte.pit.ThePit.getInstance(), killer.getUniqueId().toString()));
                }""")
write(p, t)

# 3) TradeManager：SHOW_ITEM 悬停改用 Adventure 的 ItemStack 悬停
p = 'base/src/main/java/net/mizukilab/pit/menu/trade/TradeManager.java'
t = read(p)
t = t.replace("import net.minecraft.server.v1_8_R3.NBTTagCompound;\n", "")
old_b = """                net.minecraft.server.v1_8_R3.ItemStack nms = PublicUtil.toNMStackQuick(item);
                NBTTagCompound tag = new NBTTagCompound();
                nms.save(tag);
                BaseComponent[] hoverEventComponents = new BaseComponent[]{
                        new TextComponent(tag.toString())
                };
                playerA.spigot().sendMessage(new ChatComponentBuilder(CC.translate(message))
                        .setCurrentHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, hoverEventComponents)).create());"""
new_b = """                sendItemHover(playerA, message, item);"""
t = t.replace(old_b, new_b)
old_a = """                net.minecraft.server.v1_8_R3.ItemStack nms = PublicUtil.toNMStackQuick(item);
                NBTTagCompound tag = new NBTTagCompound();
                nms.save(tag);
                BaseComponent[] hoverEventComponents = new BaseComponent[]{
                        new TextComponent(tag.toString())
                };
                playerA.spigot().sendMessage(new ChatComponentBuilder(CC.translate(message))
                        .setCurrentHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, hoverEventComponents)).create());"""
t = t.replace(old_a, new_b)

helper = """
    /**
     * 1.21：SHOW_ITEM 悬停改为 Adventure 的 ItemStack 悬停，不再手工序列化 NBT。
     */
    private static void sendItemHover(org.bukkit.entity.Player player, String message, ItemStack item) {
        net.kyori.adventure.text.Component component = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection()
                .deserialize(CC.translate(message))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showItem(item));
        player.sendMessage(component);
    }
"""
if 'sendItemHover' not in t.split('private static void sendItemHover')[0]:
    idx = t.rstrip().rfind('}')
    t = t[:idx] + helper + t[idx:]
write(p, t)

print('batch3 ok')

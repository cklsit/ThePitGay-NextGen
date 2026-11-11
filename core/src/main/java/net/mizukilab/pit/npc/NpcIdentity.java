package net.mizukilab.pit.npc;

import cn.charlotte.pit.ThePit;
import cn.mythicland.npclib.internal.NPCManager;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;

/** Constant-time recognition without loading an optional NPC plugin's classes. */
public final class NpcIdentity {
    private NpcIdentity() { }

    // Extensions embed their own copy of NPCLib, so instanceof the core's class
    // cannot recognize their bots. Cache the result without retaining classloaders.
    private static final ClassValue<Boolean> MYTHIC_BOTS = new ClassValue<>() {
        @Override protected Boolean computeValue(Class<?> type) {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                String name = current.getName();
                // 1.21 的 Bot 仍是真实 ServerPlayer 子类，包名保留 v1_8_R3 命名空间，
                // 因此按后缀匹配以兼容被 shade 重定位过的副本。
                if (name.endsWith(".bot.v1_8_R3.BotEntity")
                        || name.equals("cn.mythicland.npclib.bot.v1_8_R3.BotEntity")
                        || name.equals("net.mizukilab.pit.libs.npclib.bot.v1_8_R3.BotEntity")
                        || name.equals("cn.mx.thepitaddon.npc.lib.bot.v1_8_R3.BotEntity")) return true;
            }
            return false;
        }
    };

    public static boolean isMythicBot(Entity entity) {
        return entity instanceof CraftEntity craft && craft.getHandle() != null
                && isMythicBotType(craft.getHandle().getClass());
    }

    static boolean isMythicBotType(Class<?> type) { return MYTHIC_BOTS.get(type); }

    public static boolean isNpc(Entity entity) {
        if (entity == null) return false;
        if (isMythicBot(entity)) return true;
        if (NPCManager.getByEntityId(entity.getEntityId()) != null
                || entity.hasMetadata("NPC") || entity.hasMetadata("Bot")) return true;
        ThePit core = ThePit.getInstance();
        return core != null && core.getCustomEntityNPCFactory() != null
                && core.getCustomEntityNPCFactory().isCustomNPC(entity);
    }
}

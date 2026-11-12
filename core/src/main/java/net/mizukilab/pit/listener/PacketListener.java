package net.mizukilab.pit.listener;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.event.PotionAddEvent;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEffect;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * 药水效果加成的包监听。
 *
 * <p>原实现基于 ProtocolLib 的 {@code ENTITY_EFFECT} 结构修改器读取字段；
 * 1.21 该包结构改为 Holder 后旧字段索引越界（FieldAccessException），
 * 现改用插件内置的 PacketEvents 解析同一个包。</p>
 */
public class PacketListener extends PacketListenerAbstract {

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_EFFECT) return;
        Player player = event.getPlayer();
        if (player == null) return;

        WrapperPlayServerEntityEffect wrapper = new WrapperPlayServerEntityEffect(event);
        if (wrapper.getEntityId() != player.getEntityId()) return;

        PotionEffectType type = toBukkitType(wrapper);
        if (type == null) return;

        final int amplifier = wrapper.getEffectAmplifier();
        final int duration = wrapper.getEffectDurationTicks();
        final boolean ambient = wrapper.isAmbient();
        final boolean particles = wrapper.isVisible();

        Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> {
            PotionAddEvent potionEvent = new PotionAddEvent(player,
                    new PotionEffect(type, duration, amplifier, ambient, particles));
            potionEvent.callEvent();
            if (potionEvent.isCancelled()) {
                Bukkit.getScheduler().runTaskLater(ThePit.getInstance(),
                        () -> player.removePotionEffect(type), 1L);
            }
        }, 1L);
    }

    private static PotionEffectType toBukkitType(WrapperPlayServerEntityEffect wrapper) {
        try {
            String name = wrapper.getPotionType().getName().toString();
            if (name == null || name.isEmpty()) return null;
            PotionEffectType type = PotionEffectType.getByKey(NamespacedKey.fromString(name));
            return type != null ? type : PotionEffectType.getByName(name);
        } catch (Throwable ignored) {
            return null;
        }
    }
}

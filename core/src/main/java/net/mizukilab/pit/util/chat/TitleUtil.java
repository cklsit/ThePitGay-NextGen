package net.mizukilab.pit.util.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/1 13:26
 */
public class TitleUtil {


    /**
     * 1.21：改用 Adventure Title，替代 PacketPlayOutTitle 的 TITLE/SUBTITLE 两次发包。
     * 原实现按 tick 传入淡入淡出，这里换算为 Duration（1 tick = 50ms）。
     */
    public static void sendTitle(Player player, String title, String sub, int fadeIn, int fadeOut, int duration) {
        Component titleComponent = title == null ? null
                : LegacyComponentSerializer.legacySection().deserialize(CC.translate(title));
        Component subComponent = sub == null ? null
                : LegacyComponentSerializer.legacySection().deserialize(CC.translate(sub));
        Title.Times times = Title.Times.times(ticks(fadeIn), ticks(duration), ticks(fadeOut));
        player.showTitle(Title.title(
                titleComponent == null ? Component.empty() : titleComponent,
                subComponent == null ? Component.empty() : subComponent,
                times));
    }

    private static Duration ticks(int ticks) {
        return Duration.ofMillis(Math.max(0, ticks) * 50L);
    }
}

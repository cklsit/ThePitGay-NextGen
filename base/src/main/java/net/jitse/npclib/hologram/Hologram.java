/*
 * Copyright (c) 2018 Jitse Boonstra
 */

package net.jitse.npclib.hologram;

import net.jitse.npclib.internal.NPCBase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import java.util.ArrayList;
import java.util.List;

/**
 * NPC 头顶多行文本。
 * <p>
 * 1.21 适配：原实现通过反射拼装 1.8 的 DataWatcher 与生成包逐玩家下发；
 * 现改为真实 {@link TextDisplay} 实体（支持 \n 多行），可见性由 {@code Player#showEntity/hideEntity} 控制。
 */
public class Hologram {

    private static final double DELTA = 0.3;

    private final List<String> text;
    private final Location location;
    private TextDisplay display;

    public Hologram(List<String> text, Location location) {
        this.text = text == null ? new ArrayList<>() : new ArrayList<>(text);
        this.location = location.clone().add(0, 2.1, 0);
    }

    public List<Object> getUpdatePackets(List<String> newText) {
        // 真实实体不需要构造数据包，这里把新文本透传给 update()
        return new ArrayList<>(newText == null ? new ArrayList<String>() : newText);
    }

    public void update(Player player, List<Object> data) {
        List<String> lines = new ArrayList<>(data.size());
        for (Object element : data) {
            lines.add(String.valueOf(element));
        }
        this.text.clear();
        this.text.addAll(lines);
        applyText();
    }

    public void show(Player player) {
        TextDisplay current = ensure();
        if (current != null) {
            player.showEntity(NPCBase.getPlugin(), current);
        }
    }

    public void hide(Player player) {
        if (display != null && display.isValid()) {
            player.hideEntity(NPCBase.getPlugin(), display);
        }
    }

    public void destroy() {
        if (display != null) {
            display.remove();
            display = null;
        }
    }

    public TextDisplay ensure() {
        if (display != null && display.isValid()) {
            return display;
        }
        if (location.getWorld() == null) {
            return null;
        }
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(NPCBase.getPlugin(), this::ensure);
            return null;
        }
        display = location.getWorld().spawn(location, TextDisplay.class, entity -> {
            entity.setPersistent(false);
            entity.setInvulnerable(true);
            entity.setSilent(true);
            entity.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
            entity.setSeeThrough(true);
            entity.setShadowed(false);
            entity.setAlignment(TextDisplay.TextAlignment.CENTER);
            entity.setLineWidth(240);
            entity.addScoreboardTag("pit_npc_text");
        });
        applyText();
        return display;
    }

    private void applyText() {
        if (display == null || !display.isValid()) {
            return;
        }
        String joined = String.join("\n", text);
        display.text(LegacyComponentSerializer.legacySection().deserialize(joined));
    }

    public List<String> getText() {
        return text;
    }

    public Component asComponent() {
        return LegacyComponentSerializer.legacySection().deserialize(String.join("\n", text));
    }
}

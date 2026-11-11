/*
 * Copyright (c) 2018 Jitse Boonstra
 */

package cn.mythicland.npclib.api.skin;

import org.bukkit.entity.Player;

import java.util.Optional;

public final class PlayerSkinService {
    public Optional<Skin> resolve(Player player) {
        return player.getPlayerProfile().getProperties().stream()
                .filter(property -> "textures".equals(property.getName()))
                .filter(property -> property.getValue() != null && !property.getValue().isBlank())
                .filter(property -> property.getSignature() != null && !property.getSignature().isBlank())
                .findFirst()
                .map(property -> new Skin(property.getValue(), property.getSignature()));
    }
}

package net.mizukilab.pit.util.nametag;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

@Getter
@Setter
public class BufferedNametag {

    private String groupName, prefix, suffix;
    private boolean friendlyInvis;
    private Player player;
    private boolean hideNameTag;

    public BufferedNametag(String groupName, String prefix, String suffix, boolean friendlyInvis, Player player) {
        this(groupName, prefix, suffix, friendlyInvis, player, false);
    }

    public BufferedNametag(String groupName, String prefix, String suffix, boolean friendlyInvis, Player player, boolean hideNameTag) {
        this.groupName = groupName;
        this.prefix = prefix;
        this.suffix = suffix;
        this.friendlyInvis = friendlyInvis;
        this.player = player;
        this.hideNameTag = hideNameTag;
    }

}

package cn.charlotte.pit.data.sub;

import cn.charlotte.pit.data.deserializer.PlayerInvDeserializer;
import cn.charlotte.pit.data.serializer.PlayerInvSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.mizukilab.pit.util.inventory.InventoryUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * @Author: EmptyIrony
 * @Date: 2020/12/30 23:41
 */
@JsonSerialize(using = PlayerInvSerializer.class)
@JsonDeserialize(using = PlayerInvDeserializer.class)
public class PlayerInv {

    private ItemStack[] contents;
    private ItemStack[] armorContents;

    public PlayerInv() {
        this.contents = new ItemStack[36];
        this.armorContents = new ItemStack[4];
    }

    public PlayerInv(ItemStack[] contents, ItemStack[] armorContents) {
        this.contents = contents;
        this.armorContents = armorContents;
    }

    public static PlayerInv fromPlayerInventory(org.bukkit.inventory.PlayerInventory inv) {
        return new PlayerInv(inv.getContents(), inv.getArmorContents());
    }

    public static PlayerInv deserialization(String input) {
        return InventoryUtil.playerInventoryFromString(input);
    }

    public void applyItemToPlayer(Player player) {
        // 顺序很重要：getContents() 在现代 Paper 含护甲槽(36-39)，
        // 若先设护甲再设背包，背包里的空护甲槽会把护甲整块覆盖掉。
        org.bukkit.inventory.ItemStack[] safeContents = contents;
        if (safeContents != null && safeContents.length > 36) {
            safeContents = java.util.Arrays.copyOf(safeContents, 36);
        }
        if (safeContents != null) {
            player.getInventory().setContents(safeContents);
        }
        player.getInventory().setArmorContents(armorContents);
        player.setItemOnCursor(null);
    }

    public ItemStack[] getContents() {
        return this.contents;
    }

    public void setContents(ItemStack[] contents) {
        this.contents = contents;
    }

    public ItemStack[] getArmorContents() {
        return this.armorContents;
    }

    public void setArmorContents(ItemStack[] armorContents) {
        this.armorContents = armorContents;
    }

    public ItemStack getHelmet() {
        return this.armorContents[3];
    }

    public ItemStack getChestPiece() {
        return this.armorContents[2];
    }

    public ItemStack getLeggings() {
        return this.armorContents[1];
    }

    public ItemStack getBoots() {
        return this.armorContents[0];
    }

    public String serialize() {
        return InventoryUtil.playerInvToString(this);
    }

    public String toString() {
        return "PlayerInv(contents=" + java.util.Arrays.deepToString(this.getContents()) + ", armorContents=" + java.util.Arrays.deepToString(this.getArmorContents()) + ")";
    }
}


package net.mizukilab.pit.item.type.mythic;

import net.mizukilab.pit.item.IMythicHammer;
import net.mizukilab.pit.item.IMythicItem;
import org.bukkit.Material;

/**
 * 神话之锤：1.21 原生重锤（MACE），常驻高生命 100 / 120 / 135。
 */
public class MythicHammerItem extends IMythicItem implements IMythicHammer {

    public MythicHammerItem() {
        this.maxLive = 100;
        this.live = 100;
    }

    @Override
    public String getInternalName() {
        return "mythic_hammer";
    }

    @Override
    public String getItemDisplayName() {
        return "神话之锤";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return Material.MACE;
    }

    @Override
    public double getItemDamage() {
        return 8.0;
    }

    /**
     * 按物品阶写入常驻高生命：tier 1 = 100 / tier 2 = 120 / tier 3 = 135。
     */
    public void applyTierMaxLive(int tier) {
        int value = switch (tier) {
            case 1 -> 100;
            case 2 -> 120;
            case 3 -> 135;
            default -> 100;
        };
        this.maxLive = value;
        this.live = value;
    }
}

package net.mizukilab.pit.item.type.mythic;

import net.mizukilab.pit.item.IMythicHammer;
import net.mizukilab.pit.item.IMythicItem;
import org.bukkit.Material;

/**
 * 神话之锤物品的最小占位实现。
 *
 * <p>hammer 专属附魔体系（main 分支的 112 个 hammer 类）尚未随 NextGen 迁回，
 * 但 NextGen 的战斗与工具代码里已有对该物品类型的分支，先补一个可编译、
 * 可参与类型判定的实现；hammer 特性迁回后在这里补全数值与附魔逻辑。</p>
 */
public class MythicHammerItem extends IMythicItem implements IMythicHammer {

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
        return Material.GOLDEN_AXE;
    }

    @Override
    public double getItemDamage() {
        return 4.0D;
    }
}

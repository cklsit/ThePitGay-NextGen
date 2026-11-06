package cn.charlotte.pit.compat.item;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

/**
 * 1.8 的“基础材质 + data 值”染色体系在 1.13 被拆成独立材质。
 * 这里提供反向映射，使 {@code new ItemBuilder(Material.WHITE_WOOL).durability(14)}
 * 这类旧写法继续得到红色羊毛。
 */
public final class LegacyColor {

    private static final String[] COLORS = {
            "WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY",
            "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"
    };

    /**
     * 旧版染料顺序与羊毛不同：0 为墨囊（黑），15 为骨粉（白）。
     */
    private static final String[] DYE_COLORS = {
            "BLACK", "RED", "GREEN", "BROWN", "BLUE", "PURPLE", "CYAN", "LIGHT_GRAY",
            "GRAY", "PINK", "LIME", "YELLOW", "LIGHT_BLUE", "MAGENTA", "ORANGE", "WHITE"
    };

    private static final String[] SKULLS = {"SKELETON_SKULL", "WITHER_SKELETON_SKULL", "ZOMBIE_HEAD", "PLAYER_HEAD", "CREEPER_HEAD", "DRAGON_HEAD"};

    private LegacyColor() {
    }

    @Nullable
    public static Material resolve(@Nullable Material base, int data) {
        if (base == null) {
            return null;
        }
        if (data < 0 || data > 15) {
            return null;
        }
        String name = base.name();
        String color = COLORS[data];
        switch (name) {
            case "WHITE_WOOL":
                return byName(color + "_WOOL");
            case "TERRACOTTA":
                return byName(color + "_TERRACOTTA");
            case "WHITE_STAINED_GLASS":
                return byName(color + "_STAINED_GLASS");
            case "WHITE_STAINED_GLASS_PANE":
                return byName(color + "_STAINED_GLASS_PANE");
            case "WHITE_CARPET":
                return byName(color + "_CARPET");
            case "WHITE_BANNER":
                return byName(color + "_BANNER");
            case "WHITE_BED":
                return byName(color + "_BED");
            case "INK_SAC":
                return byName(DYE_COLORS[data] + "_DYE");
            case "PLAYER_HEAD":
                return data < SKULLS.length ? byName(SKULLS[data]) : null;
            default:
                return null;
        }
    }

    @Nullable
    private static Material byName(String name) {
        Material material = Material.matchMaterial(name);
        return material;
    }
}

package cn.charlotte.pit.compat.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * 1.8 通过 NMS {@code ItemSword/ItemBow/ItemArmor} 判定的物品分类，
 * 1.21 改为按材质名判定，语义保持一致。
 */
public final class ItemTypes {

    private ItemTypes() {
    }

    private static String name(ItemStack stack) {
        return stack == null ? "" : stack.getType().name();
    }

    public static boolean isSword(ItemStack stack) {
        return name(stack).endsWith("_SWORD");
    }

    public static boolean isBow(ItemStack stack) {
        String name = name(stack);
        return name.equals("BOW") || name.equals("CROSSBOW");
    }

    public static boolean isArmor(ItemStack stack) {
        String name = name(stack);
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")
                || name.equals("TURTLE_HELMET") || name.equals("ELYTRA");
    }

    public static boolean isHelmet(ItemStack stack) {
        String name = name(stack);
        return name.endsWith("_HELMET") || name.equals("TURTLE_HELMET") || name.equals("CARVED_PUMPKIN");
    }

    public static boolean isPickaxe(ItemStack stack) {
        return name(stack).endsWith("_PICKAXE");
    }

    public static boolean isAxe(ItemStack stack) {
        return name(stack).endsWith("_AXE");
    }

    /**
     * 神话之锤：按 PDC internal name 精确匹配，不按材质家族判定。
     * 普通 MACE / 普通斧都不算。
     */
    public static boolean isMythicHammer(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        return "mythic_hammer".equals(net.mizukilab.pit.util.item.ItemUtil.getInternalName(stack));
    }
}

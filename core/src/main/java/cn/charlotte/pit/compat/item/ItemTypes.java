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
}

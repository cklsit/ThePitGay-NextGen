package net.mizukilab.pit.util.item;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.compat.item.ItemData;
import net.mizukilab.pit.item.AbstractPitItem;
import net.mizukilab.pit.item.IItemFactory;
import net.mizukilab.pit.util.PublicUtil;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 物品数据读写工具。
 * <p>
 * 1.21 适配：原实现直接操作 NMS 的 {@code tag.extra} 复合标签，
 * 现统一改走 {@link ItemData}（PDC 存储），键名与取值语义保持不变。
 *
 * @Author: EmptyIrony
 * @Date: 2021/1/1 1:37
 */
public class ItemUtil {

    public static final String KEY_INTERNAL = "internal";
    public static final String KEY_UUID = "uuid";
    public static final String KEY_VERSION = "version";
    public static final String KEY_ENCH = "ench";

    public static String getUUID(ItemStack item) {
        return ItemData.getString(item, KEY_UUID);
    }

    public static int getHashCodeForUUID(ItemStack item) {
        String uuid = getUUID(item);
        if (uuid == null) {
            return -1;
        }
        long up = PublicUtil.getMostSignificantBits(uuid);
        long down = PublicUtil.getLeastSignificantBits(uuid);
        if (up == 0 || down == 0) {
            return -1;
        }
        return PublicUtil.hashTwoLong(up, down);
    }

    public static UUID getUUIDObj(ItemStack stack) {
        String uuid = getUUID(stack);
        if (uuid == null) return null;
        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public static void setUUIDObj(ItemStack stack, UUID uuid) {
        setUUID(stack, uuid.toString());
    }

    public static void setUUID(ItemStack stack, String uuid) {
        ItemData.set(stack, KEY_UUID, uuid);
    }

    public static void setVer(ItemStack stack, String ver) {
        ItemData.set(stack, KEY_VERSION, ver);
    }

    public static String getVer(ItemStack stack) {
        return ItemData.getString(stack, KEY_VERSION);
    }

    public static boolean shouldUpdateItem(ItemStack stack) {
        String ver = getVer(stack);
        return ver == null || ver.equals("NULL") || !PublicUtil.itemVersion.equals(ver);
    }

    public static UUID randomUUIDItem(ItemStack stack) {
        UUID uuid;

        IItemFactory itemFactory = ThePit.getInstance().getItemFactory();
        if (itemFactory != null) {
            while (true) {
                uuid = UUID.randomUUID();
                AbstractPitItem item = itemFactory.getAbstractPitItem(uuid);
                if (item == null) {
                    break;
                }
            }
        } else {
            uuid = UUID.randomUUID();
        }
        setUUIDObj(stack, uuid);
        return uuid;
    }

    public static void signVer(ItemStack stack) {
        setVer(stack, PublicUtil.itemVersion);
    }

    public static boolean shouldUpdateUUIDAndItem(ItemStack stack) {
        return shouldUpdateItem(stack) && shouldUpdateUUID();
    }

    public static boolean shouldUpdateUUID() {
        return PublicUtil.itemVersion.endsWith("uuid");
    }

    public static boolean isIllegalItem(ItemStack item) {
        if (!ItemData.hasAny(item)) {
            return true;
        }
        String internalName = getInternalName(item);
        return internalName == null || internalName.endsWith("_reward");
    }

    public static boolean canDrop(ItemStack item) {
        return getItemBoolData(item, "tradeAllow");
    }

    public static boolean isHealingItem(ItemStack item) {
        return getItemBoolData(item, "isHealingItem");
    }

    public static boolean canTrade(ItemStack item) {
        if (!ItemData.hasAny(item)) {
            return false;
        }
        if (forceCanTrade(item)) {
            return true;
        }
        String internalName = getInternalName(item);
        boolean b = getItemBoolData(item, "canTrade");
        boolean b1 = (internalName != null && internalName.startsWith("mythic_"));
        return b1 || b;
    }

    public static boolean forceCanTrade(ItemStack item) {
        return getItemBoolData(item, "forceCanTrade");
    }

    public static boolean canSaveEnderChest(ItemStack item) {
        return getItemBoolData(item, "enderChest");
    }

    public static boolean isDefaultItem(ItemStack item) {
        return getItemBoolData(item, "defaultItem");
    }

    public static boolean isDeathDrop(ItemStack item) {
        return getItemBoolData(item, "deathDrop");
    }

    public static boolean isRemovedOnJoin(ItemStack item) {
        return getItemBoolData(item, "removeOnJoin");
    }

    public static boolean isMythicItem(ItemStack stack) {
        return getVer(stack) != null;
    }

    public static String getInternalName(ItemStack item) {
        return ItemData.getString(item, KEY_INTERNAL);
    }

    public static Object[] getInternalNameAndUUID(ItemStack stack) {
        Object[] objects = new Object[2];
        objects[0] = getInternalName(stack);
        objects[1] = getUUID(stack);
        return objects;
    }

    @Nullable
    public static Integer getItemIntData(ItemStack item, String key) {
        return ItemData.getInt(item, key);
    }

    public static boolean getItemBoolData(ItemStack item, String key) {
        return ItemData.getBoolean(item, key);
    }

    @Nullable
    public static String getItemStringData(ItemStack item, String key) {
        return ItemData.getString(item, key);
    }

    public static void setItemData(ItemStack item, String key, Object value) {
        ItemData.set(item, key, value);
    }

    public static boolean hasItemData(ItemStack item, String key) {
        return ItemData.has(item, key);
    }
}

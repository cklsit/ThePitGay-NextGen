package cn.charlotte.pit.compat.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 物品自定义数据访问层。
 * <p>
 * 1.8 时代插件把自定义数据写在 NMS 的 {@code tag.extra} 复合标签里；
 * 1.21 起 NMS 不可用，这里统一改为 {@link PersistentDataContainer}（下称 PDC）存储。
 * <p>
 * 键名沿用原有语义，写入时统一转小写并加 {@code x.} 前缀，避免与其它插件冲突。
 * 字符串列表使用 {@link #LIST_SEPARATOR} 拼接，原数据中不存在该分隔符。
 */
public final class ItemData {

    public static final String PREFIX = "x.";
    public static final String LIST_SEPARATOR = "\u0000";

    private ItemData() {
    }

    private static NamespacedKey key(String name) {
        return new NamespacedKey("thepit", PREFIX + name.toLowerCase(Locale.ROOT));
    }

    @Nullable
    private static PersistentDataContainer container(@Nullable ItemStack stack, boolean create) {
        if (stack == null || stack.getType().isAir()) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer();
    }

    public static boolean has(@Nullable ItemStack stack, String name) {
        PersistentDataContainer pdc = container(stack, false);
        return pdc != null && pdc.has(key(name));
    }

    @Nullable
    public static String getString(@Nullable ItemStack stack, String name) {
        PersistentDataContainer pdc = container(stack, false);
        return pdc == null ? null : read(pdc, key(name), PersistentDataType.STRING);
    }

    @Nullable
    public static Integer getInt(@Nullable ItemStack stack, String name) {
        PersistentDataContainer pdc = container(stack, false);
        return pdc == null ? null : read(pdc, key(name), PersistentDataType.INTEGER);
    }

    @Nullable
    public static Long getLong(@Nullable ItemStack stack, String name) {
        PersistentDataContainer pdc = container(stack, false);
        return pdc == null ? null : read(pdc, key(name), PersistentDataType.LONG);
    }

    @Nullable
    public static Double getDouble(@Nullable ItemStack stack, String name) {
        PersistentDataContainer pdc = container(stack, false);
        return pdc == null ? null : read(pdc, key(name), PersistentDataType.DOUBLE);
    }

    public static boolean getBoolean(@Nullable ItemStack stack, String name) {
        PersistentDataContainer pdc = container(stack, false);
        Byte value = pdc == null ? null : read(pdc, key(name), PersistentDataType.BYTE);
        return value != null && value != 0;
    }

    /**
     * 读取 PDC 值，类型不匹配时返回 null。
     *
     * <p>1.8 的 {@code tag.extra} 里同一个键可能先后被写成字符串或数字；
     * 迁移到强类型的 PDC 后，用错类型的读取会抛 {@link IllegalArgumentException}
     * （Paper 的 CraftPersistentDataTypeRegistry 直接断言），
     * 把整条物品/菜单渲染路径打断。这里统一吞掉类型不匹配。</p>
     */
    @Nullable
    private static <T, Z> Z read(PersistentDataContainer pdc, NamespacedKey key, PersistentDataType<T, Z> type) {
        try {
            return pdc.get(key, type);
        } catch (IllegalArgumentException typeMismatch) {
            return null;
        }
    }

    @NotNull
    public static List<String> getStringList(@Nullable ItemStack stack, String name) {
        String raw = getString(stack, name);
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(raw.split(LIST_SEPARATOR, -1)));
    }

    /**
     * 写入数据后必须写回 ItemMeta，否则修改不会生效。
     */
    public static void set(@Nullable ItemStack stack, String name, @Nullable Object value) {
        if (stack == null || stack.getType().isAir()) {
            return;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey key = key(name);
        if (value == null) {
            pdc.remove(key);
        } else if (value instanceof String string) {
            pdc.set(key, PersistentDataType.STRING, string);
        } else if (value instanceof Boolean bool) {
            pdc.set(key, PersistentDataType.BYTE, (byte) (bool ? 1 : 0));
        } else if (value instanceof Integer integer) {
            pdc.set(key, PersistentDataType.INTEGER, integer);
        } else if (value instanceof Long longValue) {
            pdc.set(key, PersistentDataType.LONG, longValue);
        } else if (value instanceof Double doubleValue) {
            pdc.set(key, PersistentDataType.DOUBLE, doubleValue);
        } else if (value instanceof Float floatValue) {
            pdc.set(key, PersistentDataType.DOUBLE, floatValue.doubleValue());
        } else if (value instanceof List<?> list) {
            List<String> strings = new ArrayList<>(list.size());
            for (Object element : list) {
                strings.add(String.valueOf(element));
            }
            pdc.set(key, PersistentDataType.STRING, String.join(LIST_SEPARATOR, strings));
        } else {
            pdc.set(key, PersistentDataType.STRING, String.valueOf(value));
        }
        stack.setItemMeta(meta);
    }

    public static void remove(@Nullable ItemStack stack, String name) {
        if (stack == null || stack.getType().isAir()) {
            return;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().remove(key(name));
        stack.setItemMeta(meta);
    }

    /**
     * 是否存在任意自定义数据，等价于旧实现的 {@code getExtra(item) != null}。
     */
    public static boolean hasAny(@Nullable ItemStack stack) {
        PersistentDataContainer pdc = container(stack, false);
        if (pdc == null) {
            return false;
        }
        for (NamespacedKey namespacedKey : pdc.getKeys()) {
            if ("thepit".equals(namespacedKey.getNamespace()) && namespacedKey.getKey().startsWith(PREFIX)) {
                return true;
            }
        }
        return false;
    }
}

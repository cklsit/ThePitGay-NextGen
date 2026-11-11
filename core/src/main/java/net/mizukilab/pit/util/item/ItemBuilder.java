package net.mizukilab.pit.util.item;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/1 1:04
 */

import cn.charlotte.pit.compat.item.ItemData;
import cn.charlotte.pit.data.sub.EnchantmentRecord;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.mizukilab.pit.enchantment.AbstractEnchantment;
import net.mizukilab.pit.item.IMythicItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.profile.PlayerProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemBuilder {

    private static int dontStack = 0;
    private ItemStack is;

    public ItemBuilder(Material mat) {
        this.is = new ItemStack(mat);
    }

    public ItemBuilder(ItemStack is) {
        this.is = is;
    }

    public ItemBuilder saved(boolean saved) {
        this.changeNbt("saved", saved);
        return this;
    }

    public ItemBuilder material(Material mat) {
        this.is = new ItemStack(mat);
        return this;
    }

    public static String formatExactTime(long timestamp) {
        if (timestamp <= 0) {
            return ChatColor.RED + "已过期";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日HH时mm分");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai")); // 使用北京时间

        Date expireDate = new Date(timestamp);
        return ChatColor.GREEN + sdf.format(expireDate);
    }

    public ItemBuilder amount(int amount) {
        this.is.setAmount(amount);
        return this;
    }

    public ItemBuilder name(String name) {
        ItemMeta meta = this.is.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        this.is.setItemMeta(meta);
        return this;
    }

    public ItemBuilder dontStack() {
        dontStack++;
        return changeNbt("uuid", dontStack);
    }

    public ItemBuilder setLetherColor(Color color) {
        LeatherArmorMeta im = (LeatherArmorMeta) is.getItemMeta();
        im.setColor(color);
        is.setItemMeta(im);
        return this;
    }

    public ItemBuilder setSkullOwner(String owner) {
        SkullMeta im = (SkullMeta) is.getItemMeta();
        im.setOwner(owner);
        is.setItemMeta(im);
        return this;
    }

    public ItemBuilder expireTime(long expireTime) {
        return this.changeNbt("expireTime", expireTime);
    }

    public static String formatTime(long ms) {
        if (ms <= 0) {
            return ChatColor.RED + "已过期";
        }
        long sec = ms / 1000;

        long days = ms / (24 * 60 * 60 * 1000);
        ms %= 24 * 60 * 60 * 1000;

        long hours = ms / (60 * 60 * 1000);
        ms %= 60 * 60 * 1000;

        long minutes = ms / (60 * 1000);

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("天");
        if (hours > 0) sb.append(hours).append("小时");
        if (minutes > 0) sb.append(minutes).append("分钟");
        if (sec > 0) sb.append(sec).append("秒");

        return ChatColor.GREEN + sb.toString();
    }

    public ItemBuilder setSkullProperty(String texture) {
        SkullMeta meta = (SkullMeta) is.getItemMeta();
        try {
            // 1.21 移除 GameProfile 反射写入，改为官方 PlayerProfile 纹理接口
            String json = new String(Base64.getDecoder().decode(texture), StandardCharsets.UTF_8);
            Matcher matcher = SKIN_URL_PATTERN.matcher(json);
            if (matcher.find()) {
                PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "PitTexture");
                profile.getTextures().setSkin(new URL(matcher.group(1)));
                meta.setOwnerProfile(profile);
            }
        } catch (Exception ignored) {
        }
        is.setItemMeta(meta);
        return this;
    }

    private static final Pattern SKIN_URL_PATTERN = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");

    public ItemBuilder lore(String name) {
        ItemMeta meta = this.is.getItemMeta();
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ObjectArrayList<>(2);
        }

        lore.add(ChatColor.translateAlternateColorCodes('&', name));
        meta.setLore(lore);
        this.is.setItemMeta(meta);
        return this;
    }

    public ItemBuilder lore(String... lore) {
        List<String> toSet = new ObjectArrayList<>(lore.length);
        ItemMeta meta = this.is.getItemMeta();

        for (String string : lore) {
            toSet.add(ChatColor.translateAlternateColorCodes('&', string));
        }

        meta.setLore(toSet);
        this.is.setItemMeta(meta);
        return this;
    }

    public ItemBuilder enchant(Enchantment enchantment, int i) {
        this.is.addUnsafeEnchantment(enchantment, i);
        return this;
    }

    public ItemBuilder customName(String customName) {
        return this.changeNbt("customName", customName);
    }

    public ItemBuilder prefix(String prefix) {
        return this.changeNbt("prefix", prefix);
    }

    public ItemBuilder lore(List<String> lore) {
        List<String> toSet = new ObjectArrayList<>(lore.size());
        ItemMeta meta = this.is.getItemMeta();

        for (String string : lore) {
            toSet.add(ChatColor.translateAlternateColorCodes('&', string));
        }

        meta.setLore(toSet);
        this.is.setItemMeta(meta);
        return this;
    }

    public ItemBuilder durability(int durability) {
        // 1.13 起染色材质被拆分，旧写法（基础材质 + data）在此还原为具体颜色变体
        Material colored = resolveLegacyColor(this.is.getType(), durability);
        if (colored != null) {
            ItemStack replacement = new ItemStack(colored, this.is.getAmount());
            if (this.is.hasItemMeta()) {
                replacement.setItemMeta(this.is.getItemMeta());
            }
            this.is = replacement;
            return this;
        }
        this.is.setDurability((short) durability);
        return this;
    }

    private static final String[] LEGACY_COLORS = {
            "WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY",
            "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"
    };

    /**
     * 旧版染料 data 顺序与羊毛不同：0 为墨囊（黑），15 为骨粉（白）。
     */
    private static final String[] LEGACY_DYE_COLORS = {
            "BLACK", "RED", "GREEN", "BROWN", "BLUE", "PURPLE", "CYAN", "LIGHT_GRAY",
            "GRAY", "PINK", "LIME", "YELLOW", "LIGHT_BLUE", "MAGENTA", "ORANGE", "WHITE"
    };

    private static final String[] LEGACY_SKULLS = {
            "SKELETON_SKULL", "WITHER_SKELETON_SKULL", "ZOMBIE_HEAD", "PLAYER_HEAD", "CREEPER_HEAD", "DRAGON_HEAD"
    };

    @Nullable
    private static Material resolveLegacyColor(@Nullable Material base, int data) {
        if (base == null || data < 0 || data > 15) {
            return null;
        }
        String name = base.name();
        String color = LEGACY_COLORS[data];
        switch (name) {
            case "WHITE_WOOL":
                return Material.matchMaterial(color + "_WOOL");
            case "TERRACOTTA":
                return Material.matchMaterial(color + "_TERRACOTTA");
            case "WHITE_STAINED_GLASS":
                return Material.matchMaterial(color + "_STAINED_GLASS");
            case "WHITE_STAINED_GLASS_PANE":
                return Material.matchMaterial(color + "_STAINED_GLASS_PANE");
            case "WHITE_CARPET":
                return Material.matchMaterial(color + "_CARPET");
            case "WHITE_BANNER":
                return Material.matchMaterial(color + "_BANNER");
            case "WHITE_BED":
                return Material.matchMaterial(color + "_BED");
            case "INK_SAC":
                return Material.matchMaterial(LEGACY_DYE_COLORS[data] + "_DYE");
            case "PLAYER_HEAD":
                return data < LEGACY_SKULLS.length ? Material.matchMaterial(LEGACY_SKULLS[data]) : null;
            default:
                return null;
        }
    }

    public ItemBuilder jewelSwordKills(int kills) {
        this.changeNbt("killed", kills);
        return this;
    }

    public ItemBuilder makeBoostedByGem(boolean boosted) {
        this.changeNbt("boostedByGem", boosted);
        return this;
    }

    public ItemBuilder makeBoostedByGlobalGem(boolean boosted) {
        this.changeNbt("boostedByGlobalGem", boosted);
        return this;
    }

    public ItemBuilder makeBoostedByBook(boolean boosted) {
        this.changeNbt("boostedByBook", boosted);
        return this;
    }

    public ItemBuilder recordEnchantments(List<EnchantmentRecord> records) {
        final StringBuilder builder = new StringBuilder();

        if (records.size() > 5) {
            records = records.subList(records.size() - 5, records.size());
        }

        for (EnchantmentRecord record : records) {
            if (!builder.isEmpty()) {
                builder.append(";");
            }

            builder.append(record.getEnchanter())
                    .append("|")
                    .append(record.getDescription())
                    .append("|")
                    .append(record.getTimestamp());
        }

        this.changeNbt("records", builder.toString());

        return this;
    }

    public ItemBuilder enchantment(Enchantment enchantment, int level) {
        this.is.addUnsafeEnchantment(enchantment, level);
        return this;
    }

    public ItemBuilder canReplace(boolean allow) {
        this.changeNbt("canReplace", allow);
        return this;
    }

    public ItemBuilder enchantment(Enchantment enchantment) {
        this.is.addUnsafeEnchantment(enchantment, 1);
        return this;
    }

    public ItemBuilder shiny() {
        return this.enchant(Enchantment.LURE, 1).flags(ItemFlag.values());
    }

    public ItemBuilder flags(ItemFlag... flags) {
        ItemMeta itemMeta = this.is.getItemMeta();
        itemMeta.addItemFlags(flags);
        this.is.setItemMeta(itemMeta);
        return this;
    }

    public ItemBuilder type(Material material) {
        this.is.setType(material);
        return this;
    }

    public ItemBuilder clearLore() {
        ItemMeta meta = this.is.getItemMeta();
        meta.setLore(new ArrayList<>());
        this.is.setItemMeta(meta);
        return this;
    }

    public ItemBuilder clearEnchantments() {

        for (Enchantment e : this.is.getEnchantments().keySet()) {
            this.is.removeEnchantment(e);
        }

        return this;
    }

    public ItemBuilder canDrop(boolean allow) {
        this.changeNbt("tradeAllow", allow);
        return this;
    }

    public ItemBuilder isHealingItem(boolean isHealingItem) {
        this.changeNbt("isHealingItem", isHealingItem);
        return this;
    }

    public ItemBuilder canTrade(boolean allow) {
        this.changeNbt("canTrade", allow);
        return this;
    }

    public ItemBuilder forceCanTrade(boolean allow) {
        this.changeNbt("forceCanTrade", allow);
        return this;
    }

    public ItemBuilder unsetForceCanTrade() {
        this.changeNbt("forceCanTrade", null);
        return this;
    }

    public ItemBuilder defaultItem() {
        this.changeNbt("defaultItem", true);
        return this;
    }

    public ItemBuilder canSaveToEnderChest(boolean allow) {
        this.changeNbt("enderChest", allow);
        return this;
    }

    public ItemBuilder changeNbt(String key, String value) {
        // 1.21：NMS tag.extra 迁移到 PDC（ItemData）
        ItemData.set(is, key, value);
        return this;
    }

    public ItemBuilder changeNbt(String key, boolean value) {
        ItemData.set(is, key, value);
        return this;
    }

    public ItemBuilder changeNbt(String key, long value) {
        ItemData.set(is, key, value);
        return this;
    }

    public ItemBuilder changeNbt(String key, int value) {
        // Addon card redemption writes stats after caching the enchanted template.
        // Invalidate on edits so its inventory refresh cannot restore the old stats.
        // Fresh serialization has no previous field and must keep the live cache.
        Integer previous = ItemData.getInt(is, key);
        if (previous != null && previous != value
                && ("tier".equals(key) || "live".equals(key) || "maxLive".equals(key))) {
            IMythicItem.clearCache(is);
        }
        ItemData.set(is, key, value);
        return this;
    }

    public ItemBuilder changeNbt(String key, double value) {
        ItemData.set(is, key, value);
        return this;
    }

    public ItemStack buildWithUnbreakable() {
        ItemMeta meta = is.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            is.setItemMeta(meta);
        }
        return is;
    }

    public ItemBuilder internalName(String name) {
        this.changeNbt("internal", name);
        return this;
    }

    public ItemBuilder removeOnJoin(boolean remove) {
        this.changeNbt("removeOnJoin", remove);
        return this;
    }

    public ItemBuilder uuid(UUID uuid) {
        //this.changeNbt("uuid", uuid.toString());
        ItemUtil.setUUIDObj(is, uuid);
        return this;
    }

    public ItemStack build() {
        return is;
    }

    public ItemBuilder deathDrop(boolean drop) {
        this.changeNbt("deathDrop", drop);
        return this;
    }

    public ItemBuilder version(String version) {
        this.changeNbt("version", version);
        return this;
    }

    public ItemBuilder enchant(Map<AbstractEnchantment, Integer> enchant) {
        List<String> records = new ArrayList<>(enchant.size());
        for (Map.Entry<AbstractEnchantment, Integer> entry : enchant.entrySet()) {
            records.add(entry.getKey().getNbtName() + ":" + entry.getValue());
        }
        ItemData.set(is, ItemUtil.KEY_ENCH, records);
        return this;
    }

    public ItemBuilder maxLive(int live) {
        this.changeNbt("maxLive", live);
        return this;
    }

    public ItemBuilder dyeColor(String color) {
        this.changeNbt("dyeColor", color);
        return this;
    }

    public ItemBuilder tier(int tier) {
        this.changeNbt("tier", tier);
        return this;
    }

    public ItemBuilder live(int live) {
        this.changeNbt("live", live);
        return this;
    }

    public ItemBuilder itemDamage(double damageValue) {
        ItemMeta meta = is.getItemMeta();
        if (meta != null) {
            // 原实现写 AttributeModifiers NBT，1.21 使用官方属性修饰符 API
            meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
            meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(
                    new NamespacedKey("thepit", "item_damage"),
                    damageValue,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.MAINHAND));
            is.setItemMeta(meta);
        }
        return this;
    }

    /**
     * 攻击速度修正（标量）：value = 0.05 表示 +5%。固定 key + 先 remove 再 add，避免重复叠加覆盖。
     */
    public ItemBuilder attackSpeed(double value) {
        ItemMeta meta = is.getItemMeta();
        if (meta != null) {
            NamespacedKey key = new NamespacedKey("thepit", "item_attack_speed");
            java.util.Collection<AttributeModifier> existing = meta.getAttributeModifiers(Attribute.ATTACK_SPEED);
            if (existing != null) {
                for (AttributeModifier mod : new ArrayList<>(existing)) {
                    if (key.equals(mod.getKey())) {
                        meta.removeAttributeModifier(Attribute.ATTACK_SPEED, mod);
                    }
                }
            }
            meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(
                    key,
                    value,
                    AttributeModifier.Operation.ADD_SCALAR,
                    EquipmentSlotGroup.MAINHAND));
            is.setItemMeta(meta);
        }
        return this;
    }

    /**
     * 击退抗性（加法）：value = 0.3 表示 +30%。固定 key + 先 remove 再 add。
     */
    public ItemBuilder knockbackResistance(double value) {
        ItemMeta meta = is.getItemMeta();
        if (meta != null) {
            NamespacedKey key = new NamespacedKey("thepit", "item_kb_resistance");
            java.util.Collection<AttributeModifier> existing = meta.getAttributeModifiers(Attribute.KNOCKBACK_RESISTANCE);
            if (existing != null) {
                for (AttributeModifier mod : new ArrayList<>(existing)) {
                    if (key.equals(mod.getKey())) {
                        meta.removeAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, mod);
                    }
                }
            }
            meta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, new AttributeModifier(
                    key, value, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
            is.setItemMeta(meta);
        }
        return this;
    }

    /**
     * 实体交互距离（加法）：value = 0.5 表示有效命中范围 +0.5 格。固定 key + 先 remove 再 add。
     */
    public ItemBuilder entityInteractionRange(double value) {
        ItemMeta meta = is.getItemMeta();
        if (meta != null) {
            NamespacedKey key = new NamespacedKey("thepit", "item_interaction_range");
            java.util.Collection<AttributeModifier> existing = meta.getAttributeModifiers(Attribute.ENTITY_INTERACTION_RANGE);
            if (existing != null) {
                for (AttributeModifier mod : new ArrayList<>(existing)) {
                    if (key.equals(mod.getKey())) {
                        meta.removeAttributeModifier(Attribute.ENTITY_INTERACTION_RANGE, mod);
                    }
                }
            }
            meta.addAttributeModifier(Attribute.ENTITY_INTERACTION_RANGE, new AttributeModifier(
                    key, value, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
            is.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder addPotionEffect(PotionEffect effect, boolean b) {
        if (this.is.getItemMeta() instanceof PotionMeta) {
            final PotionMeta meta = (PotionMeta) this.is.getItemMeta();
            meta.addCustomEffect(effect, b);
            this.is.setItemMeta(meta);
        }

        return this;
    }
}

package net.mizukilab.pit.util.item;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/1 1:04
 */

import cn.charlotte.pit.data.sub.EnchantmentRecord;
import cn.charlotte.pit.compat.item.ItemData;
import cn.charlotte.pit.compat.item.LegacyColor;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.mizukilab.pit.enchantment.AbstractEnchantment;
import net.mizukilab.pit.util.PublicUtil;
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

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
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

    public ItemBuilder material(Material mat) {
        this.is = new ItemStack(mat);
        return this;
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
        Material colored = LegacyColor.resolve(this.is.getType(), durability);
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

    public ItemBuilder changeNbt(String key, int value) {
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
        ItemUtil.setUUIDObj(is,uuid);
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

    public ItemBuilder addPotionEffect(PotionEffect effect, boolean b) {
        if (this.is.getItemMeta() instanceof PotionMeta) {
            final PotionMeta meta = (PotionMeta) this.is.getItemMeta();
            meta.addCustomEffect(effect, b);
            this.is.setItemMeta(meta);
        }

        return this;
    }
}

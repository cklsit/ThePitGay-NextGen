package net.mizukilab.pit.item;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.compat.item.ItemData;
import cn.charlotte.pit.data.sub.EnchantmentRecord;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import net.mizukilab.pit.config.NewConfiguration;
import net.mizukilab.pit.enchantment.AbstractEnchantment;
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity;
import net.mizukilab.pit.item.type.ArmageddonBoots;
import net.mizukilab.pit.item.type.mythic.MagicFishingRod;
import net.mizukilab.pit.item.type.mythic.MythicBowItem;
import net.mizukilab.pit.item.type.mythic.MythicLeggingsItem;
import net.mizukilab.pit.item.type.mythic.MythicSwordItem;
import net.mizukilab.pit.util.Utils;
import net.mizukilab.pit.util.chat.RomanUtil;
import net.mizukilab.pit.util.item.ItemBuilder;
import net.mizukilab.pit.util.random.RandomUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @Author: EmptyIrony
 * @Date: 2021/3/23 23:45
 */
@Data()
@Getter
@Setter

public abstract class IMythicItem extends AbstractPitItem {

    public int maxLive;
    public int live;
    public int tier;
    public MythicColor color;
    public DyeColor dyeColor;
    public String version;
    public String prefix;
    public boolean boostedByGem = false;
    @Getter
    private final static String defUUIDString = "00000000-0000-0000-0000-000000000001";
    @Getter
    private final static UUID defUUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public String customName = null;

    public boolean boostedByBook = false;

    public UUID uuid;

    // 0=false 1=true -1=unset

    public int forceCanTrade = -1;

    public IMythicItem() {
    }

    public static void clearCache(ItemStack e) {
    }

    public void resetUUID() {
        this.uuid = defUUID;
    }

    @Override
    public ItemStack toItemStack() {
        List<String> lore = new ObjectArrayList<>();
        String name = getItemDisplayName();
        if (this.color == null) {
            this.color = (MythicColor) RandomUtil.helpMeToChooseOne(MythicColor.RED
                    , MythicColor.ORANGE, MythicColor.BLUE, MythicColor.GREEN, MythicColor.YELLOW);
        }

        //Guardian Enchant for Archangel Chestplate

        if (isEnchanted()) {
            if (tier == 0) {
                tier = 3;
            }
        }

        if (this instanceof MythicLeggingsItem) {
            name = color.getChatColor() + (tier > 0 ? RomanUtil.convert(tier) + " 阶" : "") + color.getDisplayName() + "色" + getItemDisplayName();
            if (dyeColor != null) {
                name = dyeColor.getChatColor() + (tier > 0 ? RomanUtil.convert(tier) + " 阶" : "") + "染色神话之甲";
            }
            if (color == MythicColor.DARK) {
                name = color.getChatColor() + (tier > 0 ? RomanUtil.convert(tier) + " 阶" : "") + "暗黑之甲";
            }
            if (color == MythicColor.RAGE) {
                name = color.getChatColor() + (tier > 0 ? RomanUtil.convert(tier) + " 阶" : "") + "暴怒之甲";
            }
            if (color == MythicColor.DARK_GREEN) {
                name = color.getChatColor() + (tier > 0 ? RomanUtil.convert(tier) + " 阶" : "") + "下水道之甲";
            }
        } else if (this instanceof MythicSwordItem) {
            name = (tier >= 3 ? "&c" : "&e") + (tier > 0 ? RomanUtil.convert(tier) + " 阶" : "") + getItemDisplayName();
        } else if (this instanceof net.mizukilab.pit.item.IMythicHammer) {
            name = (tier >= 3 ? "&c" : "&6") + (tier > 0 ? RomanUtil.convert(tier) + " 阶" : "") + getItemDisplayName();
        } else if (this instanceof MythicBowItem) {
            name = (tier >= 3 ? "&c" : "&b") + (tier > 0 ? RomanUtil.convert(tier) + " 阶" : "") + getItemDisplayName();
        } else if (this instanceof MagicFishingRod) {
            name = (tier >= 3 ? "&c" : "&a") + (tier > 0 ? RomanUtil.convert(tier) + " 阶" : "") + getItemDisplayName();
        }

        int rareAmount = 0;
        int opAmount = 0;
        int uberAmount = 0;
        int enchantTotalLevel = 0;


        for (AbstractEnchantment abstractEnchantment : enchantments.keySet()) {
            int enchantLevel = enchantments.get(abstractEnchantment);
            enchantTotalLevel += enchantLevel;

            if (abstractEnchantment.getRarity() == EnchantmentRarity.RARE) {
                rareAmount++;
                if (color == MythicColor.RAGE && abstractEnchantment.getMaxEnchantLevel() == enchantLevel) {
                    this.prefix = "不可思议的";
                }
            } else if (abstractEnchantment.getRarity() == EnchantmentRarity.OP) {
                opAmount++;
            } else if (abstractEnchantment.getRarity() == EnchantmentRarity.UBER_LIMITED_RARE || abstractEnchantment.getRarity() == EnchantmentRarity.UBER_LIMITED) {
                uberAmount++;
            }
        }

        if (enchantTotalLevel >= 8) {
            this.prefix = switch (color) {
                case RAGE -> "狂躁的";
                case DARK -> "邪恶的";
                default -> "传说中的";
            };
        }

        if (this.maxLive >= 100) {
            if (this instanceof net.mizukilab.pit.item.IMythicHammer) {
                // 神话之锤本身就是高生命物品，「精制的」这类前缀对它是冗余信息
            } else if (color == MythicColor.DARK) {
                name = color.getChatColor() + (tier > 0 ? RomanUtil.convert(tier) + " 阶" : "") + "恶魔之甲";
            } else {
                this.prefix = "精制的";
            }
        }

        if (rareAmount >= 3 && this.maxLive >= 100 && !(this instanceof net.mizukilab.pit.item.IMythicHammer)) {
            this.prefix = "万里挑一的";
        } else if (rareAmount == 2 && this.maxLive >= 100 && !(this instanceof net.mizukilab.pit.item.IMythicHammer)) {
            this.prefix = "奇迹般的";
        } else if (rareAmount >= 3) {
            this.prefix = "不朽的";
        } else if (rareAmount == 2) {
            this.prefix = "不凡的";
        }

        if (enchantTotalLevel >= 7 && this.maxLive >= 100 && !(this instanceof net.mizukilab.pit.item.IMythicHammer)) {
            this.prefix = "强大的";
        }

        if (uberAmount >= 1) {
            this.prefix = "登峰造极的";
        }
        if (opAmount >= 1) {
            this.prefix = "可怕的";
        }
        if (this.prefix != null) {
            name = name.substring(0, 2) + this.prefix + " " + name;
        }


        if (this.customName != null) {
            name = customName;
        }

        // 未附魔的神话物品不显示生命行（生命值只在附魔后才有意义）
        if (maxLive != 0 && isEnchanted()) {
            lore.add(("&7生命: " + (live / (maxLive * 1.0) <= 0.6 ? (live / (maxLive * 1.0) <= 0.3 ? "&c" : "&e") : "&a") + live + "&7/" + maxLive)
                    + (isBoostedByGem() ? "&a ♦" : "") + (isBoostedByGlobalGem() ? "&b ♦" : "") + (boostedByBook ? "&6 ᥀" : ""));
            lore.add("");
        }

        if (isEnchanted()) {
            final AbstractEnchantment somber = ThePit.getInstance().getEnchantmentFactor().getEnchantmentMap().get("somber_enchant");
            if (color == MythicColor.DARK && !enchantments.containsKey(somber)) {
                enchantments.put(somber, 1);
            }
            boolean genesisFound = false;

            for (Map.Entry<AbstractEnchantment, Integer> entry : enchantments.entrySet()) {
                getEnchantLore(lore, entry, enchantments.entrySet());
                if (entry.getKey().getRarity() == EnchantmentRarity.GENESIS) {
                    genesisFound = true;
                }
            }

            if (this instanceof MythicLeggingsItem) {
                if (color != MythicColor.DARK) {
                    lore.add((dyeColor == null ? color.getChatColor() : dyeColor.getChatColor()) + "穿着时提供与铁护腿相同的伤害减免效果 &8| " + NewConfiguration.INSTANCE.getWatermarks());
                } else {
                    lore.add((dyeColor == null ? color.getChatColor() : dyeColor.getChatColor()) + "穿着时提供与皮革护腿相同的伤害减免效果 &8| " + NewConfiguration.INSTANCE.getWatermarks());
                }
            } else {
                lore.add(NewConfiguration.INSTANCE.getWatermarks());
            }

            if (genesisFound) {
                lore.add(color.getChatColor() + "阵营活动奖励");
            }

        } else {
            lore.add("&7死亡后保留");
            lore.add("");
            if (this instanceof MythicLeggingsItem) {
                lore.add((dyeColor == null ? color.getChatColor() : dyeColor.getChatColor()) + "在神话之井中附魔");
                lore.add((dyeColor == null ? color.getChatColor() : dyeColor.getChatColor()) + "同时,也是一种象征 &8| " + NewConfiguration.INSTANCE.getWatermarks());
            } else {
                lore.add("&7在神话之井中附魔 &8| " + ThePit.getApi().getWatermark());
            }
            this.tier = 0;
        }

        if (dyeColor != null && this instanceof MythicLeggingsItem) {
            lore.add("&7原: " + color.getChatColor() + color.getDisplayName() + "色神话之甲");
        }
        //Dark Pants
        ItemBuilder builder = new ItemBuilder(this.getItemDisplayMaterial());
        if (name != null) {
            builder.name(name);
        }
        if (customName != null) {
            builder.customName(customName);
        }
        if (uuid != null) {
            boolean equals = uuid == null || defUUID.equals(uuid);
            lore.add("&8" + (equals ? "Refresh on table" : uuid.toString()));
        }

        // 神话之锤：汇总攻速（C2）、击退抗性（12/63）与命中范围（35）到物品属性
        double atsScalar = 0.0D;
        double kbResistance = 0.0D;
        double reachBonus = 0.0D;
        for (Map.Entry<AbstractEnchantment, Integer> e : enchantments.entrySet()) {
            if (!(e.getKey() instanceof net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase hb)) {
                continue;
            }
            int lv = e.getValue();
            double v = hb.valueAt(lv);
            String nbt = hb.getNbtName();
            switch (nbt) {
                case "hammer_023", "hammer_065" -> {
                    double t0 = 1.0D / 0.6D;
                    atsScalar += t0 / (t0 + v) - 1.0D;
                }
                case "hammer_051" -> atsScalar += v / 100.0D;
                case "hammer_012", "hammer_063" -> kbResistance += v / 100.0D;
                case "hammer_035" -> reachBonus += v;
                default -> {
                }
            }
        }
        atsScalar = Math.max(-0.30D, Math.min(0.30D, atsScalar));
        kbResistance = Math.max(0.0D, Math.min(1.0D, kbResistance));
        reachBonus = Math.max(0.0D, Math.min(0.7D, reachBonus));

        double mythicItemDamage = this instanceof IMythicSword sword ? sword.getItemDamage()
                : (this instanceof net.mizukilab.pit.item.IMythicHammer hammer ? hammer.getItemDamage() : -1.0D);
        if (mythicItemDamage >= 0) {
            builder
                    .lore(lore)
                    .internalName(getInternalName())
                    .deathDrop(false)
                    .version(version == null ? "NULL" : version)
                    .canSaveToEnderChest(true)
                    .removeOnJoin(false)
                    .uuid(uuid == null ? defUUID : uuid)
                    .canDrop(false)
                    .canTrade(true)
                    .enchant(enchantments)
                    .itemDamage(mythicItemDamage)
                    .attackSpeed(atsScalar)
                    .knockbackResistance(kbResistance)
                    .entityInteractionRange(reachBonus)
                    .maxLive(this.maxLive)
                    .tier(this.tier)
                    .makeBoostedByGem(this.boostedByGem)
                    .makeBoostedByGlobalGem(this.boostedByGlobalGem)
                    .makeBoostedByBook(boostedByBook)
                    .live(this.live)
                    .recordEnchantments(enchantmentRecords);
        } else {
            builder
                    .lore(lore)
                    .internalName(getInternalName())
                    .deathDrop(false)
                    .version(version == null ? "NULL" : version)
                    .uuid(uuid == null ? defUUID : uuid)
                    .canDrop(false)
                    .canTrade(true)
                    .canSaveToEnderChest(true)
                    .removeOnJoin(false)
                    .enchant(enchantments)
                    .maxLive(this.maxLive)
                    .live(this.live)
                    .makeBoostedByGem(this.boostedByGem)
                    .makeBoostedByGlobalGem(this.boostedByGlobalGem)
                    .makeBoostedByBook(boostedByBook)
                    .tier(this.tier)
                    .recordEnchantments(enchantmentRecords);
        }

        if (dyeColor != null) {
            builder.dyeColor(dyeColor.name());
        }

        if (isEnchanted()) {
            builder.shiny();
        }
        this.setMythicColor(builder, color);
        if (this instanceof MythicLeggingsItem || this instanceof ArmageddonBoots) {
            builder.setLetherColor(dyeColor == null ? color.getLeatherColor() : dyeColor.getColor());
        }

        if (this.prefix != null) {
            builder.prefix(prefix);
        }
        if (forceCanTrade == 0) {
            builder.forceCanTrade(false);
        } else if (forceCanTrade == 1) {
            builder.forceCanTrade(true);
        } else {
            builder.unsetForceCanTrade();
        }

        return builder
                .buildWithUnbreakable();
    }

    public boolean isBoostedByGlobalGem() {
        return this.boostedByGlobalGem;
    }

    @Override
    public void loadFromItemStack(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        // 1.21：物品数据由 NBT 迁移到 PDC，统一走 ItemData
        if (!ItemData.hasAny(item)) {
            return;
        }

        this.boostedByGem = ItemData.getBoolean(item, "boostedByGem");
        this.boostedByGlobalGem = ItemData.getBoolean(item, "boostedByGlobalGem");
        this.boostedByBook = ItemData.getBoolean(item, "boostedByBook");

        String prefixValue = ItemData.getString(item, "prefix");
        if (prefixValue != null) {
            this.prefix = prefixValue;
        }

        String customNameValue = ItemData.getString(item, "customName");
        if (customNameValue != null) {
            this.customName = customNameValue;
        }

        String versionValue = ItemData.getString(item, "version");
        if (versionValue != null) {
            this.version = versionValue;
        }

        String dyeColorValue = ItemData.getString(item, "dyeColor");
        if (dyeColorValue != null) {
            this.dyeColor = DyeColor.valueOf(dyeColorValue);
        }

        String uuidValue = ItemData.getString(item, "uuid");
        if (uuidValue != null) {
            this.uuid = UUID.fromString(uuidValue);
        }

        String mythicColor = ItemData.getString(item, "mythic_color");
        if (mythicColor == null) {
            this.color = (MythicColor) RandomUtil.helpMeToChooseOne(
                    MythicColor.RED, MythicColor.ORANGE, MythicColor.BLUE, MythicColor.GREEN, MythicColor.YELLOW);
        } else {
            this.color = MythicColor.valueOfInternalName(mythicColor);
            if (color == null) {
                this.color = (MythicColor) RandomUtil.helpMeToChooseOne(
                        MythicColor.RED, MythicColor.ORANGE, MythicColor.BLUE, MythicColor.GREEN, MythicColor.YELLOW);
            }
        }

        Integer maxLiveValue = ItemData.getInt(item, "maxLive");
        this.maxLive = maxLiveValue == null ? 0 : maxLiveValue;
        Integer liveValue = ItemData.getInt(item, "live");
        this.live = liveValue == null ? 0 : liveValue;
        // 存量物品自愈：历史版本重铸时可能把 live 补到上限之上
        if (this.maxLive > 0 && this.live > this.maxLive) {
            this.live = this.maxLive;
        }
        if (ItemData.has(item, "forceCanTrade")) {
            this.forceCanTrade = ItemData.getBoolean(item, "forceCanTrade") ? 1 : 0;
        }

        if (!ItemData.has(item, "ench")) {
            return;
        }

        String recordsString = ItemData.getString(item, "records");
        if (recordsString != null) {
            for (String recordString : Utils.splitByCharAt(recordsString, ';')) {
                final String[] split = Utils.splitByCharAt(recordString, '|');
                if (split.length >= 3) {
                    enchantmentRecords.add(new EnchantmentRecord(split[0], split[1], Long.parseLong(split[2])));
                }
            }
        }

        this.enchantments = new Object2IntOpenHashMap<>();
        this.enchantments.defaultReturnValue(-1);
        for (String entry : ItemData.getStringList(item, "ench")) {
            int index = entry.lastIndexOf(':');
            if (index <= 0) {
                continue;
            }
            try {
                cn.charlotte.pit.compat.item.ItemData.set(item, "ench_debug", entry);
                int level = Integer.parseInt(entry.substring(index + 1));
                var enchantment = ThePit.getInstance().getEnchantmentFactor()
                        .getEnchantmentMap().get(entry.substring(0, index));
                if (enchantment != null) {
                    this.enchantments.put(enchantment, level);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        if (!ItemData.has(item, "tier") && isEnchanted()) {
            if (color == MythicColor.DARK) {
                this.tier = 2;
            } else {
                this.tier = 3;
            }
        } else if (ItemData.has(item, "tier")) {
            if (!isEnchanted()) {
                this.tier = 0;
            } else {
                Integer tierValue = ItemData.getInt(item, "tier");
                this.tier = tierValue == null ? 0 : tierValue;
            }
        }
    }

    protected void setMythicColor(ItemBuilder builder, MythicColor color) {
        builder.changeNbt("mythic_color", color.getInternalName());
    }

    public int getEnchantmentLevel(String name) {
        AbstractEnchantment abstractEnchantment = ThePit.getInstance().getEnchantmentFactor().getEnchantmentMap().get(name);
        if (abstractEnchantment != null) {
            return this.enchantments.getInt(abstractEnchantment);
        }
        return -1;
    }

    public int getEnchantmentLevel(AbstractEnchantment abstractEnchantment) {
        return this.enchantments.getInt(abstractEnchantment);
    }

    public String toString() {
        return "IMythicItem(maxLive=" +
                this.getMaxLive() + ", live=" +
                this.getLive() + ", tier=" + this.getTier() + "" +
                ", color=" + this.getColor() + ", dyeColor=" + this.getDyeColor() + ", prefix=" + this.getPrefix() + ")";
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof IMythicItem)) return false;
        final IMythicItem other = (IMythicItem) o;
        if (!other.canEqual((Object) this)) return false;
        if (!super.equals(o)) return false;
        if (this.getMaxLive() != other.getMaxLive()) return false;
        if (this.getLive() != other.getLive()) return false;
        if (this.getTier() != other.getTier()) return false;
        final Object this$color = this.getColor();
        final Object other$color = other.getColor();
        if (this$color == null ? other$color != null : !this$color.equals(other$color)) return false;
        final Object this$dyeColor = this.getDyeColor();
        final Object other$dyeColor = other.getDyeColor();
        if (this$dyeColor == null ? other$dyeColor != null : !this$dyeColor.equals(other$dyeColor)) return false;
        final Object this$prefix = this.getPrefix();
        final Object other$prefix = other.getPrefix();
        if (this$prefix == null ? other$prefix != null : !this$prefix.equals(other$prefix)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof IMythicItem;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        result = result * PRIME + this.getMaxLive();
        result = result * PRIME + this.getLive();
        result = result * PRIME + this.getTier();
        final Object $color = this.getColor();
        result = result * PRIME + ($color == null ? 43 : $color.hashCode());
        final Object $dyeColor = this.getDyeColor();
        result = result * PRIME + ($dyeColor == null ? 43 : $dyeColor.hashCode());
        final Object $prefix = this.getPrefix();
        result = result * PRIME + ($prefix == null ? 43 : $prefix.hashCode());
        return result;
    }

}

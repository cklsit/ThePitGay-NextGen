p = 'core/src/main/java/net/mizukilab/pit/item/IMythicItem.java'
t = open(p, encoding='utf-8').read()

start = t.index('    @Override\n    public void loadFromItemStack(ItemStack item) {')
end = t.index('    protected void setMythicColor(ItemBuilder builder, MythicColor color) {')

new_body = '''    @Override
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
                this.enchantments.put(entry.substring(0, index), Integer.parseInt(entry.substring(index + 1)));
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

'''
t = t[:start] + new_body + t[end:]

if 'import cn.charlotte.pit.compat.item.ItemData;' not in t:
    t = t.replace('import cn.charlotte.pit.ThePit;', 'import cn.charlotte.pit.ThePit;\nimport cn.charlotte.pit.compat.item.ItemData;')

open(p, 'w', encoding='utf-8').write(t)
print('IMythicItem rewritten')

package net.mizukilab.pit.util.inventory;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.PlayerInv;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.mizukilab.pit.util.PlayerUtil;
import net.mizukilab.pit.util.PublicUtil;
import net.mizukilab.pit.util.chat.CC;
import net.mizukilab.pit.util.item.ItemBuilder;
import net.mizukilab.pit.util.item.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * @Author: EmptyIrony
 * @Date: 2020/12/30 23:40
 */
public class InventoryUtil {

    private static final Random random = new Random();

    public static int getInventoryEmptySlots(ItemStack[] itemStacks) {
        int slot = 0;
        for (ItemStack itemStack : itemStacks) {
            if (itemStack == null || itemStack.getType() == Material.AIR) {
                slot++;
            }
        }
        return slot;
    }

    public static int getInventoryFilledSlots(ItemStack[] itemStacks) {
        int slot = 0;
        for (ItemStack itemStack : itemStacks) {
            if (itemStack != null && itemStack.getType() != Material.AIR) {
                slot++;
            }
        }
        return slot;
    }

    public static int getAmountOfItem(Player player, ItemStack item) {
        int amount = 0;
        for (int i = 0; i < 36; i++) {
            if (player.getInventory().getItem(i) != null && player.getInventory().getItem(i).isSimilar(item)) {
                amount += player.getInventory().getItem(i).getAmount();
            }
        }
        return amount;
    }

    public static int getAmountOfItem(Player player, String internalName) {
        int amount = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack itemStack = player.getInventory().getItem(i);
            String internalName1 = ItemUtil.getInternalName(itemStack);
            if (itemStack != null && internalName1 != null && internalName1.equals(internalName)) {
                amount += itemStack.getAmount();
            }
        }
        return amount;
    }
    public static boolean removeItem(Player player, ItemStack item, Integer amount) {
        if (getAmountOfItem(player, item) < amount) return false;
        int requirement = amount;
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack item1 = inventory.getItem(i);
            if (item1 != null && item1.isSimilar(item)) {
                if (item1.getAmount() <= requirement) {
                    requirement -= item1.getAmount();
                    inventory.setItem(i, null);
                } else {
                    inventory.setItem(i, new ItemBuilder(item).amount(item1.getAmount() - requirement).build());
                    return true;
                }
            }
        }
        return requirement <= 0;
    }

    public static boolean removeItem(Player player, String internalName, Integer amount) {
        if (getAmountOfItem(player, internalName) < amount) return false;
        int requirement = amount;
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && internalName.equals(ItemUtil.getInternalName(item))) {
                if (item.getAmount() <= requirement) {
                    requirement -= item.getAmount();
                    inventory.setItem(i, null);
                } else {
                    inventory.setItem(i, new ItemBuilder(item).amount(item.getAmount() - requirement).build());
                    return true;
                }
            }
        }
        return requirement <= 0;
    }

    public static void removeItemWithInternalName(Player player, String name) {
        if (name == null) {
            return;
        }
        if (name.equals(ItemUtil.getInternalName(player.getItemOnCursor()))) {
            player.setItemOnCursor(null);
        }

        if (name.equals(ItemUtil.getInternalName(player.getInventory().getHelmet()))) {
            player.getInventory().setHelmet(null);
        }
        if (name.equals(ItemUtil.getInternalName(player.getInventory().getChestplate()))) {
            player.getInventory().setChestplate(null);
        }
        if (name.equals(ItemUtil.getInternalName(player.getInventory().getLeggings()))) {
            player.getInventory().setLeggings(null);
        }
        if (name.equals(ItemUtil.getInternalName(player.getInventory().getBoots()))) {
            player.getInventory().setBoots(null);
        }

        int index = 0;
        final PlayerInventory inventory = player.getInventory();
        final List<ItemStack> contents = new ObjectArrayList<>(inventory.getContents());
        for (ItemStack itemStack : contents) {
            if (name.equals(ItemUtil.getInternalName(itemStack))) {
                inventory.setItem(index, null);
            }

            index++;
        }
    }

    public static ItemStack[] fixInventoryOrder(ItemStack[] source) {
        ItemStack[] fixed = new ItemStack[36];

        System.arraycopy(source, 0, fixed, 27, 9);
        System.arraycopy(source, 9, fixed, 0, 27);

        return fixed;
    }

    public static PlayerInv playerInventoryFromPlayer(Player player) {
        final PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (profile.isTempInvUsing()) {
            return profile.getInventory();
        }

        PlayerInv inv = new PlayerInv();

        inv.setContents(player.getInventory().getContents());
        inv.setArmorContents(player.getInventory().getArmorContents());

        return inv;
    }

    public static String itemsToString(ItemStack[] items) {
        if (items == null) return "null";

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < items.length; ++i) {
            builder.append(i).append("#").append(serializeItemStack(items[i]))
                    .append((i == items.length - 1) ? "" : ";");
        }

        return builder.toString();
    }

    public static ItemStack[] stringToItems(String in) {
        if (in == null || in.equals("unset") || in.equals("null") || in.equals("'null'")) return null;
        ItemStack[] contents = new ItemStack[in.split(";").length];

        for (String s : PublicUtil.splitByCharAt(in, ';')) {
            int slot = Integer.parseInt(s.split("#")[0]);

            if (s.split("#").length == 1) {
                contents[slot] = null;
            } else {
                contents[slot] = deserializeItemStack(s.split("#")[1]);
            }
        }
        return contents;
    }

    public static String playerInvToString(PlayerInv inv) {
        if (inv == null) return "null";

        StringBuilder builder = new StringBuilder();
        ItemStack[] armor = inv.getArmorContents();
        if (armor == null) {
            armor = new ItemStack[4];
        }

        ItemStack[] armorForLog = inv.getArmorContents();
        cn.charlotte.pit.ThePit.getInstance().getLogger().info("[SerDebug] 序列化护甲 armorLen="
                + (armorForLog == null ? "null" : armorForLog.length)
                + " [0]=" + typeName(armorForLog, 0) + " [1]=" + typeName(armorForLog, 1)
                + " armorSection=" + cn.charlotte.pit.ThePit.getInstance().getLogger().getName());
        serializeItemStack(builder, armor);
        cn.charlotte.pit.ThePit.getInstance().getLogger().info("[SerDebug] armorSection="
                + builder.substring(0, Math.min(builder.length(), 80)));

        if (inv.getContents() == null) {
            inv.setContents(new ItemStack[36]);
        }

        return prependContents(builder, inv.getContents());
    }

    @NotNull
    private static String prependContents(StringBuilder builder, ItemStack[] contents) {
        for (int i = 0; i < contents.length; ++i) {
            builder.append(i).append("#").append(serializeItemStack(contents[i]))
                    .append((i == contents.length - 1) ? "" : ";");
        }

        return builder.toString();
    }

    private static void serializeItemStack(StringBuilder builder, ItemStack[] armor) {
        for (int i = 0; i < armor.length; i++) {
            if (i == 3) {
                if (armor[i] == null) {
                    builder.append(serializeItemStack(new ItemStack(Material.AIR)));
                } else {
                    builder.append(serializeItemStack(armor[3]));
                }
            } else {
                if (armor[i] == null) {
                    builder.append(serializeItemStack(new ItemStack(Material.AIR))).append(";");
                } else {
                    builder.append(serializeItemStack(armor[i])).append(";");
                }
            }
        }

        builder.append("|");
    }

    public static String playerInventoryToString(PlayerInventory inv) {
        StringBuilder builder = new StringBuilder();
        ItemStack[] armor = inv.getArmorContents();

        serializeItemStack(builder, armor);

        return prependContents(builder, inv.getContents());
    }

    public static PlayerInv playerInventoryFromString(String in) {
        if (in == null || in.equals("unset") || in.equals("null") || in.equals("'null'")){
            return null;
        }

        PlayerInv inv = new PlayerInv();

        String[] data = in.split("\\|");
        String[] split = data[0].split(";");
        ItemStack[] armor = new ItemStack[split.length];

        for (int i = 0; i < split.length; ++i) {
            armor[i] = deserializeItemStack(split[i]);
        }

        cn.charlotte.pit.ThePit.getInstance().getLogger().info("[DesDebug] 反序列化护甲 armorLen="
                + armor.length + " [0]=" + typeName(armor, 0) + " [1]=" + typeName(armor, 1)
                + " dataParts=" + data.length + " armorParts=" + split.length);
        inv.setArmorContents(armor);
        String[] split1 = data[1].split(";");
        ItemStack[] contents = new ItemStack[split1.length];

        for (String s : split1) {
            String[] split2 = s.split("#");
            int slot = Integer.parseInt(split2[0]);

            if (split2.length == 1) {
                contents[slot] = null;
            } else {
                contents[slot] = deserializeItemStack(split2[1]);
            }
        }

        inv.setContents(contents);
        return inv;
    }

    public static String inventoryToString(Inventory inv) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < inv.getContents().length; ++i) {
            builder.append(i).append("#").append(serializeItemStack(inv.getContents()[i]));
            if (i != inv.getContents().length - 1) {
                builder.append(";");
            }
        }

        return builder.toString();
    }

    public static Inventory inventoryFromString(String in) {
        Inventory inv = Bukkit.createInventory(null, 54);
        String[] split;
        @SuppressWarnings("unused")
        String[] data = split = in.split(";");

        for (String s : split) {
            String[] info = s.split("#");
            inv.setItem(Integer.parseInt(info[0]), (info.length > 1) ? deserializeItemStack(info[1]) : null);
        }

        return inv;
    }

    public static String serializeItemStack(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().equals(Material.AIR)) return "null";

        try {
            // Paper 1.20.5+ 提供稳定的物品字节序列化，替代手工 NBT 压缩流
            return "B:" + java.util.Base64.getEncoder().encodeToString(itemStack.serializeAsBytes());
        } catch (Throwable e) {
            // 某些物品（如神话之甲）serializeAsBytes 会抛异常；此前直接返回 Java null，
            // 拼进字符串后变成字面量 "null"，导致物品在存档里被抹掉。
            cn.charlotte.pit.ThePit.getInstance().getLogger()
                    .warning("[Inv] serializeAsBytes 失败，回退 legacy: " + itemStack.getType() + " -> " + e);
        }
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            org.bukkit.util.io.BukkitObjectOutputStream boos = new org.bukkit.util.io.BukkitObjectOutputStream(out);
            boos.writeObject(itemStack);
            boos.close();
            return "L:" + java.util.Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Throwable e) {
            cn.charlotte.pit.ThePit.getInstance().getLogger()
                    .severe("[Inv] legacy 序列化同样失败，物品将丢失: " + itemStack.getType() + " -> " + e);
            return "null";
        }
    }


    public static ItemStack deserializeItemStack(String itemStackString) {
        if (itemStackString == null || "null".equals(itemStackString)) return null;

        try {
            if (itemStackString.startsWith("L:")) {
                byte[] legacy = java.util.Base64.getDecoder().decode(itemStackString.substring(2));
                org.bukkit.util.io.BukkitObjectInputStream bois =
                        new org.bukkit.util.io.BukkitObjectInputStream(new java.io.ByteArrayInputStream(legacy));
                Object obj = bois.readObject();
                bois.close();
                return obj instanceof ItemStack ? (ItemStack) obj : null;
            }
            String base64 = itemStackString.startsWith("B:") ? itemStackString.substring(2) : itemStackString;
            return ItemStack.deserializeBytes(java.util.Base64.getDecoder().decode(base64));
        } catch (Throwable e) {
            cn.charlotte.pit.ThePit.getInstance().getLogger()
                    .warning("[Inv] 反序列化失败: " + e);
            return null;
        }
    }

    private static String typeName(ItemStack[] arr, int idx) {
        if (arr == null || arr.length <= idx || arr[idx] == null) return "null";
        return arr[idx].getType().name();
    }

    public static boolean isInvFull(Player player) {
        for (ItemStack itemStack : player.getInventory()) {
            if (itemStack == null || itemStack.getType() == Material.AIR) {
                return false;
            }
        }
        return true;
    }
    public static boolean isInvFull(Player player,int vl) {
        int vlC = 0;
        for (ItemStack itemStack : player.getInventory()) {
            if (itemStack == null || itemStack.getType() == Material.AIR) {
                vlC++;
                if(vlC > vl) {
                    return false;
                }
            }
        }
        return true;
    }

    private static final ItemStack DEFAULT_SWORD = new ItemBuilder(Material.IRON_SWORD).internalName("default_sword").defaultItem().canDrop(false).canSaveToEnderChest(false).buildWithUnbreakable();
    private static final ItemStack DEFAULT_BOW = new ItemBuilder(Material.BOW).internalName("default_bow").defaultItem().canDrop(false).canSaveToEnderChest(false).buildWithUnbreakable();

    //THERE IS NO SUPPLY ITEMS TO DETECT UNFAIR ITEMS
    public static void supplyItems(Player player) {
        int arrowAmount = 0;
        boolean swordFound = false;
        boolean bowFound = false;
        boolean miner = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId()).getChosePerk().entrySet().stream().anyMatch(entry -> entry.getValue().getPerkInternalName().equals("Miner"));
        int cobblestone = 0;
        PlayerProfile playerProfile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());

        int slot = 0;
        int illegalItems = 0;
        PlayerInventory inventory = player.getInventory();
        for (ItemStack item : inventory) {
            if (item == null) {
                slot++;
                continue;
            }
            if (ItemUtil.isIllegalItem(item)) {
                player.getInventory().remove(item);
                illegalItems++;
                slot++;
                continue;
            }

            //fixme: why remove the default item (sword & bow slot will reset)

            if (ItemUtil.isDefaultItem(item)) {
                //player.getInventory().remove(item);
                if (cn.charlotte.pit.compat.item.ItemTypes.isArmor(item)) {
                    inventory.remove(item);
                }
                if (cn.charlotte.pit.compat.item.ItemTypes.isSword(item) && swordFound) {
                    inventory.remove(item);
                    swordFound = false;
                }
                if (cn.charlotte.pit.compat.item.ItemTypes.isBow(item) && bowFound) {
                    inventory.remove(item);
                    swordFound = false;
                }
            }


            if (ItemUtil.isRemovedOnJoin(item)) {
                inventory.remove(item);
                slot++;
                continue;
            }

            if ("default_sword".equals(ItemUtil.getInternalName(item))) {
                swordFound = true;
            } else if ("default_bow".equals(ItemUtil.getInternalName(item))) {
                bowFound = true;
            } else if (item.getType() == Material.ARROW) {
                arrowAmount += item.getAmount();
            }
            if (item.getType() == Material.COBBLESTONE) {
                cobblestone += item.getAmount();
            }
            slot++;
        }


        if (miner) {
            if (cobblestone < 32) {
                inventory.addItem(new ItemBuilder(Material.COBBLESTONE).deathDrop(true).amount(32 - cobblestone).canDrop(false).canSaveToEnderChest(false).internalName("perk_miner").build());
            }
        }
        for (ArmorType type : ArmorType.values()) {
            ItemStack armorPiece = inventory.getItem(type.ordinal());
            if (armorPiece == null) {
                continue;
            }
            boolean isIllegal = ItemUtil.isIllegalItem(armorPiece);
            boolean isRemoved = ItemUtil.isRemovedOnJoin(armorPiece);

            if (isIllegal || isRemoved) {
                inventory.setItem(type.ordinal(), null);
                if (isIllegal) {
                    illegalItems++;
                }
            }
        }

        if (illegalItems > 0) {
            player.sendMessage(CC.translate("&c今从君之囊中寻得 &e" + illegalItems + "&c 异物，已悉数除却，恕罪"));
        }
        if (playerProfile.getPlayerOption().isOutfit()) {
            if (ItemUtil.isDefaultItem(inventory.getHelmet())) {
                inventory.setHelmet(new ItemStack(Material.AIR));
            }
            if (ItemUtil.isDefaultItem(inventory.getChestplate())) {
                inventory.setChestplate(new ItemStack(Material.AIR));
            }
            if (ItemUtil.isDefaultItem(inventory.getLeggings())) {
                inventory.setLeggings(new ItemStack(Material.AIR));
            }
            if (ItemUtil.isDefaultItem(inventory.getBoots())) {
                inventory.setBoots(new ItemStack(Material.AIR));
            }

            if (!swordFound) {
                inventory
                        .addItem(DEFAULT_SWORD);
            }
            if (!bowFound) {
                inventory
                        .addItem(DEFAULT_BOW);
            }
        }
        /*
        player.getInventory()
                .remove(Material.ARROW);
        InventoryUtil.addInvReverse(player.getInventory(),
                (new ItemBuilder(Material.ARROW).internalName("default_arrow").defaultItem().canDrop(false).canSaveToEnderChest(false).amount(32).build()));

         */

        int maxArrow = 32 + Math.max(0, PlayerUtil.getPlayerUnlockedPerkLevel(player, "arrow_armory_perk") * 8);
        if (arrowAmount > 0 && arrowAmount <= maxArrow) {
            if (arrowAmount != maxArrow) {
                ItemBuilder arrowBuilder = new ItemBuilder(Material.ARROW).internalName("default_arrow").defaultItem().canDrop(false).canSaveToEnderChest(false);

                inventory.addItem(
                        arrowBuilder.amount(maxArrow - arrowAmount).build()
                );
            }
        } else {
            if (arrowAmount > maxArrow) {
                inventory.remove(Material.ARROW);
            }
            ItemBuilder arrowBuilder = new ItemBuilder(Material.ARROW).internalName("default_arrow").defaultItem().canDrop(false).canSaveToEnderChest(false);
            InventoryUtil.addInvReverse(inventory,
                    (arrowBuilder.amount(maxArrow).build()));
        }

        if (playerProfile.getPlayerOption().isOutfit()) {
            int ironArmorSlot = random.nextInt(3);

            if (inventory.getChestplate() == null || ItemUtil.isDefaultItem(inventory.getChestplate())) {
                if (ironArmorSlot == 0) {
                    inventory.setChestplate(new ItemBuilder(Material.IRON_CHESTPLATE).defaultItem().internalName("default_armor").canDrop(false).canSaveToEnderChest(true).buildWithUnbreakable());
                } else {
                    inventory.setChestplate(new ItemBuilder(Material.CHAINMAIL_CHESTPLATE).defaultItem().internalName("default_armor").canDrop(false).canSaveToEnderChest(true).buildWithUnbreakable());
                }
            }
            if (inventory.getLeggings() == null || ItemUtil.isDefaultItem(inventory.getLeggings())) {
                if (ironArmorSlot == 1) {
                    inventory.setLeggings(new ItemBuilder(Material.IRON_LEGGINGS).defaultItem().internalName("default_armor").canDrop(false).canSaveToEnderChest(true).buildWithUnbreakable());
                } else {
                    inventory.setLeggings(new ItemBuilder(Material.CHAINMAIL_LEGGINGS).defaultItem().internalName("default_armor").canDrop(false).canSaveToEnderChest(true).buildWithUnbreakable());
                }
            }
            if (inventory.getBoots() == null || ItemUtil.isDefaultItem(inventory.getBoots())) {
                if (ironArmorSlot == 2) {
                    inventory.setBoots(new ItemBuilder(Material.IRON_BOOTS).defaultItem().internalName("default_armor").canDrop(false).canSaveToEnderChest(true).buildWithUnbreakable());
                } else {
                    inventory.setBoots(new ItemBuilder(Material.CHAINMAIL_BOOTS).defaultItem().internalName("default_armor").canDrop(false).canSaveToEnderChest(true).buildWithUnbreakable());
                }
            }
        }
    }

    enum ArmorType {
        HELMET, CHESTPLATE, LEGGINGS, BOOTS
    }

    public static int getArmorSlot(Material material) {
        String name = material.name();
        if (name.contains("HELMET")) {
            return 3;
        }
        if (name.contains("CHESTPLATE")) {
            return 2;
        }
        if (name.contains("LEGGINGS")) {
            return 1;
        }
        if (name.contains("BOOTS")) {
            return 0;
        }
        return -1;
    }

    private static int firstPartial(PlayerInventory playerInv, ItemStack item) {
        ItemStack[] inventory = playerInv.getContents();
        ItemStack filteredItem = item.clone();
        for (int i = 35; i > -1; i--) {
            ItemStack cItem = inventory[i];
            if (cItem != null && cItem.getAmount() < cItem.getMaxStackSize() && cItem.isSimilar(filteredItem)) {
                return i;
            }
        }
        return -1;
    }

    private static int firstEmpty(PlayerInventory playerInv) {
        ItemStack[] inventory = playerInv.getContents();
        for (int i = 35; i > -1; i--) {
            if (inventory[i] == null) {
                return i;
            }
        }
        return -1;
    }

    public static void addInvReverse(PlayerInventory inventory, ItemStack item) {
        while (true) {
            // Do we already have a stack of it?
            int firstPartial = firstPartial(inventory, item);

            // Drat! no partial stack
            if (firstPartial == -1) {
                // Find a free spot!
                int firstFree = firstEmpty(inventory);

                if (firstFree == -1) {
                    break;
                } else {
                    // More than a single stack!
                    if (item.getAmount() > inventory.getMaxStackSize()) {
                        ItemStack stack = item.clone();
                        stack.setAmount(inventory.getMaxStackSize());
                        inventory.setItem(firstFree, stack);
                        item.setAmount(item.getAmount() - inventory.getMaxStackSize());
                    } else {
                        // Just store it
                        inventory.setItem(firstFree, item);
                        break;
                    }
                }
            } else {
                // So, apparently it might only partially fit, well lets do just that
                ItemStack partialItem = inventory.getItem(firstPartial);

                int amount = item.getAmount();
                int partialAmount = partialItem.getAmount();
                int maxAmount = partialItem.getMaxStackSize();

                // Check if it fully fits
                if (amount + partialAmount <= maxAmount) {
                    partialItem.setAmount(amount + partialAmount);
                    // To make sure the packet is sent to the client
                    inventory.setItem(firstPartial, partialItem);
                    break;
                }

                // It fits partially
                partialItem.setAmount(maxAmount);
                // To make sure the packet is sent to the client
                inventory.setItem(firstPartial, partialItem);
                item.setAmount(amount + partialAmount - maxAmount);
            }
        }
    }

    public static boolean isInvFull(PlayerInventory inventory) {
        for (ItemStack itemStack : inventory) {
            if (itemStack == null || itemStack.getType() == Material.AIR) {
                return false;
            }
        }
        return true;
    }
}

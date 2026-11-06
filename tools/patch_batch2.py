import os

def read(p):
    return open(p, encoding='utf-8').read()

def write(p, t):
    open(p, 'w', encoding='utf-8').write(t)

# 1) AbstractEnchantment.canApply -> 材质判定
p = 'base/src/main/java/net/mizukilab/pit/enchantment/AbstractEnchantment.java'
t = read(p)
t = t.replace("import net.minecraft.server.v1_8_R3.*;\n", "")
t = t.replace("""        net.minecraft.server.v1_8_R3.ItemStack nmsItem = PublicUtil.toNMStackQuick(item);
        if (nmsItem.getItem() instanceof ItemSword) {""",
              """        if (cn.charlotte.pit.compat.item.ItemTypes.isSword(item)) {""")
t = t.replace("""        if (nmsItem.getItem() instanceof ItemBow) {""",
              """        if (cn.charlotte.pit.compat.item.ItemTypes.isBow(item)) {""")
t = t.replace("""        if (nmsItem.getItem() instanceof ItemArmor) {""",
              """        if (cn.charlotte.pit.compat.item.ItemTypes.isArmor(item)) {""")
write(p, t)

# 2) InventoryUtil：改用 Paper 的物品字节序列化
p = 'base/src/main/java/net/mizukilab/pit/util/inventory/InventoryUtil.java'
t = read(p)
t = t.replace("import net.minecraft.server.v1_8_R3.*;\n", "")
t = t.replace("import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;\n", "")
t = t.replace("""        ByteArrayOutputStream outputStream = null;
        try {
            net.minecraft.server.v1_8_R3.ItemStack nmsStack = PublicUtil.toNMStackQuick(itemStack);
            NBTTagCompound nbtTagCompound = new NBTTagCompound();
            nmsStack.save(nbtTagCompound);
            outputStream = new ByteArrayOutputStream();
            NBTCompressedStreamTools.a(nbtTagCompound, outputStream);
        } catch (SecurityException | IllegalArgumentException | IOException e) {
            e.printStackTrace();
            return null;
        }

        return java.util.Base64.getEncoder().encodeToString(outputStream.toByteArray());""",
              """        try {
            // Paper 1.20.5+ 提供稳定的物品字节序列化，替代手工 NBT 压缩流
            return java.util.Base64.getEncoder().encodeToString(itemStack.serializeAsBytes());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }""")
t = t.replace("""        ByteArrayInputStream inputStream = new ByteArrayInputStream(java.util.Base64.getDecoder().decode(itemStackString));

        NBTTagCompound nbtTagCompound = null;
        ItemStack itemStack = null;
        try {
            nbtTagCompound = NBTCompressedStreamTools.a(inputStream);
            net.minecraft.server.v1_8_R3.ItemStack nmsStack = net.minecraft.server.v1_8_R3.ItemStack.createStack(nbtTagCompound);
            itemStack = CraftItemStack.asBukkitCopy(nmsStack);
        } catch (IllegalArgumentException | SecurityException | IOException e) {
            e.printStackTrace();
            return null;
        }

        return itemStack;""",
              """        try {
            return ItemStack.deserializeBytes(java.util.Base64.getDecoder().decode(itemStackString));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }""")
t = t.replace("""                net.minecraft.server.v1_8_R3.ItemStack nmStackQuick = PublicUtil.toNMStackQuick(item);
                if (nmStackQuick.getItem() instanceof ItemArmor) {
                    inventory.remove(item);
                }
                if (nmStackQuick.getItem() instanceof ItemSword && swordFound) {
                    inventory.remove(item);
                    swordFound = false;
                }
                if (nmStackQuick.getItem() instanceof ItemBow && bowFound) {""",
              """                if (cn.charlotte.pit.compat.item.ItemTypes.isArmor(item)) {
                    inventory.remove(item);
                }
                if (cn.charlotte.pit.compat.item.ItemTypes.isSword(item) && swordFound) {
                    inventory.remove(item);
                    swordFound = false;
                }
                if (cn.charlotte.pit.compat.item.ItemTypes.isBow(item) && bowFound) {""")
write(p, t)

# 3) CustomEntityNPCFactory：NoAI/Silent 走实体 API
p = 'base/src/main/java/net/mizukilab/pit/npc/CustomEntityNPCFactory.java'
t = read(p)
t = t.replace("import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;\n", "")
t = t.replace("""                    net.minecraft.server.v1_8_R3.Entity nmsEntity = ((CraftLivingEntity) livingEntity).getHandle();
                    net.minecraft.server.v1_8_R3.NBTTagCompound nbt = new net.minecraft.server.v1_8_R3.NBTTagCompound();
                    nmsEntity.c(nbt);
                    nbt.setInt("NoAI", 1);
                    nbt.setInt("Silent", 1);
                    nbt.setByte("BatFlags", (byte) 1);
                    nmsEntity.f(nbt);""",
              """                    // 1.21：NoAI/Silent 直接走实体 API
                    livingEntity.setAI(false);
                    livingEntity.setSilent(true);
                    bat.setAwake(true);""")
t = t.replace("""                    net.minecraft.server.v1_8_R3.Entity nmsEntity = ((CraftLivingEntity) livingEntity).getHandle();
                    net.minecraft.server.v1_8_R3.NBTTagCompound nbt = new net.minecraft.server.v1_8_R3.NBTTagCompound();
                    nmsEntity.c(nbt);
                    nbt.setInt("NoAI", 1);
                    nbt.setInt("Silent", 1);
                    nmsEntity.f(nbt);""",
              """                    livingEntity.setAI(false);
                    livingEntity.setSilent(true);""")
write(p, t)

# 4) NpcFactory：NPC_v1_8_R3 -> NPC_Entity
p = 'base/src/main/java/net/mizukilab/pit/npc/NpcFactory.java'
t = read(p)
t = t.replace("import net.jitse.npclib.nms.v1_8_R3.NPC_v1_8_R3;", "import net.jitse.npclib.internal.NPC_Entity;")
t = t.replace("NPC_v1_8_R3", "NPC_Entity")
write(p, t)

print('batch2 ok')

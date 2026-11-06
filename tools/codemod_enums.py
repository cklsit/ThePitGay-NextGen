"""1.8 -> 1.21 枚举常量重命名（Material / Sound / PotionEffectType / Enchantment / EntityType）。

仅做名称替换，语义相关的地方（如带 data 值的染色材料）由后续手工处理。
"""
import os
import sys

MATERIAL = {
    "ACACIA_DOOR_ITEM": "ACACIA_DOOR",
    "BANNER": "WHITE_BANNER",
    "BED": "RED_BED",
    "BED_BLOCK": "RED_BED",
    "BIRCH_DOOR_ITEM": "BIRCH_DOOR",
    "BOOK_AND_QUILL": "WRITABLE_BOOK",
    "BURNING_FURNACE": "FURNACE",
    "CAKE_BLOCK": "CAKE",
    "CARPET": "WHITE_CARPET",
    "CARROT_ITEM": "CARROT",
    "COMMAND_MINECART": "COMMAND_BLOCK_MINECART",
    "CROPS": "WHEAT",
    "DARK_OAK_DOOR_ITEM": "DARK_OAK_DOOR",
    "DIAMOND_SPADE": "DIAMOND_SHOVEL",
    "EMPTY_MAP": "MAP",
    "ENCHANTMENT_TABLE": "ENCHANTING_TABLE",
    "ENDER_PORTAL_FRAME": "END_PORTAL_FRAME",
    "ENDER_STONE": "END_STONE",
    "EXP_BOTTLE": "EXPERIENCE_BOTTLE",
    "EYE_OF_ENDER": "ENDER_EYE",
    "GOLD_BARDING": "GOLDEN_HORSE_ARMOR",
    "GOLD_SWORD": "GOLDEN_SWORD",
    "GOLD_PICKAXE": "GOLDEN_PICKAXE",
    "GOLD_AXE": "GOLDEN_AXE",
    "GOLD_BOOTS": "GOLDEN_BOOTS",
    "GOLD_CHESTPLATE": "GOLDEN_CHESTPLATE",
    "GOLD_HELMET": "GOLDEN_HELMET",
    "GOLD_LEGGINGS": "GOLDEN_LEGGINGS",
    "GRASS": "SHORT_GRASS",
    "INK_SACK": "INK_SAC",
    "JUNGLE_DOOR_ITEM": "JUNGLE_DOOR",
    "LEASH": "LEAD",
    "LOG": "OAK_LOG",
    "MAP": "FILLED_MAP",
    "MELON_BLOCK": "MELON",
    "MUSHROOM_SOUP": "MUSHROOM_STEW",
    "NETHER_BRICK": "NETHER_BRICKS",
    "PISTON_STICKY_BASE": "STICKY_PISTON",
    "RAW_BEEF": "BEEF",
    "RAW_FISH": "COD",
    "SIGN": "OAK_SIGN",
    "SIGN_POST": "OAK_SIGN",
    "SKULL": "SKELETON_SKULL",
    "SKULL_ITEM": "PLAYER_HEAD",
    "SMOOTH_BRICK": "STONE_BRICKS",
    "SNOW_BALL": "SNOWBALL",
    "STAINED_CLAY": "TERRACOTTA",
    "STAINED_GLASS": "WHITE_STAINED_GLASS",
    "STAINED_GLASS_PANE": "WHITE_STAINED_GLASS_PANE",
    "STANDING_BANNER": "WHITE_BANNER",
    "STATIONARY_WATER": "WATER",
    "STORAGE_MINECART": "CHEST_MINECART",
    "WATCH": "CLOCK",
    "WATER_LILY": "LILY_PAD",
    "WOOD": "OAK_PLANKS",
    "WOOD_BUTTON": "OAK_BUTTON",
    "WOOD_SWORD": "WOODEN_SWORD",
    "WOOL": "WHITE_WOOL",
    "WORKBENCH": "CRAFTING_TABLE",
}

SOUND = {
    "AMBIENCE_CAVE": "AMBIENT_CAVE",
    "AMBIENCE_THUNDER": "ENTITY_LIGHTNING_BOLT_THUNDER",
    "ANVIL_BREAK": "BLOCK_ANVIL_BREAK",
    "ANVIL_LAND": "BLOCK_ANVIL_LAND",
    "ANVIL_USE": "BLOCK_ANVIL_USE",
    "BLAZE_BREATH": "ENTITY_BLAZE_SHOOT",
    "BLAZE_HIT": "ENTITY_BLAZE_HURT",
    "BURP": "ENTITY_PLAYER_BURP",
    "CHEST_CLOSE": "BLOCK_CHEST_CLOSE",
    "CHEST_OPEN": "BLOCK_CHEST_OPEN",
    "CHICKEN_EGG_POP": "ENTITY_CHICKEN_EGG",
    "CLICK": "UI_BUTTON_CLICK",
    "DIG_GRASS": "BLOCK_GRASS_BREAK",
    "DIG_GRAVEL": "BLOCK_GRAVEL_BREAK",
    "DRINK": "ENTITY_GENERIC_DRINK",
    "EAT": "ENTITY_GENERIC_EAT",
    "ENDERDRAGON_DEATH": "ENTITY_ENDER_DRAGON_DEATH",
    "ENDERDRAGON_GROWL": "ENTITY_ENDER_DRAGON_GROWL",
    "ENDERDRAGON_HIT": "ENTITY_ENDER_DRAGON_HURT",
    "ENDERDRAGON_WINGS": "ENTITY_ENDER_DRAGON_FLAP",
    "ENDERMAN_IDLE": "ENTITY_ENDERMAN_AMBIENT",
    "ENDERMAN_SCREAM": "ENTITY_ENDERMAN_SCREAM",
    "ENDERMAN_TELEPORT": "ENTITY_ENDERMAN_TELEPORT",
    "EXPLODE": "ENTITY_GENERIC_EXPLODE",
    "FALL_BIG": "ENTITY_GENERIC_BIG_FALL",
    "FIRE": "BLOCK_FIRE_AMBIENT",
    "FIREWORK_BLAST": "ENTITY_FIREWORK_ROCKET_BLAST",
    "FIREWORK_LARGE_BLAST": "ENTITY_FIREWORK_ROCKET_LARGE_BLAST",
    "FIREWORK_LAUNCH": "ENTITY_FIREWORK_ROCKET_LAUNCH",
    "FIREWORK_TWINKLE": "ENTITY_FIREWORK_ROCKET_TWINKLE",
    "FIRE_IGNITE": "ITEM_FLINTANDSTEEL_USE",
    "FIZZ": "BLOCK_FIRE_EXTINGUISH",
    "GHAST_FIREBALL": "ENTITY_GHAST_SHOOT",
    "GHAST_MOAN": "ENTITY_GHAST_AMBIENT",
    "GHAST_SCREAM": "ENTITY_GHAST_SCREAM",
    "GLASS": "BLOCK_GLASS_BREAK",
    "HORSE_ARMOR": "ITEM_ARMOR_EQUIP_GENERIC",
    "HURT_FLESH": "ENTITY_PLAYER_HURT",
    "ITEM_PICKUP": "ENTITY_ITEM_PICKUP",
    "LAVA": "BLOCK_LAVA_AMBIENT",
    "LAVA_POP": "BLOCK_LAVA_POP",
    "LEVEL_UP": "ENTITY_PLAYER_LEVELUP",
    "NOTE_BASS": "BLOCK_NOTE_BLOCK_BASS",
    "NOTE_PIANO": "BLOCK_NOTE_BLOCK_HARP",
    "NOTE_PLING": "BLOCK_NOTE_BLOCK_PLING",
    "NOTE_STICKS": "BLOCK_NOTE_BLOCK_BASEDRUM",
    "ORB_PICKUP": "ENTITY_EXPERIENCE_ORB_PICKUP",
    "PORTAL": "BLOCK_PORTAL_AMBIENT",
    "PORTAL_TRIGGER": "BLOCK_PORTAL_TRIGGER",
    "SILVERFISH_HIT": "ENTITY_SILVERFISH_HURT",
    "STEP_SNOW": "BLOCK_SNOW_STEP",
    "VILLAGER_DEATH": "ENTITY_VILLAGER_DEATH",
    "VILLAGER_HAGGLE": "ENTITY_VILLAGER_TRADE",
    "VILLAGER_NO": "ENTITY_VILLAGER_NO",
    "VILLAGER_YES": "ENTITY_VILLAGER_YES",
    "WITHER_DEATH": "ENTITY_WITHER_DEATH",
    "WITHER_IDLE": "ENTITY_WITHER_AMBIENT",
    "WITHER_SPAWN": "ENTITY_WITHER_SPAWN",
    "ZOMBIE_INFECT": "ENTITY_ZOMBIE_INFECT",
    "ZOMBIE_WOODBREAK": "ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR",
}

POTION = {
    "CONFUSION": "NAUSEA",
    "DAMAGE_RESISTANCE": "RESISTANCE",
    "INCREASE_DAMAGE": "STRENGTH",
    "JUMP": "JUMP_BOOST",
    "SLOW": "SLOWNESS",
    "SLOW_DIGGING": "MINING_FATIGUE",
}

ENCHANT = {
    "ARROW_DAMAGE": "POWER",
    "ARROW_FIRE": "FLAME",
    "ARROW_INFINITE": "INFINITY",
    "ARROW_KNOCKBACK": "PUNCH",
    "DIG_SPEED": "EFFICIENCY",
    "DURABILITY": "UNBREAKING",
}

ENTITY = {
    "PRIMED_TNT": "TNT",
}


def apply(text, prefix, table):
    count = 0
    for old, new in table.items():
        needle = prefix + "." + old
        if needle in text:
            # 避免前缀相同导致的二次替换
            text = text.replace(needle, prefix + ".__TMP__" + new)
            count += 1
    text = text.replace(".__TMP__", ".")
    return text, count


def main(paths):
    total = 0
    for path in paths:
        with open(path, encoding="utf-8") as fh:
            text = fh.read()
        new_text, n = apply(text, "Material", MATERIAL)
        new_text, n2 = apply(new_text, "Sound", SOUND)
        new_text, n3 = apply(new_text, "PotionEffectType", POTION)
        new_text, n4 = apply(new_text, "Enchantment", ENCHANT)
        new_text, n5 = apply(new_text, "EntityType", ENTITY)
        if new_text != text:
            with open(path, "w", encoding="utf-8", newline="") as fh:
                fh.write(new_text)
            total += n + n2 + n3 + n4 + n5
    print("changed constants:", total)


def collect(roots):
    found = []
    for root in roots:
        for base, _dirs, files in os.walk(root):
            for name in files:
                if name.endswith((".java", ".kt")):
                    found.append(os.path.join(base, name))
    return found


if __name__ == "__main__":
    main(collect(sys.argv[1:] or ["base", "core"]))

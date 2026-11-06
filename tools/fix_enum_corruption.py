import os

REPAIRS = [
    ("SLOWNESS_DIGGING", "MINING_FATIGUE"),
    ("OAK_SIGN_POST", "OAK_SIGN"),
    ("OAK_PLANKS_BUTTON", "OAK_BUTTON"),
    ("OAK_PLANKS_SWORD", "WOODEN_SWORD"),
    ("SKELETON_SKULL_ITEM", "PLAYER_HEAD"),
    ("BLOCK_FIRE_AMBIENTWORK_LARGE_BLAST", "ENTITY_FIREWORK_ROCKET_LARGE_BLAST"),
    ("BLOCK_FIRE_AMBIENTWORK_TWINKLE", "ENTITY_FIREWORK_ROCKET_TWINKLE"),
    ("BLOCK_FIRE_AMBIENTWORK_LAUNCH", "ENTITY_FIREWORK_ROCKET_LAUNCH"),
    ("BLOCK_FIRE_AMBIENTWORK_BLAST", "ENTITY_FIREWORK_ROCKET_BLAST"),
    ("BLOCK_PORTAL_AMBIENT_TRIGGER", "BLOCK_PORTAL_TRIGGER"),
]

changed = 0
for root in ("base", "core"):
    for bas, _dirs, names in os.walk(root):
        for name in names:
            if not name.endswith((".java", ".kt")):
                continue
            path = os.path.join(bas, name)
            text = open(path, encoding="utf-8").read()
            original = text
            for old, new in REPAIRS:
                text = text.replace(old, new)
            if text != original:
                open(path, "w", encoding="utf-8", newline="").write(text)
                changed += 1
print("repaired files:", changed)

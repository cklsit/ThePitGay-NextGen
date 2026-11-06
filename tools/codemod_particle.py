"""EnumParticle -> org.bukkit.Particle 常量替换。"""
import os
import sys

MAP = {
    "REDSTONE": "DUST",
    "ENCHANTMENT_TABLE": "ENCHANT",
    "SPELL_WITCH": "WITCH",
    "NOTE": "NOTE",
    "CLOUD": "CLOUD",
    "VILLAGER_HAPPY": "HAPPY_VILLAGER",
    "FLAME": "FLAME",
    "FIREWORKS_SPARK": "FIREWORK",
    "EXPLOSION_LARGE": "EXPLOSION",
    "DRIP_WATER": "DRIPPING_WATER",
}


def main():
    roots = sys.argv[1:] or ["base", "core"]
    changed = 0
    files = 0
    for root in roots:
        for bas, _dirs, names in os.walk(root):
            for name in names:
                if not name.endswith((".java", ".kt")):
                    continue
                path = os.path.join(bas, name)
                text = open(path, encoding="utf-8").read()
                if "EnumParticle" not in text:
                    continue
                original = text
                for old, new in MAP.items():
                    text = text.replace("EnumParticle." + old, "Particle." + new)
                text = text.replace("import net.minecraft.server.v1_8_R3.EnumParticle;", "import org.bukkit.Particle;")
                text = text.replace("import net.minecraft.server.v1_8_R3.EnumParticle", "import org.bukkit.Particle")
                if text != original:
                    open(path, "w", encoding="utf-8", newline="").write(text)
                    files += 1
                    changed += 1
    print(f"files={files}")


if __name__ == "__main__":
    main()

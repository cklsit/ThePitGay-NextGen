import os
import re

ROOT = 'core/src/main/java'

CAST_RE = re.compile(r'\(\(CraftPlayer\)\s*([A-Za-z0-9_.()]+)\)\.getHandle\(\)')

changed = 0
for bas, _dirs, names in os.walk(ROOT):
    for name in names:
        if not name.endswith('.java'):
            continue
        p = os.path.join(bas, name)
        t = open(p, encoding='utf-8').read()
        original = t

        # CraftPlayer -> 直接使用 Bukkit Player
        t = CAST_RE.sub(lambda m: '((org.bukkit.entity.Player) ' + m.group(1) + ')', t)
        # 吸收伤害字段名变化
        t = t.replace('.getAbsorptionHearts()', '.getAbsorptionAmount()')
        t = t.replace('.setAbsorptionHearts(', '.setAbsorptionAmount(')
        # 无敌帧
        t = re.sub(r'\.invulnerableTicks\s*=\s*(\d+)', r'.setNoDamageTicks(\1)', t)
        # 直接置血
        t = t.replace('.getHandle().setHealth(0.0f)', '.setHealth(0.0)')

        if t != original:
            open(p, 'w', encoding='utf-8').write(t)
            changed += 1

print('changed', changed)

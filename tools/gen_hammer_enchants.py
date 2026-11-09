import io
import os
import re

DOC = 'docs/MYTHIC_HAMMER_COMPLETE.md'
OUT = 'core/src/main/java/net/mizukilab/pit/enchantment/type/hammer/entries'
TAG_OPEN = '<'
TAG_RE = re.compile(r'<[^>]+>')


def strip_html(s):
    return TAG_RE.sub('', s)


def rarity_of(tag):
    t = strip_html(tag)
    if '季节性' in t:
        return 'EnchantmentRarity.SEASONAL'
    if '稀有' in t:
        return 'EnchantmentRarity.RARE'
    return 'EnchantmentRarity.NORMAL'


def numbers(cell):
    c = strip_html(cell).replace('❤', '')
    return [float(x) for x in re.findall(r'-?\d+(?:\.\d+)?', c)]


def cooldown_seconds(effect):
    m = re.search(r'冷却\s*(\d+(?:\.\d+)?)\s*秒', effect)
    return float(m.group(1)) if m else 0.0


def live_cost(hp_cell):
    c = strip_html(hp_cell)
    m = re.search(r'消耗\s*(\d+)\s*点\s*live', c)
    if m:
        return int(m.group(1))
    m = re.search(r'消耗\s*(\d+)\s*点', c)
    return int(m.group(1)) if m else 0


def classify(effect):
    """按效果文本归类，只自动结算可数据化的三类。"""
    t = strip_html(effect)
    if '增伤' in t:
        return 'DAMAGE_PCT'
    if '减伤' in t:
        return 'DAMAGE_REDUCTION'
    if '受到的伤害' in t and ('降低' in t or '减少' in t):
        return 'DAMAGE_REDUCTION'
    if '抵消' in t and '伤害' in t:
        return 'DAMAGE_REDUCTION'
    if ('回复' in t) and ('生命值' in t or '❤' in t) and 'live' not in t:
        return 'HEAL'
    return 'SPECIAL'


def escape_kotlin(s):
    return s.replace('\\', '\\\\').replace('"', '\\"')


doc = io.open(DOC, encoding='utf-8').read()
data = {}
for line in doc.split('\n'):
    if not re.match(r'^\|\s*\d+\s*\|', line):
        continue
    parts = [x.strip() for x in line.strip('|').split('|')]
    if len(parts) != 7:
        continue
    if not parts[0].isdigit():
        continue
    n = int(parts[0])
    if 1 <= n <= 100 and n not in data:
        data[n] = parts

print('parsed entries:', len(data))

os.makedirs(OUT, exist_ok=True)
for f in os.listdir(OUT):
    os.remove(os.path.join(OUT, f))

made = 0
for n in range(1, 101):
    p = data.get(n)
    if not p:
        print('MISSING', n)
        continue
    name, tag, tier_cell, effect, hp_cell = p[1], p[2], p[3], p[4], p[5]
    rarity = rarity_of(tag)
    nums = numbers(tier_cell)
    vals = nums[:3] if nums else [0.0]
    if len(vals) == 1:
        vals = [vals[0]] * 3
    elif len(vals) == 2:
        vals = [vals[0], vals[1], vals[1]]
    def fmt(v):
        s = ('%.4g' % v)
        return s if ('.' in s or 'e' in s) else (s + '.0')
    vals_str = ', '.join(fmt(v) for v in vals)
    cd = cooldown_seconds(effect)
    lc = live_cost(hp_cell)
    eff_kind = classify(effect)
    lore = escape_kotlin(strip_html(effect))
    cls = 'HammerEnchant%03d' % n
    code = (
        'package net.mizukilab.pit.enchantment.type.hammer.entries\n'
        '\n'
        'import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity\n'
        'import net.mizukilab.pit.enchantment.type.hammer.HammerEffect\n'
        'import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase\n'
        '\n'
        '/** %d. %s */\n'
        'class %s : HammerEnchantBase(\n'
        '    "%s",\n'
        '    "hammer_%03d",\n'
        '    %s,\n'
        '    listOf(%s),\n'
        '    "&7%s",\n'
        '    %s,\n'
        '    %d,\n'
        '    HammerEffect.%s\n'
        ')\n'
    ) % (n, name, cls, name, n, rarity, vals_str, lore, fmt(cd), lc, eff_kind)
    io.open(os.path.join(OUT, cls + '.kt'), 'w', encoding='utf-8').write(code)
    made += 1

print('generated:', made)

package cn.irina.thepitaddon.enchantment.type.limit;

import net.mizukilab.pit.enchantment.AbstractEnchantment;
import net.mizukilab.pit.enchantment.IActionDisplayEnchant;
import net.mizukilab.pit.enchantment.param.item.WeaponOnly;
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity;
import net.mizukilab.pit.parm.listener.IAttackEntity;
import net.mizukilab.pit.parm.listener.IPlayerKilledEntity;
import net.mizukilab.pit.util.PlayerUtil;
import net.mizukilab.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;


import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@WeaponOnly
public class Verdict extends AbstractEnchantment implements  IAttackEntity, IActionDisplayEnchant, IPlayerKilledEntity {
    private static final HashMap<UUID, Cooldown> cooldown = new HashMap<>();

    @Override
    public String getEnchantName() {
        return "裁决";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "verdict";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.OP;
    }

    @Nullable
    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "若命中时使目标生命值低于 &c" + (2 + (enchantLevel * 0.5)) + "❤ &7时, 则该次命中直接致死 " + (enchantLevel >= 4 ? "" : "(" + (10 - (enchantLevel * 2)) + "s冷却)") + "/s" +
                "&7(每击杀一位目标将减少此附魔1s冷却)";
    }

    @Override
    public void handleAttackEntity(int enchantLevel, Player attacker, Entity entity, double damage, AtomicDouble atomicDouble, AtomicDouble atomicDouble1, AtomicBoolean atomicBoolean) {
        if (!(entity instanceof Player)) return;
        Player target = (Player) entity;

        if (!(target.getHealth() <= (2 + (enchantLevel * 0.5)) * 2) || !cooldown.getOrDefault(attacker.getUniqueId(), new Cooldown(0L)).hasExpired()) return;
        cooldown.put(attacker.getUniqueId(), new Cooldown(10 - (enchantLevel * 2L), TimeUnit.SECONDS));
        target.playEffect(target.getLocation(), Effect.STEP_SOUND, Material.REDSTONE_BLOCK);
        target.playSound(target.getLocation(), Sound.STEP_STONE, 1, 1);
        target.damage(target.getMaxHealth() * 20);
    }

    @Override
    public void handlePlayerKilled(int enchantLevel, Player killer, Entity entity, AtomicDouble atomicDouble, AtomicDouble atomicDouble1) {
        if (cooldown.get(killer.getUniqueId()).hasExpired()) return;
        cooldown.put(killer.getUniqueId(), new Cooldown(Math.max(0L, cooldown.get(killer.getUniqueId()).getRemaining() - 1000L)));
    }

    @Override
    public String getText(int i, Player attacker) {
        if (!PlayerUtil.isVenom(attacker)) return getCooldownActionText(cooldown.getOrDefault(attacker.getUniqueId(), new Cooldown(0L)));

        return "&c&l✘";
    }
}

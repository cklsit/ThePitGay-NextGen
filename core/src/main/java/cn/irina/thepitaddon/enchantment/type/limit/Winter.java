package cn.irina.thepitaddon.enchantment.type.limit;

import cn.irina.thepitaddon.Main;
import com.google.common.util.concurrent.AtomicDouble;
import net.mizukilab.pit.enchantment.AbstractEnchantment;
import net.mizukilab.pit.enchantment.IActionDisplayEnchant;
import net.mizukilab.pit.enchantment.param.item.WeaponOnly;
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity;
import net.mizukilab.pit.parm.listener.IAttackEntity;
import net.mizukilab.pit.util.cooldown.Cooldown;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@WeaponOnly
public class Winter extends AbstractEnchantment implements IAttackEntity, Listener, IActionDisplayEnchant {
    private static final String WINTER_FRAIL = "WinterFrail";
    private static final HashMap<UUID, Cooldown> cooldown = new HashMap<>();

    @Override
    public String getEnchantName() {
        return "凛冬";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "winter";
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
        return "&7对带有 &c缓慢 &7效果的玩家造成的伤害 &c+" + (enchantLevel * 4 + 4) + "%/s" +
                "&7攻击目标时将对其施加: /s" +
                "  &f▶ &b霜寒 &f(00:0" + (enchantLevel + 1) + ") /s" +
                "  &f▶ &8脆弱 &f(00:0" + ((enchantLevel * 2) + 2) + ") /s" +
                "&7效果 &b霜寒&7: 移速大幅降低, 对他人造成的伤害 &9-20% /s" +
                "&7效果 &8脆弱&7: 受到的暴击伤害 &c+20% /s" +
                (enchantLevel >= 3 ? "&7同时, 攻击目标时将清除自身的 &c缓慢 &7效果 /s" + "&8每秒只可触发一次" : "&8每秒只可触发一次/s") +
                "/s " +
                "/s  \"&7&o冰冷苦海, 那骨骸被雪覆盖" +
                "/s      &7&o恐惧仍在, 深深印在你脑海\"";
    }

    @Override
    public void handleAttackEntity(
            int enchantLevel,
            Player player,
            Entity target,
            double v,
            AtomicDouble atomicDouble,
            AtomicDouble boostDamage,
            AtomicBoolean atomicBoolean
    ) {
        if (!(target instanceof Player)) return;

        if (!cooldown.getOrDefault(player.getUniqueId(), new Cooldown(0L)).hasExpired()) return;
        cooldown.put(player.getUniqueId(), new Cooldown(1L, TimeUnit.SECONDS));

        Player targetPlayer = (Player) target;
        if (targetPlayer.hasPotionEffect(PotionEffectType.SLOW)) {
            boostDamage.getAndAdd(enchantLevel * 0.04 + 0.04);
        }

        for (PotionEffect potionEffect : targetPlayer.getActivePotionEffects()) {
            if (!potionEffect.getType().equals(PotionEffectType.SLOW)) continue;
            int level = potionEffect.getAmplifier();
            if (level >= 1) return;
            targetPlayer.removePotionEffect(potionEffect.getType());
        }

        targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (enchantLevel + 1) * 20, 1, false, true));

        if (targetPlayer.hasMetadata(WINTER_FRAIL)) targetPlayer.removeMetadata(WINTER_FRAIL, Main.getInstance());

        targetPlayer.setMetadata(WINTER_FRAIL, new FixedMetadataValue(Main.getInstance(), System.currentTimeMillis() + ((enchantLevel * 2L) + 2) * 1000L));

        if (enchantLevel >= 3 && player.hasPotionEffect(PotionEffectType.SLOW))
            player.removePotionEffect(PotionEffectType.SLOW);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player attacker = (Player) event.getDamager();

        if (!attacker.hasMetadata(WINTER_FRAIL) || (attacker.getMetadata(WINTER_FRAIL).get(0)).asLong() <= System.currentTimeMillis())
            return;
        event.setDamage(event.getDamage() * 0.8);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onProtect(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player target = (Player) event.getEntity();

        if (!target.hasMetadata(WINTER_FRAIL) || (target.getMetadata(WINTER_FRAIL).get(0)).asLong() <= System.currentTimeMillis())
            return;
        event.setDamage(event.getDamage() * 1.2);
    }


    @Override
    public String getText(int i, Player player) {
        return getCooldownActionText(cooldown.getOrDefault(player.getUniqueId(), new Cooldown(0L)));
    }
}

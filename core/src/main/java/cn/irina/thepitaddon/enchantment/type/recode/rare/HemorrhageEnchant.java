package cn.irina.thepitaddon.enchantment.type.recode.rare;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.buff.impl.HemorrhageDeBuff;
import com.google.common.util.concurrent.AtomicDouble;
import net.mizukilab.pit.enchantment.AbstractEnchantment;
import net.mizukilab.pit.enchantment.IActionDisplayEnchant;
import net.mizukilab.pit.enchantment.param.event.PlayerOnly;
import net.mizukilab.pit.enchantment.param.item.WeaponOnly;
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity;
import net.mizukilab.pit.parm.listener.IAttackEntity;
import net.mizukilab.pit.util.PlayerUtil;
import net.mizukilab.pit.util.cooldown.Cooldown;
import net.mizukilab.pit.util.time.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: Misoryan
 * @Created_In: 2021/2/27 16:34
 */
@WeaponOnly
public class HemorrhageEnchant extends AbstractEnchantment implements IAttackEntity, IActionDisplayEnchant {

    private static final HashMap<UUID, Cooldown> cooldown = new HashMap<>();
    private static final HashMap<UUID, Cooldown> immune = new HashMap<>();
    private static final HemorrhageDeBuff buff = new HemorrhageDeBuff();


    @Override
    public String getEnchantName() {
        return "嗜血";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "Hemorrhage";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.RARE;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    private double getTrueDamage(int enchantLevel) {
        return enchantLevel + 1.0;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "&7攻击对敌方施加以下效果: /s" +
                "  &f▶ &c流血 &f(" + TimeUtil.millisToTimer((enchantLevel * 2L) * 1000) + ")/s" +
                "  &f▶ &c缓慢 &f(" + TimeUtil.millisToTimer((enchantLevel * 2L) * 1000) + ") &7效果. (" + (8 - enchantLevel * 2) + "秒冷却) /s" +
                "  &f▶ &4侵蚀 &f(" + TimeUtil.millisToTimer((enchantLevel * 2L) * 1000) + ") &7效果. (" + (8 - enchantLevel * 2) + "秒冷却) /s" +
                "/s&7效果 &c流血 &7: 无法受到与被施加 &6生命吸收 &7效果" +
                "/s&7效果 &4侵蚀 &7: 受到或被施加 &6生命吸收 &7效果时, 将转化为对应数值的&f真实伤害";
    }

    @Override
    @PlayerOnly
    public void handleAttackEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        cooldown.putIfAbsent(attacker.getUniqueId(), new Cooldown(0));
        if (cooldown.get(attacker.getUniqueId()).hasExpired()) {
            Player targetPlayer = (Player) target;
            if (targetPlayer == null || "bot".equalsIgnoreCase(targetPlayer.getName())) return;
            if (immune.getOrDefault(targetPlayer.getUniqueId(), new Cooldown(0)).hasExpired()) {
                cooldown.put(attacker.getUniqueId(), new Cooldown(8 - enchantLevel * 2L, TimeUnit.SECONDS));
                immune.put(targetPlayer.getUniqueId(), new Cooldown(enchantLevel * 2L, TimeUnit.SECONDS));
                targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (enchantLevel * 2) * 20, 0), false);
                buff.stackBuff(targetPlayer, (enchantLevel * 2L) * 20);
                long startTime = System.currentTimeMillis();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!targetPlayer.isOnline()) {
                            this.cancel();
                            return;
                        }
                        targetPlayer.removePotionEffect(PotionEffectType.ABSORPTION);
                        CraftPlayer craftPlayer = (CraftPlayer) Bukkit.getPlayer(targetPlayer.getUniqueId());
                        if (craftPlayer == null) {
                            this.cancel();
                            return;
                        }
                        float absorptionHearts = craftPlayer.getHandle().getAbsorptionHearts();
                        craftPlayer.getHandle().setAbsorptionHearts(0f);
                        if (absorptionHearts > 0f) {
                            PlayerUtil.damage(
                                    attacker,
                                    targetPlayer,
                                    PlayerUtil.DamageType.TRUE,
                                    getTrueDamage((int) absorptionHearts),
                                    true);
                        }

                        if (System.currentTimeMillis() - startTime >= (enchantLevel * 2L) * 1000) {
                            this.cancel();
                        }
                    }
                }.runTaskTimer(ThePit.getInstance(), 1L, 1L);
            }
        }
    }

    @Override
    public String getText(int level, Player player) {
        return cooldown.getOrDefault(player.getUniqueId(), new Cooldown(0)).hasExpired() ? "&a&l✔" : "&c&l" + TimeUtil.millisToRoundedTime(cooldown.get(player.getUniqueId()).getRemaining()).replace(" ", "");
    }
}

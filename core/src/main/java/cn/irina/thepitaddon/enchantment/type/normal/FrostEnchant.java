package cn.irina.thepitaddon.enchantment.type.normal;

import cn.charlotte.pit.ThePit;
import cn.irina.thepitaddon.utils.TimeUtil;
import com.google.common.util.concurrent.AtomicDouble;
import net.mizukilab.pit.enchantment.AbstractEnchantment;
import net.mizukilab.pit.enchantment.IActionDisplayEnchant;
import net.mizukilab.pit.enchantment.param.item.WeaponOnly;
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity;
import net.mizukilab.pit.parm.listener.IAttackEntity;
import net.mizukilab.pit.util.PlayerUtil;
import net.mizukilab.pit.util.chat.CC;
import net.mizukilab.pit.util.chat.RomanUtil;
import net.mizukilab.pit.util.cooldown.Cooldown;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import real.nanoneko.register.IMagicLicense;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@WeaponOnly
public class FrostEnchant extends AbstractEnchantment implements IAttackEntity, IActionDisplayEnchant, IMagicLicense {
    private static final HashMap<UUID, Integer> hitCounts = new HashMap<>();

    public String getEnchantName() {
        return "强力击: 寒霜";
    }

    public int getMaxEnchantLevel() {
        return 3;
    }

    public String getNbtName() {
        return "frost";
    }

    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.RARE;
    }

    public Cooldown getCooldown() {
        return null;
    }

    public String getUsefulnessLore(int enchantLevel) {
        return "&7每攻击命中敌方 &e" + onHits(enchantLevel) + " &7次时 /s" +
                "&7将会为攻击者施加 &b寒霜侵袭 &f(" + TimeUtil.formatTotalSeconds(enchantLevel) + ") &7效果 /s" +
                "&7&b寒霜侵袭&f: &7给予目标 &c缓慢 " + RomanUtil.convert(getSlownessLevel(enchantLevel)) + " &7效果 /s" +
                "&7并且每秒对目标造成 &c0.5❤ &7的&c必中&7伤害";
    }

    private int onHits(int enchantLevel) {
        return enchantLevel >= 3 ? 2 : 4 - enchantLevel;
    }

    private int getSlownessLevel(int enchantLevel) {
        return enchantLevel >= 3 ? 2 : 1;
    }

    @Override
    public void handleAttackEntity(
            int enchantLevel,
            Player attacker,
            Entity target,
            double damage,
            AtomicDouble boostDamage,
            AtomicDouble reduceDamage,
            AtomicBoolean cancel
    ) {
        if (!(target instanceof Player targetPlayer)) return;

        int currentHits = hitCounts.getOrDefault(attacker.getUniqueId(), 0) + 1;
        hitCounts.put(attacker.getUniqueId(), currentHits);

        if (currentHits >= onHits(enchantLevel)) {
            hitCounts.put(attacker.getUniqueId(), 0);
            targetPlayer.sendMessage(CC.translate("&c&l寒霜侵袭! &7你的移速下降了!"));
            attacker.sendMessage(CC.translate("&b&l寒霜侵袭! &7目标移速下降了!"));
            targetPlayer.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.SLOW,
                            20 * enchantLevel,
                            getSlownessLevel(enchantLevel) - 1,
                            false,
                            true
                    )
            );
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (targetPlayer.isOnline() && !targetPlayer.isDead()) {
                        PlayerUtil.damage(targetPlayer, PlayerUtil.DamageType.TRUE, 1.0, false);
                    }
                }
            }.runTaskLater(ThePit.getInstance(), 20L);
        }
    }

    @Override
    public String getText(int enchantLevel, Player player) {
        int currentHits = hitCounts.getOrDefault(player.getUniqueId(), 0);
        int requiredHits = onHits(enchantLevel);
        if (currentHits >= requiredHits) {
            return "&a&l✔";
        } else {
            return "&e&l" + currentHits + "/" + requiredHits;
        }
    }
}
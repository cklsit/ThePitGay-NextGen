package cn.irina.thepitaddon.enchantment.type.normal;

import com.google.common.util.concurrent.AtomicDouble;
import net.mizukilab.pit.enchantment.AbstractEnchantment;
import net.mizukilab.pit.enchantment.param.event.PlayerOnly;
import net.mizukilab.pit.enchantment.param.item.WeaponOnly;
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity;
import net.mizukilab.pit.parm.listener.IAttackEntity;
import net.mizukilab.pit.util.cooldown.Cooldown;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicBoolean;

@WeaponOnly
public class Mythicism extends AbstractEnchantment implements IAttackEntity {
    @Override
    public String getEnchantName() {
        return "神话力场";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "mythicism";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.NORMAL;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "&7穿着 &6神话之甲 &7时造成的伤害 &c+" + (4 * enchantLevel + 4) + "%";
    }

    @Override
    @PlayerOnly
    public void handleAttackEntity(
            int enchantLevel,
            Player attacker,
            Entity target,
            double damage,
            AtomicDouble finalDamage,
            AtomicDouble boostDamage,
            AtomicBoolean cancel) {
        if (attacker.getInventory().getLeggings() != null && attacker.getInventory().getLeggings().getType().equals(Material.LEATHER_LEGGINGS)) {
            boostDamage.set(boostDamage.get() + (0.04 * enchantLevel) + 0.04);
        }
    }
}

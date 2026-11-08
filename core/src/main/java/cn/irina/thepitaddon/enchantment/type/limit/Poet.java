package cn.irina.thepitaddon.enchantment.type.limit;

import com.google.common.util.concurrent.AtomicDouble;
import net.mizukilab.pit.enchantment.AbstractEnchantment;
import net.mizukilab.pit.enchantment.param.item.WeaponOnly;
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity;
import net.mizukilab.pit.parm.listener.IAttackEntity;
import net.mizukilab.pit.util.cooldown.Cooldown;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicBoolean;

@WeaponOnly
public class Poet extends AbstractEnchantment implements IAttackEntity {

    @Override
    public String getEnchantName() {
        return "诗人";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "poet";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.OP;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "&7握持时攻击他人自己瞬间暴毙";
    }

    @Override
    public void handleAttackEntity(
            int i, Player attacker,
            Entity entity, double v,
            AtomicDouble atomicDouble,
            AtomicDouble atomicDouble1,
            AtomicBoolean atomicBoolean) {
        if (entity instanceof Player) {
            double maxHealth = ((Player) entity).getHealth();
            attacker.damage(maxHealth * 100);
        }
    }
}

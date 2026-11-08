package cn.irina.thepitaddon.enchantment.type.rare;

import cn.charlotte.pit.data.PlayerProfile;
import com.google.common.util.concurrent.AtomicDouble;
import net.mizukilab.pit.enchantment.AbstractEnchantment;
import net.mizukilab.pit.enchantment.IActionDisplayEnchant;
import net.mizukilab.pit.enchantment.param.item.WeaponOnly;
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity;
import net.mizukilab.pit.parm.listener.IAttackEntity;
import net.mizukilab.pit.util.cooldown.Cooldown;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import real.nanoneko.register.IMagicLicense;

import java.util.concurrent.atomic.AtomicBoolean;

@WeaponOnly
public class VitalEchoEnchant extends AbstractEnchantment implements IAttackEntity, IActionDisplayEnchant, IMagicLicense {

    public String getEnchantName() {
        return "生命回响";
    }

    public int getMaxEnchantLevel() {
        return 3;
    }

    public String getNbtName() {
        return "vital_echo";
    }

    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.RARE;
    }

    public Cooldown getCooldown() {
        return null;
    }

    public String getUsefulnessLore(int enchantLevel) {
        return "&7每次命中目标时将回复 &c0.5❤ &7生命值 /s" +
                "&7每命中目标 &e" + Hits(enchantLevel) + " &7次时 ,下次攻击将对目标额外造成 &c" + enchantLevel * 10 + "% &7的伤害/s" +
                "&7同时, 将额外回复 &c1.0❤ &7生命值";
    }

    private int Hits(int enchantLevel) {
        switch (enchantLevel) {
            case 2:
                return 5;
            case 3:
                return 4;
            default:
                return 6;
        }
    }

    @WeaponOnly
    public void handleAttackEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        int hit = attacker.getItemInHand() != null && attacker.getItemInHand().getType() == Material.BOW
                ? PlayerProfile.getPlayerProfileByUuid(attacker.getUniqueId()).getBowHit()
                : PlayerProfile.getPlayerProfileByUuid(attacker.getUniqueId()).getMeleeHit();

        int activeHitValue = Hits(enchantLevel);

        if (attacker.getHealth() <= attacker.getMaxHealth() - 1) {
            attacker.setHealth(attacker.getHealth() + 1);
        }

        if (hit % activeHitValue == 0) {
            boostDamage.getAndAdd(enchantLevel * 0.1);
            if (attacker.getHealth() <= attacker.getMaxHealth() - 2) {
                attacker.setHealth(attacker.getHealth() + 2);
            }
        }
    }

    public String getText(int enchantLevel, Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        int hit = (player.getItemInHand() != null && player.getItemInHand().getType() == Material.BOW) ?
                profile.getBowHit() : profile.getMeleeHit();
        return hit % Hits(enchantLevel) == 0 ? "&a&l✔" : "&e&l" + (Hits(enchantLevel) - hit % Hits(enchantLevel));
    }
}

package cn.irina.thepitaddon.enchantment.type.rare;

import cn.charlotte.pit.ThePit;
import com.google.common.util.concurrent.AtomicDouble;
import net.mizukilab.pit.enchantment.AbstractEnchantment;
import net.mizukilab.pit.enchantment.param.event.PlayerOnly;
import net.mizukilab.pit.enchantment.param.item.BowOnly;
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity;
import net.mizukilab.pit.parm.listener.IPlayerShootEntity;
import net.mizukilab.pit.util.PlayerUtil;
import net.mizukilab.pit.util.cooldown.Cooldown;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.Nullable;
import real.nanoneko.register.IMagicLicense;

import java.util.concurrent.atomic.AtomicBoolean;

@BowOnly

public class TearEnchant extends AbstractEnchantment implements Listener, IPlayerShootEntity, IMagicLicense {
    @Override
    public String getEnchantName() {
        return "撕裂";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "tear";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.RARE;
    }

    @Nullable
    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "&7射中目标为目标施加 &6撕裂 &f(00:0" + (enchantLevel + 2) + ") &7标记 /s" +
                "&7攻击带有标记 &6撕裂 &7的目标时: /s" +
                "&7每次攻击恢复自身已损生命值的 &c" + enchantLevel * 6 + "% /s" +
                "&7额外对目标造成 &c" + enchantLevel * 5 + "% &7的伤害";
    }

    @Override
    public void handleShootEntity(int enchantLevel, Player shooter, Entity target, double v, AtomicDouble atomicDouble, AtomicDouble atomicDouble1, AtomicBoolean atomicBoolean) {
        Player targetPlayer = (Player) target;

        if (targetPlayer.hasMetadata("tearDamager")) targetPlayer.removeMetadata("tearDamager", ThePit.getInstance());

        if (shooter.hasMetadata("tearAttacker")) shooter.removeMetadata("tearAttacker", ThePit.getInstance());

        targetPlayer.setMetadata("tearDamager", new FixedMetadataValue(ThePit.getInstance(), System.currentTimeMillis() + ((enchantLevel + 2) * 1000L)));
        shooter.setMetadata("tearAttacker", new FixedMetadataValue(ThePit.getInstance(), System.currentTimeMillis() + ((enchantLevel + 2) * 1000L)));
    }

    @PlayerOnly
    @EventHandler(
            priority = EventPriority.LOW
    )
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player target = (Player)event.getEntity();
        if (!(event.getDamager() instanceof Arrow) && !(event.getDamager() instanceof Player)) return; // 修复逻辑判断条件
        Player attacker = (Player)(event.getDamager() instanceof Arrow ? ((Arrow) event.getDamager()).getShooter() : event.getDamager());

        if (ThePit.getApi() == null) return;
        if (attacker.getInventory().getLeggings() == null) return;
        
        int enchantLevel = ThePit.getApi().getItemEnchantLevel(attacker.getInventory().getLeggings(), this.getNbtName());

        if (PlayerUtil.isVenom(attacker) || PlayerUtil.isVenom(target)) return;

        if (target.hasMetadata("tearDamager") && target.getMetadata("tearDamager").get(0).asLong() > System.currentTimeMillis()) {
            if (attacker.hasMetadata("tearAttacker") && attacker.getMetadata("tearAttacker").get(0).asLong() > System.currentTimeMillis()) {
                event.setDamage(event.getFinalDamage() * (1 + (enchantLevel * 0.05)));
                double lostHealth = attacker.getMaxHealth() - attacker.getHealth();
                PlayerUtil.heal(attacker, lostHealth * (enchantLevel * 0.06));
            }
        }
    }
}
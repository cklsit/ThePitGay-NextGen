package cn.irina.thepitaddon.enchantment.type.limit;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import net.mizukilab.pit.enchantment.AbstractEnchantment;
import net.mizukilab.pit.enchantment.IActionDisplayEnchant;
import net.mizukilab.pit.enchantment.param.item.ArmorOnly;
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity;
import net.mizukilab.pit.parm.listener.IAttackEntity;
import net.mizukilab.pit.parm.listener.IPlayerDamaged;
import net.mizukilab.pit.util.PlayerUtil;
import net.mizukilab.pit.util.chat.CC;
import net.mizukilab.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.Nullable;


import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@ArmorOnly
public class DoomPact extends AbstractEnchantment implements  IAttackEntity, IPlayerDamaged, Listener, IActionDisplayEnchant {
    private static final HashMap<UUID, UUID> lockPlayer = new HashMap<>();

    @Override
    public String getEnchantName() {
        return "厄兆同契";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "doom_pact";
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
        return "&7命中 &f非Bot &7目标时将锁定该目标, 并将此后受到的伤害作为 &c普通伤害 &7分出 &c" + (10 + (enchantLevel * 5)) + "% &7施加给锁定目标, 同时, 自身恢复 &c0.75❤ /s" +
                "&7(自身死亡后取消锁定)";
    }

    @Override
    public void handleAttackEntity(int i, Player attacker, Entity entity, double v, AtomicDouble atomicDouble, AtomicDouble atomicDouble1, AtomicBoolean atomicBoolean) {
        if (!(entity instanceof Player)) return;
        Player target = (Player) entity;

        if (target.hasMetadata("NPC")) return;

        if (lockPlayer.get(attacker.getUniqueId()) != null) return;
        lockPlayer.put(attacker.getUniqueId(), target.getUniqueId());
    }

    @Override
    public void handlePlayerDamaged(int enchantLevel, Player victim, Entity entity, double damage, AtomicDouble atomicDouble, AtomicDouble finalDamage, AtomicBoolean atomicBoolean) {
        if (lockPlayer.get(victim.getUniqueId()) == null) return;
        Player target = Bukkit.getPlayer(lockPlayer.get(victim.getUniqueId()));

        if (target == null) return;
        PlayerProfile tp = PlayerProfile.getRawCache(target.getUniqueId());

        if (!tp.isInArena()) return;
        PlayerUtil.heal(victim, 1.5);
        double gotDamage = damage * (1 - (0.1 + (enchantLevel * 0.05)));

        target.damage(gotDamage);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (ThePit.getApi().getItemEnchantLevel(player.getInventory().getLeggings(), this.getNbtName()) < 1) return;

        if (lockPlayer.get(player.getUniqueId()) == null) return;
        lockPlayer.remove(player.getUniqueId());
    }

    @Override
    public String getText(int i, Player player) {
        Player target = Bukkit.getPlayer(lockPlayer.get(player.getUniqueId()));
        if (target == null) return CC.translate("&c无目标");

        return CC.translate("&e锁定: " + target.getDisplayName());
    }
}

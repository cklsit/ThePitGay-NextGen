package cn.irina.thepitaddon.enchantment.type.limit;

import com.google.common.util.concurrent.AtomicDouble;
import net.mizukilab.pit.enchantment.AbstractEnchantment;
import net.mizukilab.pit.enchantment.IActionDisplayEnchant;
import net.mizukilab.pit.enchantment.param.item.ArmorOnly;
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity;
import net.mizukilab.pit.parm.listener.IAttackEntity;
import net.mizukilab.pit.parm.listener.IPlayerDamaged;
import net.mizukilab.pit.util.chat.CC;
import net.mizukilab.pit.util.cooldown.Cooldown;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@ArmorOnly
public class Test extends AbstractEnchantment implements IAttackEntity, IPlayerDamaged{
    private static final HashMap<UUID, UUID> lockPlayer = new HashMap<>();

    @Override
    public String getEnchantName() {
        return "测试";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "test";
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
        return "受到攻击时, 输出玩家的格挡状态";
    }

    @Override
    public void handleAttackEntity(int i, Player attacker, Entity entity, double v, AtomicDouble atomicDouble, AtomicDouble atomicDouble1, AtomicBoolean atomicBoolean) {
        if (!(entity instanceof Player)) return;
        attacker.sendMessage(CC.translate("&c" + attacker.isBlocking() + " " + ((Player) entity).isBlocking()));
    }

    @Override
    public void handlePlayerDamaged(int enchantLevel, Player victim, Entity entity, double damage, AtomicDouble atomicDouble, AtomicDouble finalDamage, AtomicBoolean atomicBoolean) {
        if (!(entity instanceof Player)) return;
        victim.sendMessage(CC.translate("&c" + victim.isBlocking() + " " + ((Player) entity).isBlocking()));
    }
}

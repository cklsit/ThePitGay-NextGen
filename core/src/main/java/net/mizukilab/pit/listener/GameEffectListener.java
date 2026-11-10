package net.mizukilab.pit.listener;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.KillRecap;
import cn.charlotte.pit.data.sub.PerkData;
import cn.charlotte.pit.data.sub.QuestData;
import cn.charlotte.pit.event.OriginalTimeChangeEvent;
import cn.charlotte.pit.event.PitDamageEvent;
import cn.charlotte.pit.event.PitDamagePlayerEvent;
import cn.charlotte.pit.perk.AbstractPerk;
import cn.charlotte.pit.perk.PerkFactory;
import com.google.common.util.concurrent.AtomicDouble;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.SneakyThrows;
import net.mizukilab.pit.config.NewConfiguration;
import net.mizukilab.pit.enchantment.AbstractEnchantment;
import net.mizukilab.pit.enchantment.EnchantmentFactor;
import net.mizukilab.pit.enchantment.HitTickGuard;
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity;
import net.mizukilab.pit.enchantment.type.ragerare.Regularity;
import net.mizukilab.pit.item.AbstractPitItem;
import net.mizukilab.pit.item.IMythicItem;
import net.mizukilab.pit.item.MythicColor;
import net.mizukilab.pit.item.type.mythic.MythicBowItem;
import net.mizukilab.pit.item.type.mythic.MythicLeggingsItem;
import net.mizukilab.pit.parm.listener.*;
import net.mizukilab.pit.quest.AbstractQuest;
import net.mizukilab.pit.util.Einstein;
import net.mizukilab.pit.util.PlayerUtil;
import net.mizukilab.pit.util.RangedStreamLineList;
import net.mizukilab.pit.util.Utils;
import net.mizukilab.pit.util.chat.CC;
import net.mizukilab.pit.util.item.ItemUtil;
import nya.Skip;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/2 12:40
 */
@Skip
public class GameEffectListener implements Listener {

    private final DecimalFormat numFormatTwo = new DecimalFormat("0.00");
    public static void processKilled(IPlayerKilledEntity ins, int level, Player killer, Entity target, AtomicDouble coin, AtomicDouble exp) {
        if (level != -1 && CallbackFilters.KILLED.allows(ins, target)) {
            ins.handlePlayerKilled(level, killer, target, coin, exp);
        }
    }

    public static void processBeKilledByEntity(IPlayerBeKilledByEntity ins, int level, Player myself, Entity target, AtomicDouble coin, AtomicDouble exp) {
        if (level != -1 && CallbackFilters.BE_KILLED.allows(ins, target)) {
            ins.handlePlayerBeKilledByEntity(level, myself, target, coin, exp);
        }
    }

    private static void fixDamage(EntityDamageByEntityEvent event, Entity damager1) {
        if (damager1 instanceof Player attacker) {
            // Regularity already computed this hit from the preceding attack, not weapon base damage.
            if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                    && Regularity.isFollowUp(attacker, event.getEntity())) return;
            //修正伤害
            ItemStack itemInHand = attacker.getItemInHand();
            if (itemInHand == null || itemInHand.getType() == Material.AIR
                    || itemInHand.getType() == Material.FISHING_ROD) {
                event.setDamage(1);
            } else {
                event.setDamage(Einstein.clamp(event.getDamage(), 0, 9));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerFired(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && event.getCause().equals(EntityDamageEvent.DamageCause.FIRE_TICK)) {
//            event.setCancelled(true);
////            if (player.getHealth() <= event.getFinalDamage()) {
////                player.damage(player.getMaxHealth() * 100);
////            } else {
////                double newHealth = player.getHealth() - event.getFinalDamage();
////                // 确保生命值在有效范围内
////                player.setHealth(Math.max(0, Math.min(newHealth, player.getMaxHealth())));
////            }
//
            PlayerProfile.getPlayerProfileByUuid(player.getUniqueId())
                    .setNoDamageAnimations(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void modifyLeatherArmor(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player op) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(op.getUniqueId());
            if (ItemUtil.isTheItemType(op.getInventory().getHelmet(),"kings_helmet")) {
                //0.12是每点伤害 铁裤和皮革裤子的减免差距，所以减去0.12*伤害值，以增加皮革裤子为铁裤防御
                event.setDamage(EntityDamageEvent.DamageModifier.ARMOR, event.getDamage(EntityDamageEvent.DamageModifier.ARMOR) - 0.06 * event.getDamage());
            }
            AbstractPitItem leggings = profile.leggings;
            if(leggings instanceof MythicLeggingsItem i){
                if(i.getColor() == MythicColor.DARK){
                    return;
                }
                event.setDamage(EntityDamageEvent.DamageModifier.ARMOR, event.getDamage(EntityDamageEvent.DamageModifier.ARMOR) - 0.12 * event.getDamage());
            }

        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void addEnchantToArrow(EntityShootBowEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof Player)) return;
        ItemStack firedBow = event.getBow();
        if (firedBow == null || firedBow.getType() != Material.BOW) return;
        // The event's bow is authoritative even before the next equipment tick.
        // Finalize after extensions have replaced/cancelled the original projectile.
        if (!(Utils.getMythicItem(firedBow) instanceof MythicBowItem bowItem)) return;
        event.getProjectile().setMetadata("enchant", new FixedMetadataValue(ThePit.getInstance(),
                bowItem.getEnchantments().clone()));
        // An in-flight arrow must not share a mutable enchantment map with its source item.
        Utils.pointMetadataAndRemove(event.getProjectile(), 500, "enchant");
    }

    @EventHandler
    public void onTimeChange(OriginalTimeChangeEvent event) {
        CC.boardCast("Time change to: " + event.getTime());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerDamagePlayer(EntityDamageByEntityEvent event) {
        // NextGen：实体已死亡时不再结算（避免对尸体重复触发）
        if (event.getEntity() instanceof org.bukkit.entity.LivingEntity le && le.getHealth() <= 0) {
            event.setCancelled(true);
            return;
        }
        if (event.getDamager() instanceof Player attacker) {
            if (NewConfiguration.INSTANCE.getRepairFeatures())  {
                if (event.getEntity() instanceof org.bukkit.entity.LivingEntity livingEntity) { //特性修复 //TODO
                    if (livingEntity.getNoDamageTicks() > 5) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
            //修正伤害
            ItemStack itemInHand = attacker.getInventory().getItemInMainHand();
            if (itemInHand == null || itemInHand.getType() == Material.AIR
                    || itemInHand.getType() == Material.FISHING_ROD) {
                event.setDamage(1);
            } else {
                float f = (float) attacker.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
                if (f > 9) {
                    event.setDamage(9);
                }
            }
        }
        fixDamage(event, damagerEntity);

        ThePit instance = ThePit.getInstance();
        PerkFactory perkFactory = instance.getPerkFactory();
        EnchantmentFactor enchantmentFactor = instance.getEnchantmentFactor();
        AtomicDouble finalDamage = new AtomicDouble(event.getDamage());
        AtomicDouble boostDamage = new AtomicDouble(1);
        AtomicBoolean cancel = new AtomicBoolean(false);
        Player damager = null;

        PlayerProfile playerProfileByUuid = PlayerProfile.getPlayerProfile(damagerEntity);
        Set<AbstractPerk> disabledPerks = instance.getDisabledPerks();
        NewConfiguration instance1 = NewConfiguration.INSTANCE;
        if (damagerEntity instanceof Player) {
            damager = (Player) damagerEntity;
            Utils.refreshPlayerEquipment(damager, playerProfileByUuid);

            if (PlayerUtil.critical(damager)){
                boostDamage.set(1.5);
            }
            if (instance1.getNoobProtect() && playerProfileByUuid.getPrestige() <= 0 && playerProfileByUuid.getLevel() < instance1.getNoobProtectLevel()) {
                boostDamage.getAndAdd(instance1.getNoobDamageBoost() - 1);
            }
            //TODO perk handlers
            //perk handler
            //faster
            processPerkATK(event, playerProfileByUuid, perkFactory, disabledPerks, damager, finalDamage, boostDamage, cancel);
            //TODO heldItem -- leggings handlers
            boolean shouldIgnoreEnchant;
            List<IAttackEntity> attackEntities = enchantmentFactor.getAttackEntities();
            if (!attackEntities.isEmpty()) {
                shouldIgnoreEnchant = PlayerUtil.shouldIgnoreEnchant(damager
                        , entity);
            } else {
                shouldIgnoreEnchant = false;
            }
            IMythicItem heldItem = (IMythicItem) playerProfileByUuid.heldItem;
            IMythicItem leggings = (IMythicItem) playerProfileByUuid.leggings;
            //十分甚至九分的重构

            if ((heldItem != null || leggings != null)) {
                processAttackWithLeggingAndHeldItems(shouldIgnoreEnchant, event, damager, finalDamage, boostDamage, cancel, heldItem, leggings);
            }
            //TODO quest handler

            QuestData currentQuest1 = playerProfileByUuid.getCurrentQuest();
            if (currentQuest1 != null) {
                AbstractQuest handle = currentQuest1.getHandle();
                if (handle instanceof IAttackEntity iac) {
                    processAttackEntity(iac, currentQuest1.getLevel(), damager, entity, event.getFinalDamage(), finalDamage, boostDamage, cancel);

                }
            }
            //When you were hurt by a projectile
        } else if (damagerEntity instanceof Projectile projectile && projectile.getShooter() instanceof Player) {
            damager = (Player) (projectile.getShooter());
            playerProfileByUuid = PlayerProfile.getPlayerProfile(damager);

            if (instance1.getNoobProtect() && playerProfileByUuid.getPrestige() <= 0 && playerProfileByUuid.getLevel() < instance1.getNoobProtectLevel()) {
                boostDamage.getAndAdd(instance1.getNoobDamageBoost() - 1);
            }

            for (IPlayerShootEntity ins : perkFactory.getPlayerShootEntities()) {
                AbstractPerk perk = (AbstractPerk) ins;
                int level = perk.getPlayerLevel(damager);
                processShootEntity(ins, level, damager, entity, damagerEntity, event.getFinalDamage(), finalDamage, boostDamage, cancel);
            }
            //projectile handler
            List<MetadataValue> enchant = projectile.getMetadata("enchant");
            if (!enchant.isEmpty()) {
                final MetadataValue value = enchant.get(0);
                final Object enchants = value.value();
                if (enchants instanceof Object2IntOpenHashMap a) {
                    Object2IntOpenHashMap<AbstractEnchantment> ench = (Object2IntOpenHashMap<AbstractEnchantment>) a;
                    boolean shouldIgnoreEnchant = PlayerUtil.shouldIgnoreEnchant(damager, entity);
                    processShot(shouldIgnoreEnchant, event, ench, damager, finalDamage, boostDamage, cancel);
                }
            }
            QuestData currentQuest1 = playerProfileByUuid.getCurrentQuest();
            if (currentQuest1 != null) {
                AbstractQuest handle = currentQuest1.getHandle();
                if (handle instanceof IPlayerShootEntity isc) {
                    processShootEntity(isc, currentQuest1.getLevel(), damager, entity, damagerEntity, event.getFinalDamage(), finalDamage, boostDamage, cancel);

                }
            }
            projectile.removeMetadata("enchant", instance); //garbage collector
        }

        if (entity instanceof Player player) {

            boolean npc = PlayerUtil.isNPC(player);

            if (!npc) {//针对npc不生效。
                PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(entity.getUniqueId());
                Utils.refreshPlayerEquipment(player, profile);

                if (instance1.getNoobProtect() && profile.getPrestige() <= 0
                        && profile.getLevel() < instance1.getNoobProtectLevel()) {
                    boostDamage.getAndAdd(instance1.getNoobDamageReduce() - 1);
                }
                if (PlayerUtil.isEquippingAngelChestplate(player)) {
                    boostDamage.getAndAdd(-0.1);
                }
                if (PlayerUtil.isEquippingAngelBoots(player)) {
                    boostDamage.getAndAdd(-0.1);
                }
                if (PlayerUtil.isEquippingAngelHelmet(player)) {
                    boostDamage.getAndAdd(-0.1);
                }
                if (PlayerUtil.isEquippingAngelLeggings(player)) {
                    boostDamage.getAndAdd(-0.1);
                }
                if (damagerEntity instanceof Player) {
                    processPerksDMGed(event, player, profile, perkFactory, disabledPerks, finalDamage, boostDamage, cancel);

                    if (!enchantmentFactor.getPlayerDamageds().isEmpty()) {
                        boolean shouldIgnoreEnchant = PlayerUtil.shouldIgnoreEnchant(damager, entity);
                        IMythicItem leggings = (IMythicItem) profile.leggings;
                        IMythicItem sword = (IMythicItem) profile.heldItem;
                        if (!(sword instanceof MythicLeggingsItem)) {
                            processEnchDMGed(event, player, leggings, shouldIgnoreEnchant, finalDamage, boostDamage, cancel);
                        }
                        processEnchDMGed(event, player, sword, shouldIgnoreEnchant, finalDamage, boostDamage, cancel);

                    }


                    QuestData currentQuest1 = profile.getCurrentQuest();
                    processQuestDmged(event, player, currentQuest1, finalDamage, boostDamage, cancel);
                } else if (damagerEntity instanceof Projectile
                        && ((Projectile) damagerEntity).getShooter() instanceof Player) {
                    damager = (Player) ((Projectile) damagerEntity).getShooter();
                    processPerksDMGed(event, player, profile, perkFactory, disabledPerks, finalDamage, boostDamage, cancel);

                    if (!enchantmentFactor.getPlayerDamageds().isEmpty()) {
                        boolean shouldIgnoreEnchant = PlayerUtil.shouldIgnoreEnchant(damager, entity);
                        IMythicItem leggings = (IMythicItem) profile.leggings;
                        IMythicItem sword = (IMythicItem) profile.heldItem;
                        processEnchDMGed(event, player, leggings, shouldIgnoreEnchant, finalDamage, boostDamage, cancel);
                        processEnchDMGed(event, player, sword, shouldIgnoreEnchant, finalDamage, boostDamage, cancel);
                    }


                    QuestData currentQuest1 = profile.getCurrentQuest();
                    processQuestDmged(event, player, currentQuest1, finalDamage, boostDamage, cancel);
                }
                if (damager != null) {
                    double finalDamage1 = event.getFinalDamage();
                    new PitDamagePlayerEvent(event, damager, finalDamage1, event.getDamage(), player).callEvent();
                    new PitDamageEvent(event, damager, finalDamage1, event.getDamage()).callEvent();
                    RangedStreamLineList<KillRecap.DamageData> damageLogs = playerProfileByUuid.getKillRecap().getDamageLogs();
                    if (!damageLogs.isEmpty()) {
                        damageLogs.peekFirst().setBoostDamage(boostDamage.get());
                    }
                }
            }
            //mirror enchant code end
            player.setLastDamageCause(event);
            EntityPlayer handle = ((CraftPlayer) player).getHandle();
            if (damager != null) {
                // 1.21：击杀者归属用 metadata 记录（死亡结算读取）
                player.setMetadata("pit_killer", new org.bukkit.metadata.FixedMetadataValue(
                        ThePit.getInstance(), damager.getUniqueId().toString()));
            }

            if (player.getHealth() < finalDamage.get()) {
                // 添加安全检查，避免机器人死亡时的NullPointerException
                try {
                    player.damage(500000.0);
                } catch (Exception e) {
                    // 如果damage方法出错，直接设置生命值为0
                    try {
                        player.setHealth(0);
                    } catch (Exception ex) {
                        // 如果setHealth也失败，尝试使用更底层的方式
                        try {
                            ((org.bukkit.entity.Player) player).setHealth(0.0f);
                        } catch (Exception ignored) {
                            // 最后的安全网，静默忽略
                        }
                    }
                }
                event.setCancelled(true);
            } else { //TODO 修正 一击毙命, 但是event不cancel
                double v = player.getHealth() - finalDamage.get();
                if (v > 0) {
                    // 确保生命值不超过最大生命值上限
                    try {
                        player.setHealth(Math.min(v, player.getMaxHealth()));
                    } catch (Exception e) {
                        // 如果setHealth失败，尝试直接设置为当前生命值（不改变）
                        try {
                            player.setHealth(player.getHealth());
                        } catch (Exception ignored) {
                            // 静默忽略
                        }
                    }
                } else {
                    try {
                        player.setHealth(0);
                    } catch (Exception e) {
                        // 如果setHealth失败，尝试使用底层API
                        try {
                            ((org.bukkit.entity.Player) player).setHealth(0.0f);
                        } catch (Exception ignored) {
                            // 最后的安全网，静默忽略
                        }
                    }
                }
            }
            //we need to ensure the final damage is pushed into the event
            event.setDamage(finalDamage.get() * Einstein.clamp(boostDamage.get(), 0, 100));
        }

        if (!cancel.get()) {
            if (damagerEntity instanceof FishHook hook) {
                final PlayerProfile damagerProfile = PlayerProfile.getPlayerProfileByUuid(((Player) hook.getShooter()).getUniqueId());
                damagerProfile.setFishingNumber(damagerProfile.getFishingNumber() + 1);
            }
        } else {
            event.setCancelled(true);
            return;
        }
        debug(event, damagerEntity, cancel);
    }

    static Vector ZERO = new Vector();

    private void debug(EntityDamageByEntityEvent event, Entity damagerEntity, AtomicBoolean cancel) {
        if (event.getEntity() instanceof Player player) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            if (profile.getPlayerOption().isDebugDamageMessage()) {
                player.sendMessage(CC.translate("&7受到伤害(Damage/Final Damage): &c" + numFormatTwo.format(event.getDamage()) + "&7/&c" + numFormatTwo.format(event.getFinalDamage())));
            }
        }
        if (damagerEntity instanceof Player player && !(event.getEntity() instanceof Item)) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            if (profile.getPlayerOption().isDebugDamageMessage()) {
                player.sendMessage(CC.translate("&7造成伤害(Damage/Final Damage): &c" + numFormatTwo.format(event.getDamage()) + "&7/&c" + numFormatTwo.format(event.getFinalDamage())));
                final double value = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
                final float enchantDamage = 0f; // 1.21：附魔额外伤害由属性系统统一计算
                final boolean critical = player.getFallDistance() > 0.0F && !player.isOnGround()
                        && !player.isInWater() && !player.isInsideVehicle()
                        && player.getPotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS) == null;
                player.sendMessage(CC.translate("&7基础: &c" + value + "&7,附魔伤害: &c" + enchantDamage + "&7,cancel:" + cancel.get() + " ,暴击: &c" + critical));
            }
        }
    }

    private void processAttackWithLeggingAndHeldItems(boolean shouldIgnore, EntityDamageByEntityEvent event, Player finalDamager1, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel, IMythicItem heldItem, IMythicItem leggings) {
        var registry = ThePit.getInstance().getEnchantmentFactor().getEnchantmentMap();
        BiConsumer<AbstractEnchantment, Integer> consumer = (storedEnchantment, level) -> {
            AbstractEnchantment enchant = registry.get(storedEnchantment.getNbtName());
            if (enchant instanceof IAttackEntity ins) {
                if (shouldIgnore && (enchant.getRarity() != EnchantmentRarity.DARK_NORMAL
                        && enchant.getRarity() != EnchantmentRarity.DARK_RARE)) {
                    return;
                }
                processAttackEntity(ins, level, finalDamager1, event.getEntity(), event.getFinalDamage(), finalDamage, boostDamage, cancel);
            }
        };
        if (heldItem != null) {
            heldItem.getEnchantments().forEach(consumer);
        }
        if (leggings != null) {
            leggings.getEnchantments().forEach(consumer);
        }
    }

    private void processShot(boolean shouldIgnoreEnch, EntityDamageByEntityEvent event, Object2IntOpenHashMap<AbstractEnchantment> ench, Player finalDamager, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        var registry = ThePit.getInstance().getEnchantmentFactor().getEnchantmentMap();
        ench.forEach((storedEnchantment, level) -> {
            // Keep the shot's level, but use the currently installed implementation after a replacement.
            AbstractEnchantment enchantment = registry.get(storedEnchantment.getNbtName());
            if (enchantment instanceof IPlayerShootEntity enchantmentCasted) {
                EnchantmentRarity rarity = enchantment.getRarity();
                if (shouldIgnoreEnch && (rarity != EnchantmentRarity.DARK_NORMAL && rarity != EnchantmentRarity.DARK_RARE)) {
                    return;
                }

                this.processShootEntity(enchantmentCasted, level, finalDamager, event.getEntity(), event.getDamager(), event.getFinalDamage(), finalDamage, boostDamage, cancel);
            }
        });
    }

    private void processEnchDMGed(EntityDamageByEntityEvent event, Player player, IMythicItem leggings, boolean shouldIgnoreEnchant, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        if (leggings != null) {
            var registry = ThePit.getInstance().getEnchantmentFactor().getEnchantmentMap();
            leggings.getEnchantments().forEach((storedEnchantment, level) -> {
                AbstractEnchantment enchant = registry.get(storedEnchantment.getNbtName());
                if (enchant == null) return;
                if (shouldIgnoreEnchant && (enchant.getRarity() != EnchantmentRarity.DARK_NORMAL
                        && enchant.getRarity() != EnchantmentRarity.DARK_RARE)) {
                    return;
                }
                if (enchant instanceof IPlayerDamaged ins) {
                    processPlayerDamaged(ins, level, player, event.getDamager(), event.getFinalDamage(), finalDamage, boostDamage, cancel);
                }
            });
        }
    }

    private void processQuestDmged(EntityDamageByEntityEvent event, Player player, QuestData currentQuest1, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        if (currentQuest1 != null) {
            AbstractQuest handle = currentQuest1.getHandle();
            if (handle instanceof IPlayerDamaged ins) {
                processPlayerDamaged(ins, currentQuest1.getLevel(), player, event.getDamager(), event.getFinalDamage(), finalDamage, boostDamage, cancel);
            }
        }
    }

    private void processPerkATK(EntityDamageByEntityEvent event, PlayerProfile profile, PerkFactory perkFactory, Set<AbstractPerk> disabledPerks, Player finalDamager1, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        profile.getUnlockedPerkMap().values().forEach(i -> {
            AbstractPerk abstractPerk = i.getHandle(perkFactory.getPerkMap());

            if (abstractPerk == null || !abstractPerk.isPassive()) {
                return;
            }
            if (disabledPerks.contains(abstractPerk)) {
                return;
            }
            if (abstractPerk instanceof IAttackEntity attackEntity) {

                processAttackEntity(attackEntity, abstractPerk.getPlayerLevel(finalDamager1), finalDamager1, event.getEntity(), event.getFinalDamage(), finalDamage, boostDamage, cancel);

            }
        });
        profile.getChosePerk().values().forEach(i -> {
            AbstractPerk abstractPerk = i.getHandle(perkFactory.getPerkMap());
            if (abstractPerk == null || abstractPerk.isPassive()) {
                return;
            }
            if (disabledPerks.contains(abstractPerk)) {
                return;
            }
            if (abstractPerk instanceof IAttackEntity attackEntity) {

                processAttackEntity(attackEntity, i.getLevel(), finalDamager1, event.getEntity(), event.getFinalDamage(), finalDamage, boostDamage, cancel);

            }
        });
    }

    private void processPerksDMGed(EntityDamageByEntityEvent event, Player player, PlayerProfile profile, PerkFactory perkFactory, Set<AbstractPerk> disabledPerks, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        profile.getUnlockedPerkMap().values().forEach(i -> {
            AbstractPerk abstractPerk = i.getHandle(perkFactory.getPerkMap());
            if (abstractPerk != null && abstractPerk.isPassive() && i.getLevel() > 0) {
                takeEffect(event, player, disabledPerks, finalDamage, boostDamage, cancel, i, abstractPerk);
            }
        });
        profile.getChosePerk().values().forEach(i -> {
            AbstractPerk abstractPerk = i.getHandle(perkFactory.getPerkMap());
            if (abstractPerk != null && !abstractPerk.isPassive() && i.getLevel() >= 0) {
                takeEffect(event, player, disabledPerks, finalDamage, boostDamage, cancel, i, abstractPerk);
            }
        });
    }

    private void takeEffect(EntityDamageByEntityEvent event, Player player, Set<AbstractPerk> disabledPerks, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel, PerkData i, AbstractPerk abstractPerk) {
        if (disabledPerks.contains(abstractPerk)) {
            return;
        }
        if (abstractPerk instanceof IPlayerDamaged ins) {

            processPlayerDamaged(ins, i.getLevel(), player, event.getDamager(), event.getFinalDamage(), finalDamage, boostDamage, cancel);
        }
    }

    @EventHandler
    public void onItemDamaged(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        AtomicBoolean atomicBoolean = new AtomicBoolean();
        IMythicItem mm = Utils.getMythicItem(item);
        if (mm != null) {
            for (IItemDamage callback : ThePit.getInstance().getEnchantmentFactor().getIItemDamages()) {
                int level = ((AbstractEnchantment) callback).getItemEnchantLevel(mm);
                if (level > 0) callback.handleItemDamaged(level, item, event.getPlayer(), atomicBoolean);
            }
        }
        if (atomicBoolean.get()) {
            event.setCancelled(true);
        }
    }

    private void processShootEntity(IPlayerShootEntity ins, int level, Player damager, Entity target, Entity damageSource, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        if (level != -1 && CallbackFilters.SHOOT.allows(ins, target, damageSource)) {
            if (!HitTickGuard.attack(ins, damager, damageSource, cancel.get())) return;
            ins.handleShootEntity(level, damager, target, damage, finalDamage, boostDamage, cancel);
        }
    }

    private void processPlayerDamaged(IPlayerDamaged ins, int level, Player player, Entity damager, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        if (level != -1 && CallbackFilters.DAMAGED.allows(ins, damager)) {
            if (!HitTickGuard.damaged(ins, player, damager, cancel.get())) return;
            ins.handlePlayerDamaged(level, player, damager, damage, finalDamage, boostDamage, cancel);
        }
    }

    private void processAttackEntity(IAttackEntity ins, int level, Player damager, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        if (level != -1 && CallbackFilters.ATTACK.allows(ins, target)) {
            if (!HitTickGuard.attack(ins, damager, null, cancel.get())) return;
            // Legacy Addon treats a non-null AIR helmet (common on bots) as real armor.
            // Match its class, not its NBT, so an extension replacement retains its own behavior.
            if (ins.getClass().getName().equals("cn.apnf.mistral.enchantments.normal.RageKill")) {
                if (level > 0 && !cancel.get() && target instanceof Player player
                        && !PlayerUtil.shouldIgnoreEnchant(damager)) {
                    ItemStack helmet = player.getInventory().getHelmet();
                    if (helmet == null || helmet.getType() == Material.AIR) {
                        boostDamage.addAndGet(1D + level * 0.25D);
                    }
                }
                return;
            }
            ins.handleAttackEntity(level, damager, target, damage, finalDamage, boostDamage, cancel);
        }
    }

}

package net.mizukilab.pit.npc;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.event.PitProfileLoadedEvent;
import cn.charlotte.pit.util.hologram.Hologram;
import cn.charlotte.pit.util.hologram.HologramAPI;
import lombok.Getter;
import lombok.SneakyThrows;
import net.mizukilab.pit.npc.events.CustomEntityNPCInteractEvent;
import net.mizukilab.pit.util.chat.CC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * @Author: Araykal
 * @Date: 2025/6/21
 */
public class CustomEntityNPCFactory implements Listener {

    @Getter
    private final List<AbstractCustomEntityNPC> customEntityNPCs = new ArrayList<>();

    @Getter
    private final Map<UUID, AbstractCustomEntityNPC> entityToNPCMap = new HashMap<>();

    private final Map<UUID, Map<UUID, List<Hologram>>> playerNPCHolograms = new HashMap<>();

    private final Map<UUID, BukkitTask> fixedPositionTasks = new HashMap<>();

    private static final String NPC_TAG_KEY = "custom_npc";

    private NamespacedKey npcTagKey() {
        return new NamespacedKey(ThePit.getInstance(), NPC_TAG_KEY);
    }

    private void tagEntity(Entity entity, AbstractCustomEntityNPC npc) {
        try {
            entity.getPersistentDataContainer()
                    .set(npcTagKey(), PersistentDataType.STRING, npc.getNpcInternalName());
        } catch (Exception ignored) {
        }
    }

    private String npcTagOf(Entity entity) {
        try {
            return entity.getPersistentDataContainer().get(npcTagKey(), PersistentDataType.STRING);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 清理目标点位附近的残留 NPC。
     *
     * 历史版本只在启动时清扫「已加载区块」的实体，NPC 所在区块未加载时旧实体不会被删除，
     * 于是每次重启都会在同一坐标再生成一个，最终叠成一堆村民。
     * 这里在生成前强制加载该区块并就地清扫，同时兼容没有标记的旧版本实体
     * （无 AI 的同类型生物）。
     */
    private void clearStaleEntities(Location location, EntityType type) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        if (!world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            world.getChunkAt(location).load();
        }
        int removed = 0;
        for (Entity entity : world.getNearbyEntities(location, 2.5, 3.0, 2.5)) {
            if (entity instanceof Player) {
                continue;
            }
            // 已经登记并存活的其他 NPC 不动
            if (entityToNPCMap.containsKey(entity.getUniqueId())) {
                continue;
            }
            boolean tagged = npcTagOf(entity) != null;
            boolean legacyDuplicate = entity.getType() == type
                    && entity instanceof LivingEntity
                    && !((LivingEntity) entity).hasAI();
            if (tagged || legacyDuplicate) {
                entity.remove();
                removed++;
            }
        }
        if (removed > 0) {
            Bukkit.getLogger().info("[NPC] 清理 " + removed + " 个残留实体 @ "
                    + world.getName() + " " + location.getBlockX() + "," + location.getBlockY()
                    + "," + location.getBlockZ());
        }
    }

    public boolean isCustomNPC(Entity entity) {
        return entityToNPCMap.containsKey(entity.getUniqueId());
    }

    public AbstractCustomEntityNPC getCustomNPC(Entity entity) {
        return entityToNPCMap.get(entity.getUniqueId());
    }


    public void reload() {
        cleanup();
        customEntityNPCs.forEach(this::createNPCEntity);
        Bukkit.getOnlinePlayers().forEach(this::showNPCsToPlayer);
    }


    public void showNPCsToPlayer(Player player) {
        cleanupPlayerHolograms(player);
        
        customEntityNPCs.forEach(npc -> {
            if (npc.getEntity() != null && !npc.getEntity().isDead()) {
                createPersonalHologramForPlayer(npc, player);
            }
        });
    }
    
    private void cleanupPlayerHolograms(Player player) {
        Map<UUID, List<Hologram>> playerHolograms = playerNPCHolograms.get(player.getUniqueId());
        if (playerHolograms != null) {
            playerHolograms.values().forEach(holograms -> {
                holograms.forEach(hologram -> {
                    if (hologram.isSpawned()) {
                        hologram.deSpawn();
                    }
                });
            });
            playerHolograms.clear();
        }
    }
    
    private void createPersonalHologramForPlayer(AbstractCustomEntityNPC npc, Player player) {
        if (npc.getEntity() == null) return;

        double hologramHeight = getHologramHeight(npc.getEntityType());
        Location baseLocation = npc.getEntity().getLocation().add(0, hologramHeight, 0);
        List<String> lines = npc.getNpcTextLine(player);

        if (!lines.isEmpty()) {
            List<Hologram> holograms = new ArrayList<>();
            
            for (int i = 0; i < lines.size(); i++) {
                Location lineLocation = baseLocation.clone().add(0, (lines.size() - 1 - i) * 0.29, 0);
                Hologram hologram = HologramAPI.createHologram(lineLocation, CC.translate(lines.get(i)));
                hologram.spawn(Collections.singletonList(player));
                holograms.add(hologram);
            }

            playerNPCHolograms.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                    .put(npc.getEntity().getUniqueId(), holograms);
        }
    }


    @SneakyThrows
    public void init(Collection<Class<? extends AbstractCustomEntityNPC>> classes) {
        cleanupWorldEntities();

        Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> {
            for (Class<?> clazz : classes) {
                if (AbstractCustomEntityNPC.class.isAssignableFrom(clazz)) {
                    try {
                        AbstractCustomEntityNPC customNPC = (AbstractCustomEntityNPC) clazz.getConstructor().newInstance();
                        createNPCEntity(customNPC);
                        customEntityNPCs.add(customNPC);
                    } catch (Exception e) {
                        Bukkit.getLogger().severe("创建自定义NPC失败: " + clazz.getSimpleName() + " - " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }

        }, 20L);
        Bukkit.getScheduler().runTaskTimer(ThePit.getInstance(), this::maintainNPCs, 20L, 20L);
        Bukkit.getScheduler().runTaskTimer(ThePit.getInstance(), this::updateHolograms, 100L, 100L);
    }

    private void createNPCEntity(AbstractCustomEntityNPC npc) {
        try {
            Location location = npc.getNpcSpawnLocation();
            if (location == null || location.getWorld() == null) {
                return;
            }

            // 同一个 NPC 实例若已持有活体实体，先移除，避免重复堆叠
            Entity previous = npc.getEntity();
            if (previous != null && !previous.isDead()) {
                entityToNPCMap.remove(previous.getUniqueId());
                previous.remove();
            }

            // 生成前清扫该点位的残留实体（含历史版本遗留的重复村民）
            clearStaleEntities(location, npc.getEntityType());

            Entity entity = location.getWorld().spawnEntity(location, npc.getEntityType());
            tagEntity(entity, npc);


            initializeNPCEntity(entity, npc);


            npc.setEntity(entity);
            entityToNPCMap.put(entity.getUniqueId(), npc);

        } catch (Exception e) {
            Bukkit.getLogger().severe("创建NPC失败: " + npc.getNpcInternalName() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeNPCEntity(Entity entity, AbstractCustomEntityNPC npc) {

        npc.initializeEntity(entity);

        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;

            if (entity.getType() == EntityType.BAT) {
                try {
                    Bat bat = (Bat) entity;
                    bat.setAwake(true);

                    // 1.21：NoAI/Silent 直接走实体 API
                    livingEntity.setAI(false);
                    livingEntity.setSilent(true);

                    trackFixedPosition(entity);

                } catch (Exception e) {
                    Bukkit.getLogger().warning("失败: " + e.getMessage());
                }
            } else if (!npc.canMove()) {
                try {
                    livingEntity.setAI(false);
                    livingEntity.setSilent(true);
                } catch (Exception e) {
                    Bukkit.getLogger().warning("失败: " + e.getMessage());
                }
            }
            
            if (npc.getNpcHeldItem() != null && livingEntity instanceof org.bukkit.entity.LivingEntity) {
                org.bukkit.inventory.EntityEquipment equipment = livingEntity.getEquipment();
                if (equipment != null) {
                    equipment.setItemInHand(npc.getNpcHeldItem());
                }
            }

            if (entity instanceof Ageable) {
                Ageable ageable = (Ageable) entity;
                if (npc.isAdult()) {
                    ageable.setAdult();
                } else {
                    ageable.setBaby();
                }
            }
        }
    }

    void trackFixedPosition(Entity entity) {
        UUID id = entity.getUniqueId();
        stopFixedPosition(id);
        Location fixedLocation = entity.getLocation().clone();
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(ThePit.getInstance(), () -> {
            if (!entity.isValid() || entity.isDead()) {
                stopFixedPosition(id);
                return;
            }
            Location current = entity.getLocation();
            if (!Objects.equals(current.getWorld(), fixedLocation.getWorld())
                    || current.distanceSquared(fixedLocation) > 0.01) entity.teleport(fixedLocation);
            if (entity instanceof Bat bat) bat.setAwake(true);
        }, 1L, 5L);
        fixedPositionTasks.put(id, task);
    }

    private void stopFixedPosition(UUID id) {
        BukkitTask task = fixedPositionTasks.remove(id);
        if (task != null) task.cancel();
    }

    private double getHologramHeight(EntityType entityType) {
        switch (entityType) {
            case IRON_GOLEM:
                return 2.5;
            case SILVERFISH, BAT:
                return 0.6;
            default:
                return 1.9;
        }
    }



    private void maintainNPCs() {
        customEntityNPCs.removeIf(npc -> {
            Entity entity = npc.getEntity();
            if (entity == null || entity.isDead()) {
                if (entity != null) {
                    stopFixedPosition(entity.getUniqueId());
                    entityToNPCMap.remove(entity.getUniqueId());
                    cleanupNPCHolograms(entity.getUniqueId());
                }
                return true;
            }
            return false;
        });
    }
    
    private void updateHolograms() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Map<UUID, List<Hologram>> playerHolograms = playerNPCHolograms.get(player.getUniqueId());
            if (playerHolograms != null) {
                for (UUID npcEntityId : playerHolograms.keySet()) {
                    AbstractCustomEntityNPC npc = entityToNPCMap.get(npcEntityId);
                    if (npc != null && npc.getEntity() != null && !npc.getEntity().isDead()) {
                        updateNPCHologramForPlayer(npc, player);
                    }
                }
            }
        }
    }
    
    private void updateNPCHologramForPlayer(AbstractCustomEntityNPC npc, Player player) {
        List<Hologram> existingHolograms = playerNPCHolograms.get(player.getUniqueId()).get(npc.getEntity().getUniqueId());
        if (existingHolograms == null) return;
        
        List<String> newLines = npc.getNpcTextLine(player);
        if (existingHolograms.size() != newLines.size()) {
            existingHolograms.forEach(hologram -> {
                if (hologram.isSpawned()) {
                    hologram.deSpawn();
                }
            });
            createPersonalHologramForPlayer(npc, player);
        } else {
            for (int i = 0; i < newLines.size(); i++) {
                existingHolograms.get(i).setText(CC.translate(newLines.get(i)));
            }
        }
    }
    
    private void cleanupNPCHolograms(UUID npcEntityId) {
        playerNPCHolograms.values().forEach(playerHolograms -> {
            List<Hologram> holograms = playerHolograms.remove(npcEntityId);
            if (holograms != null) {
                holograms.forEach(hologram -> {
                    if (hologram.isSpawned()) {
                        hologram.deSpawn();
                    }
                });
            }
        });
    }

    /**
     * 只清理带 NPC 标记的实体。
     *
     * 原实现会删除所有已加载区块内的非玩家、非掉落物实体（宠物、盔甲架、事件生物一并被清），
     * 且对未加载区块无效，是重复堆叠的根源之一。
     */
    private void cleanupWorldEntities() {
        try {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Player) {
                        continue;
                    }
                    if (npcTagOf(entity) != null) {
                        entity.remove();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cleanup() {
        fixedPositionTasks.values().forEach(BukkitTask::cancel);
        fixedPositionTasks.clear();
        entityToNPCMap.values().forEach(npc -> {
            if (npc.getEntity() != null && !npc.getEntity().isDead()) {
                npc.getEntity().remove();
            }
        });

        playerNPCHolograms.values().forEach(playerHolograms -> {
            playerHolograms.values().forEach(holograms -> {
                holograms.forEach(hologram -> {
                    if (hologram.isSpawned()) {
                        hologram.deSpawn();
                    }
                });
            });
        });

        entityToNPCMap.clear();
        playerNPCHolograms.clear();
    }


    @EventHandler
    public void onPlayerJoin(PitProfileLoadedEvent event) {
        Player player = Bukkit.getPlayer(event.getPlayerProfile().getPlayerUuid());
        if (player == null || !player.isOnline()) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> {
            showNPCsToPlayer(player);
        }, 20L);
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        AbstractCustomEntityNPC npc = getCustomNPC(entity);

        if (npc != null) {
            event.setCancelled(true);

            CustomEntityNPCInteractEvent interactEvent = new CustomEntityNPCInteractEvent(
                    event.getPlayer(), npc, entity
            );
            Bukkit.getPluginManager().callEvent(interactEvent);

            if (!interactEvent.isCancelled()) {
                npc.handlePlayerInteract(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        AbstractCustomEntityNPC npc = getCustomNPC(entity);

        if (npc != null && !npc.canTakeDamage()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        Entity entity = event.getEntity();
        AbstractCustomEntityNPC npc = getCustomNPC(entity);

        if (npc != null) {

            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cleanupPlayerHolograms(event.getPlayer());
        playerNPCHolograms.remove(event.getPlayer().getUniqueId());
    }
}

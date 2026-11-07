package net.mizukilab.pit.events.impl;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.events.AbstractEvent;
import cn.charlotte.pit.events.trigger.type.INormalEvent;
import net.mizukilab.pit.config.NewConfiguration;
import net.mizukilab.pit.config.PitWorldConfig;
import net.mizukilab.pit.medal.impl.challenge.CakeEventMedal;
import net.mizukilab.pit.util.LocationUtil;
import net.mizukilab.pit.util.aabb.AABB;
import net.mizukilab.pit.util.chat.CC;
import net.mizukilab.pit.util.chat.MessageType;
import net.mizukilab.pit.util.cooldown.Cooldown;
import net.mizukilab.pit.util.random.RandomUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @Author: EmptyIrony
 * @Date: 2021/2/5 23:37
 */
public class CakeEvent extends AbstractEvent implements INormalEvent, Listener {

    private static java.util.List<org.bukkit.Location[]> zones;
    private static final java.util.Map<org.bukkit.Location, org.bukkit.block.data.BlockData> ORIGINAL_BLOCKS = new java.util.HashMap<>();
    private final DecimalFormat numFormatTwo = new DecimalFormat("0.00");
    private final DecimalFormat df = new DecimalFormat(",###,###,###,###");
    private AABB alignedBB;
    private Map<UUID, CakePlayerData> dataCache;

    public CakeEvent() {
        if (zones == null) {
            PitWorldConfig config = ThePit.getInstance().getPitConfig();
            zones = new java.util.ArrayList<>();
            zones.add(new org.bukkit.Location[]{config.getCakeZoneAPosA(), config.getCakeZoneAPosB()});
            zones.add(new org.bukkit.Location[]{config.getCakeZoneBPosA(), config.getCakeZoneBPosB()});
            zones.add(new org.bukkit.Location[]{config.getCakeZoneCPosA(), config.getCakeZoneCPosB()});
            zones.add(new org.bukkit.Location[]{config.getCakeZoneDPosA(), config.getCakeZoneDPosB()});
        }
    }

    @Override
    public String getEventInternalName() {
        return "cake";
    }

    @Override
    public String getEventName() {
        return "&d蛋糕争夺战";
    }

    @Override
    public int requireOnline() {
        return NewConfiguration.INSTANCE.getEventOnlineRequired().get(getEventInternalName());
    }

    @Override
    public void onActive() {
        CC.boardCast(MessageType.EVENT, "&d蛋糕! &7巨型蛋糕已生成在地图上，吃掉蛋糕获得大量金币和经验!");

        this.dataCache = new HashMap<>();

        final org.bukkit.Location[] zone = zones.get(RandomUtil.random.nextInt(zones.size()));
        final int minX = Math.min(zone[0].getBlockX(), zone[1].getBlockX());
        final int minY = Math.min(zone[0].getBlockY(), zone[1].getBlockY());
        final int minZ = Math.min(zone[0].getBlockZ(), zone[1].getBlockZ());
        final int maxX = Math.max(zone[0].getBlockX(), zone[1].getBlockX());
        final int maxY = Math.max(zone[0].getBlockY(), zone[1].getBlockY());
        final int maxZ = Math.max(zone[0].getBlockZ(), zone[1].getBlockZ());
        this.alignedBB = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

        org.bukkit.World bukkitWorld = Bukkit.getWorlds().get(0);
        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
            ORIGINAL_BLOCKS.clear();
            java.util.Random random = new java.util.Random();
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        org.bukkit.block.Block block = bukkitWorld.getBlockAt(x, y, z);
                        Material target;
                        double roll = random.nextDouble();
                        if (roll < 0.94) {
                            target = Material.CAKE;
                        } else if (roll < 0.97) {
                            target = Material.RED_TERRACOTTA;
                        } else {
                            target = Material.BLACK_TERRACOTTA;
                        }
                        if (block.getType() != target) {
                            ORIGINAL_BLOCKS.put(block.getLocation(), block.getBlockData());
                            block.setType(target, false);
                        }
                    }
                }
            }
        });

        Bukkit.getPluginManager()
                .registerEvents(this, ThePit.getInstance());


    }

    @Override
    public void onInactive() {
        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
            for (java.util.Map.Entry<org.bukkit.Location, org.bukkit.block.data.BlockData> entry : ORIGINAL_BLOCKS.entrySet()) {
                entry.getKey().getBlock().setBlockData(entry.getValue(), false);
            }
            ORIGINAL_BLOCKS.clear();
        });

        HandlerList.unregisterAll(this);

        for (UUID uuid : dataCache.keySet()) {
            if (dataCache.get(uuid).coins >= 5000 && Bukkit.getPlayer(uuid) != null) {
                new CakeEventMedal().addProgress(PlayerProfile.getPlayerProfileByUuid(uuid), 1);
            }
        }
    }

    @EventHandler
    public void onEatCake(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        final Block block = event.getClickedBlock();
        final Location location = block.getLocation();
        final AABB blockAABB = new AABB(location.getX() - 1, location.getY() - 1, location.getZ() - 1, location.getX() + 1, location.getY() + 1, location.getZ() + 1);
        final Player player = event.getPlayer();
        final PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());


        if (alignedBB.intersectsWith(blockAABB)) {
            CakePlayerData cakePlayerData = dataCache.get(player.getUniqueId());
            if (cakePlayerData == null) {
                cakePlayerData = new CakePlayerData();
                cakePlayerData.uuid = player.getUniqueId();
                cakePlayerData.name = player.getDisplayName();
                cakePlayerData.cooldown = new Cooldown(100);

                dataCache.put(player.getUniqueId(), cakePlayerData);
            }

            if (!cakePlayerData.cooldown.hasExpired()) {
                return;
            }

            if (!canEat(block.getLocation())) {
                CC.send(MessageType.EVENT, player, "&d&l蛋糕! &c你不能吃里面的蛋糕! 请先吃掉周围的蛋糕!");
                return;
            }

            cakePlayerData.cooldown = new Cooldown(100);

            if (block.getType() == Material.CAKE) {
                int baseCoins = 1;

                if (block.getData() == 6) {
                    baseCoins += 5;
                    block.setType(Material.AIR);
                } else {
                    block.setBlockData(block.getBlockData().getMaterial().createBlockData());
                }
                block.getState().update();

                // final double totalCoins = baseCoins + baseCoins * i * 0.25D;
                final double totalCoins = baseCoins;

                cakePlayerData.clicked++;
                cakePlayerData.coins += totalCoins;

                profile.grindCoins(totalCoins);
                profile.setCoins(profile.getCoins() + totalCoins);

                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1, 1);

                CC.send(MessageType.EVENT, player, "&d&l蛋糕! &6+" + totalCoins + "金币 &d" + cakePlayerData.clicked + "次吃蛋糕" + " &6(" + numFormatTwo.format(cakePlayerData.coins) + "金币在这次事件中)");

            } else if (block.getType() == Material.TERRACOTTA) {
                final byte data = block.getState().getData().getData();
                if (data == 2) {
                    block.setType(Material.AIR);

                    double totalCoins = 20;
                    cakePlayerData.clicked++;
                    cakePlayerData.coins += totalCoins;

                    profile.grindCoins(totalCoins);
                    profile.setCoins(profile.getCoins() + totalCoins);

                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1, 1);

                    CC.send(MessageType.EVENT, player, "&d&l蛋糕! &7吃下了 &c樱桃 &6+20金币&7!");
                } else if (data == 15) {
                    block.setType(Material.AIR);

                    double totalXp = 20;
                    cakePlayerData.clicked++;
                    cakePlayerData.coins += totalXp;

                    profile.setExperience(profile.getExperience() + totalXp);
                    profile.applyExperienceToPlayer(player);

                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1, 1);

                    CC.send(MessageType.EVENT, player, "&d&l蛋糕! &7吃下了 &4巧克力 &b+20经验&7!");
                }
            }
        }

    }

    private boolean canEat(Location location) {
        for (Location face : LocationUtil.getFaces(location)) {
            if (face.getBlock().getType() != Material.CAKE && face.getBlock().getType() != Material.TERRACOTTA) {
                return true;
            }
        }
        return false;
    }

    public static class CakePlayerData {

        private UUID uuid;
        private String name;
        private int coins;
        private int clicked;
        private Cooldown cooldown;
    }
}

package net.mizukilab.pit.events.impl.major;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.PlayerInv;
import cn.charlotte.pit.event.PitKillEvent;
import cn.charlotte.pit.event.PitPlayerSpawnEvent;
import cn.charlotte.pit.events.*;
import cn.charlotte.pit.events.trigger.type.IEpicEvent;
import cn.charlotte.pit.events.trigger.type.addon.IPreparative;
import cn.charlotte.pit.events.trigger.type.addon.IScoreBoardInsert;
import cn.charlotte.pit.events.trigger.type.addon.ISortable;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.BlockPattern;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import cn.charlotte.pit.compat.math.MathHelper;
import net.mizukilab.pit.config.NewConfiguration;
import net.mizukilab.pit.config.PitWorldConfig;
import net.mizukilab.pit.item.type.SpireArmor;
import net.mizukilab.pit.item.type.SpireSword;
import net.mizukilab.pit.item.type.mythic.MythicLeggingsItem;
import net.mizukilab.pit.medal.impl.challenge.SpireFloorMedal;
import net.mizukilab.pit.handlers.PlayerMoveHandler;
import net.mizukilab.pit.util.PlayerUtil;
import net.mizukilab.pit.util.Utils;
import net.mizukilab.pit.util.chat.CC;
import net.mizukilab.pit.util.chat.TitleUtil;
import net.mizukilab.pit.util.cooldown.Cooldown;
import net.mizukilab.pit.util.inventory.InventoryUtil;
import net.mizukilab.pit.util.item.ItemBuilder;
import net.mizukilab.pit.util.random.RandomUtil;
import net.mizukilab.pit.util.time.TimeUtil;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: EmptyIrony
 * @Date: 2021/3/21 11:02
 */
public class SpireEvent extends AbstractEvent implements IEpicEvent, Listener, IPreparative, ISortable, IScoreBoardInsert {
    public static final int FLOOR_COUNT = 9;

    private final Map<UUID, PlayerSpireData> dataMap;
    private final Map<UUID, Integer> rankMap = new HashMap<>();
    private final List<UUID> cooldownList = new ArrayList<>();
    private Cooldown timer;
    private BukkitRunnable runnable;
    private EditSession session;
    private List<Location> floorLocations;
    private World portalWorld;
    private CuboidRegion portalRegion;

    /** 解析 legacy .schematic 读取器，优先按别名查找，找不到时退回内置 MCEdit 格式。 */
    private static ClipboardFormat resolveSchematicFormat() {
        ClipboardFormat format = ClipboardFormats.findByAlias("schematic");
        if (format == null) {
            format = com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat.MCEDIT_SCHEMATIC;
        }
        return format;
    }
    private EditSession teleport;
    private EditSession wall;

    public SpireEvent() {
        this.dataMap = new HashMap<>();
    }

    @Override
    public String getEventInternalName() {
        return "spire";
    }

    @Override
    public String getEventName() {
        return "&5&l尖塔夺魁";
    }

    @Override
    public int requireOnline() {
        return NewConfiguration.INSTANCE.getEventOnlineRequired().get(getEventInternalName());
    }

    @Override
    public void onActive() {
        PitWorldConfig config = ThePit.getInstance().getPitConfig();
        floorLocations = List.copyOf(config.getSpireFloorLoc().subList(0, FLOOR_COUNT));
        Location portalA = config.getPortalPosA();
        World entranceWorld = portalA.getWorld();
        CuboidRegion entrance = new CuboidRegion(BukkitAdapter.asBlockVector(portalA),
                BukkitAdapter.asBlockVector(config.getPortalPosB()));
        portalWorld = entranceWorld;
        portalRegion = entrance;
        dataMap.clear();
        rankMap.clear();
        cooldownList.clear();
        this.timer = new Cooldown(5, TimeUnit.MINUTES);
        this.runnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (timer.hasExpired()) {
                    cancel();
                    if (SpireEvent.this.equals(ThePit.getInstance().getEventFactory().getActiveEpicEvent())) {
                        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                            ThePit.getInstance()
                                    .getEventFactory()
                                    .inactiveEvent(SpireEvent.this);
                        });
                    }
                }
                ThePit.getInstance()
                        .getBossBar()
                        .getBossBar()
                        .setTitle(CC.translate("&5&l大型事件! &6&l" + getEventName() + " &7将在 &a" + TimeUtil.millisToTimer(timer.getRemaining()) + "&7 后结束!"));
                ThePit.getInstance()
                        .getBossBar()
                        .getBossBar()
                        .setProgress(timer.getRemaining() / (1000 * 60 * 5f));
                refreshCooldownPlayer();
            }
        };

        this.runnable.runTaskTimer(ThePit.getInstance(), 20, 20);

        Bukkit.getPluginManager().registerEvents(this, ThePit.getInstance());

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setMaxHealth(player.getMaxHealth());
            player.setHealth(player.getMaxHealth());

            List<Location> spawns = config.getSpawnLocations();
            Location location = spawns.get(RandomUtil.random.nextInt(spawns.size()));
            player.teleport(location);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1, 0.5F);
        }

        EventWorldEdits.build(this, () -> buildArena(config, entranceWorld, entrance));
    }

    private void buildArena(PitWorldConfig config, World entranceWorld, CuboidRegion entrance) {
        Location location = config.getSpireLoc();
        try (InputStream input = ThePit.getInstance().getResource("spire.schematic")) {
            if (input == null) {
                throw new IllegalStateException("Missing spire.schematic");
            }
            Clipboard clipboard;
            try (ClipboardReader reader = resolveSchematicFormat().getReader(input)) {
                clipboard = reader.read();
            }
            session = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld()));
            Operations.complete(new ClipboardHolder(clipboard).createPaste(session)
                    .to(BukkitAdapter.asBlockVector(location)).ignoreAirBlocks(false).build());
            EventWorldEdits.flush(session);

            Location middle = config.getRagePitMiddle();
            wall = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(middle.getWorld()));
            wall.makeCylinder(BukkitAdapter.asBlockVector(middle), new BlockPattern(BlockTypes.GLASS.getDefaultState()),
                    config.getRagePitRadius(), config.getRagePitHeight(), false);
            EventWorldEdits.flush(wall);

            teleport = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(entranceWorld));
            BlockMask airMask = new BlockMask(teleport.getExtent(), BlockTypes.AIR.getDefaultState().toBaseBlock());
            teleport.replaceBlocks(entrance, airMask, new BlockPattern(BlockTypes.END_PORTAL.getDefaultState()));
            EventWorldEdits.flush(teleport);
        } catch (Exception failure) {
            throw new IllegalStateException("Could not build the Spire arena", failure);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onJoinArea(EntityPortalEnterEvent event) {
        if (event.getPortalType() != PortalType.ENDER || !(event.getEntity() instanceof Player player)) {
            return;
        }
        Location portal = event.getLocation();
        if (!portalWorld.equals(portal.getWorld()) || !portalRegion.contains(BukkitAdapter.asBlockVector(portal))) {
            return;
        }

        // Detect portal contact before Minecraft looks for the disabled End dimension.
        event.setCancelled(true);
        runEventTask(() -> joinArea(player));
    }

    private void joinArea(Player player) {
        UUID playerId = player.getUniqueId();
        if (!player.isOnline() || !portalWorld.equals(player.getWorld()) || dataMap.containsKey(playerId)) {
            return;
        }

        PlayerSpireData data = new PlayerSpireData();
        data.uuid = playerId;
        data.name = player.getName();
        data.inArea = true;
        dataMap.put(playerId, data);

        player.closeInventory();
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(playerId);
        profile.setInventory(PlayerInv.fromPlayerInventory(player.getInventory()));
        profile.setTempInvUsing(true);

        PlayerUtil.resetPlayer(player, true, true);
        giveInvSets(player);
        teleportPlayer(player, data);
    }

    @EventHandler
    public void onSpawn(PitPlayerSpawnEvent event) {
        final PlayerSpireData data = dataMap.get(event.getPlayer().getUniqueId());
        if (data != null) {
            dataMap.remove(event.getPlayer().getUniqueId());
        }
        final PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(event.getPlayer().getUniqueId());


        if (profile.isLoaded()) {
            profile.getInventory()
                    .applyItemToPlayer(event.getPlayer());
            profile.setTempInvUsing(false);
        }
    }

    private void giveInvSets(Player player) {
        final PlayerSpireData data = this.dataMap.get(player.getUniqueId());
        final int floor = data.floor;

        if (floor == 0) {
            final SpireSword sword = new SpireSword(Material.WOODEN_SWORD);
            player.getInventory().addItem(sword.toItemStack());
        } else if (floor == 1 || floor == 2) {
            player.getInventory().addItem(SpireArmor.toItemStack(Material.WOODEN_SWORD));
            player.getInventory().setChestplate(SpireArmor.toItemStack(Material.LEATHER_CHESTPLATE));

            player.getInventory().setLeggings(SpireArmor.toItemStack(Material.CHAINMAIL_LEGGINGS));
        } else if (floor == 3 || floor == 4 || floor == 5) {
            player.getInventory().addItem(SpireArmor.toItemStack(Material.IRON_SWORD));

            player.getInventory().setChestplate(SpireArmor.toItemStack(Material.IRON_CHESTPLATE));
            player.getInventory().setLeggings(SpireArmor.toItemStack(Material.CHAINMAIL_LEGGINGS));
            player.getInventory().setBoots(SpireArmor.toItemStack(Material.IRON_BOOTS));
        } else if (floor >= 6) {
            final SpireSword sword = new SpireSword(Material.DIAMOND_SWORD);
            player.getInventory().addItem(sword.toItemStack());
            player.getInventory().setChestplate(SpireArmor.toItemStack(Material.DIAMOND_CHESTPLATE));
            player.getInventory().setLeggings(SpireArmor.toItemStack(Material.IRON_LEGGINGS));
            player.getInventory().setBoots(SpireArmor.toItemStack(Material.DIAMOND_BOOTS));
        }

    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final PlayerSpireData data = dataMap.get(player.getUniqueId());
        if (data == null) {
            return;
        }
        dataMap.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            if (cooldownList.contains(event.getDamager().getUniqueId()) || cooldownList.contains(event.getEntity().getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onKill(PitKillEvent event) {
        try {
            final Player killer = event.getKiller();
            final PlayerSpireData data = dataMap.get(killer.getUniqueId());
            if (data == null) {
                return;
            }

            //floor up start
            if (data.floor == 5) {
                data.reachedSixFloor = true;
            }

            //玩家升级，给予新的物品以及传送至新的一层
            if (data.floor < FLOOR_COUNT - 1) {
                data.lastFloorChange = System.currentTimeMillis();
                data.floor++;
                if (data.floor == FLOOR_COUNT - 1) {
                    new SpireFloorMedal().setProgress(PlayerProfile.getPlayerProfileByUuid(killer.getUniqueId()), 1);
                }
            }
            data.soul = data.soul + this.getKillRewardSouls(data);
            data.kills++;
            TitleUtil.sendTitle(killer, CC.translate("&a击杀!"), this.getFloorColor(data.floor) + "第" + (data.floor + 1) + "层", 5, 20, 10);
            killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.5F);

            runEventTaskLater(() -> {
                int amount = InventoryUtil.getAmountOfItem(killer, "broken_soul");

                this.handleUpdate(killer, data);

                if (amount < 2) {
                    amount++;
                }

                killer.getInventory().addItem(
                        new ItemBuilder(Material.MAGMA_CREAM)
                                .name("&b断裂的灵魂")
                                .lore(
                                        "&7事件物品",
                                        "",
                                        "&7使用后获得效果:",
                                        "&3抗性提升 II (0:03)",
                                        "&74&c❤&7 回复",
                                        "&73&6❤&7 伤害吸收")
                                .internalName("broken_soul")
                                .amount(amount)
                                .removeOnJoin(true)
                                .canDrop(false)
                                .canTrade(false)
                                .canSaveToEnderChest(false)
                                .build()
                );
            }, 1L);
            //floor up end

            LivingEntity target1 = event.getTarget();
            final Player target;
            final PlayerSpireData targetData;
            if (target1 instanceof Player) {
                target = (Player) event.getTarget();
                targetData = dataMap.get(target.getUniqueId());
                if (targetData == null) {
                    return;
                }
            } else {
                return;
            }


            targetData.floor--;
            if (targetData.floor < 0) {
                targetData.floor = 0;
                targetData.lastFloorChange = System.currentTimeMillis();
                targetData.soul = targetData.soul - Math.min(targetData.soul, this.getDeadPunishmentSouls(targetData));

                TitleUtil.sendTitle(target, CC.translate("&c被击杀"), this.getFloorColor(data.floor) + "第" + (data.floor + 1) + "层", 5, 10, 20);
            }
            runEventTaskLater(() -> this.handleUpdate(target, targetData), 2L);

            refreshRank();
        } catch (Exception e) {
            CC.printErrorWithCode(event.getKiller(), e);
        }
    }

    private int getArenaOnline(int floor) {
        int online = 0;
        for (Map.Entry<UUID, PlayerSpireData> entry : this.dataMap.entrySet()) {
            final PlayerSpireData value = entry.getValue();
            if (value.inArea) {
                if (value.floor == floor) {
                    online++;
                }
            }
        }
        return online;
    }

    private void refreshRank() {
        final Map<UUID, Integer> map = new HashMap<>();

        final AtomicInteger rank = new AtomicInteger(this.dataMap.size());
        this.dataMap.values()
                .stream()
                .sorted(Comparator.comparingInt(data -> data.soul))
                .forEach(data -> {
                    map.put(data.uuid, rank.getAndDecrement());
                });

        this.rankMap.clear();
        this.rankMap.putAll(map);
    }

    private void teleportPlayer(Player player, PlayerSpireData data) {
        final Location center = floorLocations.get(data.floor);

        int radius = 32 - data.floor * 3;
        //x取值范围为[-radius,radius],但是因为要平方计算，所以只取正数
        int randomX = RandomUtil.random.nextInt(radius);
        //由x^2 + y^2 = r^2得
        //y^2 = r^2 - x^2
        //开根后得到y值
        double randomZ = MathHelper.sqrt(radius * radius - randomX * randomX);
        //因x,y均为正，所以随机取负值
        if (RandomUtil.hasSuccessfullyByChance(0.5)) {
            randomX = -randomX;
        }
        if (RandomUtil.hasSuccessfullyByChance(0.5)) {
            randomZ = -randomZ;
        }
        //最后，换算成坐标
        final Location finalLocation = new Location(center.getWorld(), center.getX() + randomX, center.getY(), center.getZ() + randomZ);

        player.teleport(finalLocation);
    }

    private void handleUpdate(Player player, PlayerSpireData data) {
        PlayerUtil.resetPlayer(player, true, true);
        this.giveInvSets(player);
        runEventTask(() -> {
            this.teleportPlayer(player, data);

            if (data.floor >= 4) {
                if (this.getArenaOnline(data.floor) >= 2) {
                    this.cooldownList.add(player.getUniqueId());
//                    PlayerMoveHandler.getCantMoveList().add(player);
                }
            }
        });
    }

    private int getKillRewardSouls(PlayerSpireData data) {
        return switch (data.floor) {
            case 4, 5 -> 5;
            case 6 -> 8;
            case 7 -> 10;
            case 8 -> 25;
            default -> 1;
        };
    }

    private int getDeadPunishmentSouls(PlayerSpireData data) {
        return switch (data.floor) {
            case 4, 5 -> -3;
            case 6 -> -5;
            case 7 -> -6;
            case 8 -> -20;
            default -> 0;
        };
    }

    @Override
    public void onInactive() {
        HandlerList.unregisterAll(this);
        if (runnable != null) runnable.cancel();
        EventWorldEdits.undo(() -> {
            // Restore in reverse order, including sessions created before a partial failure.
            for (EditSession edit : new EditSession[]{teleport, wall, session}) {
                if (edit == null) continue;
                try {
                    EventWorldEdits.restore(edit);
                } catch (RuntimeException failure) {
                    ThePit.getInstance().getLogger().log(java.util.logging.Level.SEVERE,
                            "Could not restore part of the Spire arena", failure);
                }
            }
            teleport = null;
            wall = null;
            session = null;
        });

        Runnable restorePlayers = () -> {
            Bukkit.getOnlinePlayers().forEach(player -> {
                final PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
                if (profile.isTempInvUsing()) {
                    profile.getInventory().applyItemToPlayer(player);
                    profile.setTempInvUsing(false);
                }

                Location location = ThePit.getInstance().getPitConfig()
                        .getSpawnLocations()
                        .get(RandomUtil.random.nextInt(ThePit.getInstance().getPitConfig().getSpawnLocations().size()));

                player.teleport(location);
            });
        };
        if (ThePit.getInstance().isEnabled()) {
            Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), restorePlayers, 1L);
        } else {
            restorePlayers.run();
        }

        refreshCooldownPlayer();
        if (isAborting()) return;

        CC.boardCast(CC.CHAT_BAR);
        CC.boardCast("&6&l天坑事件结束: " + this.getEventName() + "&6&l!");

        for (Player player : Bukkit.getOnlinePlayers()) {
            final PlayerSpireData data = dataMap.get(player.getUniqueId());

            final int rank = rankMap.containsKey(player.getUniqueId()) ? (rankMap.get(player.getUniqueId()) + 1) : -1;
            final PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());

            double rewardCoins = 0;
            int rewardRenown = 0;
            if (rank <= 3) {
                rewardCoins += 2000;
                rewardRenown += 2;
            } else if (rank <= 20) {
                rewardCoins += 500;
                rewardRenown += 1;
            } else {
                rewardCoins += 100;
            }
            if (data != null && data.reachedSixFloor) {
                rewardRenown++;
            }
            if (ThePit.getInstance().getPitConfig().isGenesisEnable() && profile.getGenesisData().getTier() >= 5 && rewardRenown > 0) {
                rewardRenown++;
            }
            int enchantBoostLevel = Utils.getEnchantLevel(player.getInventory().getLeggings(), "Paparazzi");
            if (PlayerUtil.isVenom(player) || PlayerUtil.isEquippingSomber(player)) {
                enchantBoostLevel = 0;
            }
            if (enchantBoostLevel > 0) {
                rewardCoins += 0.5 * enchantBoostLevel * rewardCoins;
                rewardRenown += MathHelper.floor(0.5 * enchantBoostLevel * rewardRenown);
                MythicLeggingsItem mythicLeggings = new MythicLeggingsItem();
                mythicLeggings.loadFromItemStack(player.getInventory().getLeggings());
                if (mythicLeggings.isEnchanted()) {
                    if (mythicLeggings.getMaxLive() > 0 && mythicLeggings.getLive() <= 2) {
                        player.getInventory().setLeggings(new ItemStack(Material.AIR));
                    } else {
                        mythicLeggings.setLive(mythicLeggings.getLive() - 2);
                        player.getInventory().setLeggings(mythicLeggings.toItemStack());
                    }
                }
            }
            if (PlayerUtil.isPlayerUnlockedPerk(player, "self_confidence")) {
                if (rank <= 5) {
                    rewardCoins += 5000;
                } else if (rank <= 10) {
                    rewardCoins += 2500;
                } else if (rank <= 15) {
                    rewardCoins += 1000;
                }
            }
            if (data == null) {
                rewardCoins = 100;
                rewardRenown = 0;
            }
            profile.grindCoins(rewardCoins);
            profile.setCoins(profile.getCoins() + rewardCoins);
            profile.setRenown(profile.getRenown() + rewardRenown);

            profile.kingsQuestsData.checkUpdate();
            if (profile.kingsQuestsData.getAccepted()) {
                if (!profile.kingsQuestsData.getCompleted()) {
                    profile.kingsQuestsData.setCollectedRenown(profile.kingsQuestsData.getCollectedRenown() + rewardRenown);
                }
            }

            player.sendMessage(CC.translate("&6你的奖励: &6+" + rewardCoins + "硬币 &e+" + rewardRenown + "声望"));
            if (rank > 0) {
                player.sendMessage(CC.translate("&6&l你: &7收集了总计 &b" + (data == null ? 0 : data.soul) + " &7的灵魂 (排名#" + rank + ")"));
            }
            if (data != null && data.reachedSixFloor) {
                player.sendMessage(CC.translate("&6&l额外奖励: &a&l成功! &7最高进入过第六层或更高!"));
            } else {
                player.sendMessage(CC.translate("&6&l额外奖励: &c&l失败! &7你没能进入过第六层!"));
            }
            if (rankMap.size() >= 3) {
                Map<Integer, Player> rankPlayerMap = new HashMap<>();
                for (Map.Entry<UUID, Integer> entry : rankMap.entrySet()) {
                    if (entry.getValue() <= 3) {
                        rankPlayerMap.put(entry.getValue(), Bukkit.getPlayer(entry.getKey()));
                    }
                }

                player.sendMessage(CC.translate("&6顶级玩家: "));
                for (int i = 1; i < 4; i++) {
                    final Player top = rankPlayerMap.get(i);
                    if (top != null && top.isOnline()) {
                        final PlayerSpireData spireData = dataMap.get(top.getUniqueId());
                        PlayerProfile topProfile = PlayerProfile.getPlayerProfileByUuid(top.getUniqueId());

                        player.sendMessage(CC.translate(" &e&l#" + (i) + " " + topProfile.getFormattedName() + " &7收集了 &b" + spireData.soul + "&b灵魂"));
                    }
                }
            }
        }
        CC.boardCast(CC.CHAT_BAR);
    }

    private void refreshCooldownPlayer() {
        final ArrayList<UUID> list = new ArrayList<>(cooldownList);
        final long now = System.currentTimeMillis();
        for (UUID uuid : list) {
            final Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                final PlayerSpireData data = dataMap.get(uuid);
                if (data != null) {
                    if (now - data.lastFloorChange >= 3 * 1000) {
                        cooldownList.remove(player.getUniqueId());
                        PlayerMoveHandler.getCantMoveList().remove(player);
                    } else {
                        //fixme: player still can move on here
                        final long lastTime = (3 * 1000 - (now - data.lastFloorChange)) / 1000;
                        TitleUtil.sendTitle(player, CC.translate("&a获得击杀"), this.getFloorColor(data.floor) + "第" + (data.floor + 1) + CC.translate("层&e(" + lastTime + ")"), 5, 20, 5);
                    }
                } else {
                    cooldownList.remove(player.getUniqueId());
                    PlayerMoveHandler.getCantMoveList().remove(player);
                }
            } else {
                cooldownList.remove(uuid);
                if (player != null) {
                    PlayerMoveHandler.getCantMoveList().remove(player);
                }
            }
        }
    }

    @Override
    public void onPreActive() {

    }

    public String getDisplayFloor(UUID uuid) {
        final PlayerSpireData data = this.dataMap.get(uuid);
        if (data == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(CC.translate("&" + this.getFloorColor(data.floor).getChar()));

        switch (data.floor) {
            case 0: {
                builder.append("壹");
                break;
            }
            case 1: {
                builder.append("贰");
                break;
            }
            case 2: {
                builder.append("叁");
                break;
            }
            case 3: {
                builder.append("肆");
                break;
            }
            case 4: {
                builder.append("伍");
                break;
            }
            case 5: {
                builder.append("陆");
                break;
            }
            case 6: {
                builder.append("柒");
                break;
            }
            case 7: {
                builder.append("捌");
                break;
            }
            case 8: {
                builder.append("玖");
                break;
            }
        }

        return builder.toString();
    }

    private ChatColor getFloorColor(int floor) {
        //todo: 根据地图颜色适配chat color;
        switch (floor) {

            case 0:
                return ChatColor.RED;
            case 1:
                return ChatColor.YELLOW;
            case 2:
                return ChatColor.GREEN;
            case 3:
                return ChatColor.AQUA;
            case 4:
                return ChatColor.BLUE;
            case 5:
                return ChatColor.DARK_PURPLE;
            case 6:
                return ChatColor.LIGHT_PURPLE;
            case 7:
                return ChatColor.WHITE;
            case 8:
                return ChatColor.GRAY;
            default: {
                return ChatColor.WHITE;
            }
        }
    }

    @Override
    public int getRank(Player player) {
        return this.rankMap.getOrDefault(player.getUniqueId(), -1);
    }

    @Override
    public List<String> insert(Player player) {
        List<String> line = new ArrayList<>();
        line.add("&f剩余时间: &a" + TimeUtil.millisToTimer(this.timer.getRemaining()));
        final PlayerSpireData data = this.dataMap.get(player.getUniqueId());
        if (data == null) {
            line.add("&f灵魂: &b0");
            return line;
        }


        line.add("&f楼层: " + this.getDisplayFloor(player.getUniqueId()));
        line.add("&f灵魂: &b" + data.soul + (this.getRank(player) > 0 ? " &7[&e#" + this.getRank(player) + "&7]" : ""));

        return line;
    }

    public static class PlayerSpireData {

        private UUID uuid;
        private String name;
        private int kills;
        private int soul;
        private long lastFloorChange;

        private boolean reachedSixFloor;

        private int floor;
        private boolean inArea;
    }
}

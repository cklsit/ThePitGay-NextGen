package cn.charlotte.pit.bot;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedEnumEntityUseAction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 假人（Bot）管理器。
 * <p>
 * Bot 以玩家模型呈现：渲染由 {@link PlayerBotRenderer} 通过玩家信息包 + 实体生成包完成，
 * 因此对客户端而言与正常玩家一致（Tab 列表、皮肤、模型、头部朝向）。
 */
public final class BotManager {

    private static final Pattern UUID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F]{32})\"");
    private static final Pattern VALUE_PATTERN = Pattern.compile("\"value\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern SIGNATURE_PATTERN = Pattern.compile("\"signature\"\\s*:\\s*\"([^\"]+)\"");

    private final ThePit plugin;
    private final Map<String, PlayerBotRenderer> bots = new LinkedHashMap<>();
    private final File storageFile;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public BotManager(ThePit plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "bots.yml");
        load();
        new BotCombatListener().start();
    }

    public Collection<PlayerBotRenderer> getBots() {
        return bots.values();
    }

    public PlayerBotRenderer getBot(String name) {
        return bots.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean exists(String name) {
        return bots.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public PlayerBotRenderer add(String name, Location location, BotSkin skin) {
        PlayerBotRenderer bot = new PlayerBotRenderer(name, skin, location);
        bots.put(name.toLowerCase(Locale.ROOT), bot);
        bot.spawnAll();
        save();
        return bot;
    }

    public boolean remove(String name) {
        PlayerBotRenderer bot = bots.remove(name.toLowerCase(Locale.ROOT));
        if (bot == null) {
            return false;
        }
        bot.despawnAll();
        save();
        return true;
    }

    public void setSkin(String name, BotSkin skin) {
        PlayerBotRenderer bot = getBot(name);
        if (bot != null) {
            bot.setSkin(skin);
            save();
        }
    }

    /**
     * 玩家进服后补发一次，避免登录瞬间丢包导致看不到 Bot。
     */
    public void respawnFor(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (PlayerBotRenderer bot : bots.values()) {
                bot.spawnFor(player);
            }
        }, 20L);
    }

    public void despawnAll() {
        for (PlayerBotRenderer bot : bots.values()) {
            bot.despawnAll();
        }
    }

    /**
     * 皮肤参数支持：base64 纹理值 / 纹理 URL / 正版玩家名。
     * 玩家名需要异步请求 Mojang，回调在主线程执行。
     */
    public void resolveSkin(String name, String input, java.util.function.Consumer<BotSkin> callback) {
        BotSkin inline = BotSkin.parseInline(input);
        if (inline != null) {
            callback.accept(inline);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            BotSkin resolved = fetchByPlayerName(input);
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(resolved));
        });
    }

    private BotSkin fetchByPlayerName(String playerName) {
        try {
            HttpRequest uuidRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + playerName))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> uuidResponse = httpClient.send(uuidRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (uuidResponse.statusCode() != 200) {
                return null;
            }
            Matcher uuidMatcher = UUID_PATTERN.matcher(uuidResponse.body());
            if (!uuidMatcher.find()) {
                return null;
            }
            String id = uuidMatcher.group(1);
            HttpRequest profileRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + id + "?unsigned=false"))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> profileResponse = httpClient.send(profileRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (profileResponse.statusCode() != 200) {
                return null;
            }
            String body = profileResponse.body();
            Matcher valueMatcher = VALUE_PATTERN.matcher(body);
            if (!valueMatcher.find()) {
                return null;
            }
            Matcher signatureMatcher = SIGNATURE_PATTERN.matcher(body);
            String signature = signatureMatcher.find() ? signatureMatcher.group(1) : null;
            return new BotSkin(valueMatcher.group(1), signature);
        } catch (Exception exception) {
            return null;
        }
    }

    private void load() {
        if (!storageFile.exists()) {
            return;
        }
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(storageFile);
        ConfigurationSection section = configuration.getConfigurationSection("bots");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }
            String world = entry.getString("world", "world");
            if (Bukkit.getWorld(world) == null) {
                continue;
            }
            Location location = new Location(Bukkit.getWorld(world),
                    entry.getDouble("x"), entry.getDouble("y"), entry.getDouble("z"),
                    (float) entry.getDouble("yaw"), (float) entry.getDouble("pitch"));
            BotSkin skin = entry.getString("skin-value") == null
                    ? BotSkin.DEFAULT
                    : new BotSkin(entry.getString("skin-value"), entry.getString("skin-signature"));
            bots.put(key.toLowerCase(Locale.ROOT), new PlayerBotRenderer(key, skin, location));
        }
    }

    public void save() {
        YamlConfiguration configuration = new YamlConfiguration();
        for (PlayerBotRenderer bot : bots.values()) {
            Location location = bot.getLocation();
            String path = "bots." + bot.getName();
            configuration.set(path + ".world", location.getWorld() == null ? "world" : location.getWorld().getName());
            configuration.set(path + ".x", location.getX());
            configuration.set(path + ".y", location.getY());
            configuration.set(path + ".z", location.getZ());
            configuration.set(path + ".yaw", location.getYaw());
            configuration.set(path + ".pitch", location.getPitch());
            configuration.set(path + ".skin-value", bot.getSkin().getValue());
            configuration.set(path + ".skin-signature", bot.getSkin().getSignature());
        }
        try {
            if (!storageFile.getParentFile().exists() && !storageFile.getParentFile().mkdirs()) {
                return;
            }
            configuration.save(storageFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("保存 bots.yml 失败: " + exception.getMessage());
        }
    }

    public void spawnAll() {
        for (PlayerBotRenderer bot : bots.values()) {
            bot.spawnAll();
        }
    }

    public PlayerBotRenderer findBotByEntityId(int entityId) {
        for (PlayerBotRenderer bot : bots.values()) {
            if (bot.getEntityId() == entityId) {
                return bot;
            }
        }
        return null;
    }

    /**
     * Bot 战斗监听：Bot 是给玩家刷金币/经验的沙袋。
     * <p>
     * Bot 是假玩家（无真实实体），攻击只能通过拦截 {@code USE_ENTITY} 包判定。
     */
    private final class BotCombatListener {

        private static final double COINS_PER_HIT = 2.0;
        private static final double XP_PER_HIT = 5.0;

        public void start() {
            plugin.getLogger().info("[BotDebug] BotCombatListener 注册 USE_ENTITY 监听");
            ProtocolLibrary.getProtocolManager().addPacketListener(
                    new PacketAdapter(plugin, PacketType.Play.Client.USE_ENTITY) {
                        @Override
                        public void onPacketReceiving(PacketEvent event) {
                            if (event.getPacketType() != PacketType.Play.Client.USE_ENTITY) {
                                return;
                            }
                            int entityId = event.getPacket().getIntegers().read(0);
                            WrappedEnumEntityUseAction action = event.getPacket().getEnumEntityUseActions().read(0);
                            String actionName = action == null ? "null" : action.getAction().name();
                            if (!"ATTACK".equals(actionName)) {
                                return;
                            }
                            plugin.getLogger().info("[BotDebug] 收到攻击包 entity=" + entityId
                                    + " 攻击者=" + event.getPlayer().getName());
                            handleAttack(event.getPlayer(), entityId);
                        }
                    });
        }

        private void handleAttack(Player player, int entityId) {
            PlayerBotRenderer bot = findBotByEntityId(entityId);
            if (bot == null) {
                plugin.getLogger().info("[BotDebug] entityId " + entityId + " 未匹配到任何 bot");
                return;
            }
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            if (profile != null) {
                profile.setCoins(profile.getCoins() + COINS_PER_HIT);
                profile.grindCoins(COINS_PER_HIT);
                profile.setExperience(profile.getExperience() + XP_PER_HIT);
            }
            double damage = 1.0;
            if (player.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
                damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue();
            }
            boolean dead = bot.damage(damage);
            plugin.getLogger().info("[BotDebug] 命中 bot=" + bot.getName() + " 伤害=" + damage
                    + " 剩余血量=" + bot.getHealth() + " 死亡=" + dead);
            if (dead) {
                plugin.getLogger().info("[BotDebug] bot 已击杀，0.1s 后原地重生");
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    bot.respawn();
                    plugin.getLogger().info("[BotDebug] bot 已重生");
                }, 2L);
            }
        }
    }

    /**
     * /bot {type} {name} [x] [y] [z]
     */
    public static final class Command implements CommandExecutor, TabCompleter {

        private final BotManager manager;

        public Command(BotManager manager) {
            this.manager = manager;
        }

        @Override
        public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
            if (!sender.hasPermission("thepit.bot")) {
                sender.sendMessage("§c无权限。");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§e用法: /bot {add|remove|skin} {name} [x] [y] [z]");
                return true;
            }
            String type = args[0].toLowerCase(Locale.ROOT);
            String name = args[1];
            switch (type) {
                case "add" -> handleAdd(sender, name, args);
                case "remove" -> {
                    if (!manager.remove(name)) {
                        sender.sendMessage("§c不存在名为 " + name + " 的 bot。");
                        return true;
                    }
                    sender.sendMessage("§a已移除 bot §f" + name);
                }
                case "skin" -> handleSkin(sender, name, args);
                default -> sender.sendMessage("§e用法: /bot {add|remove|skin} {name} [x] [y] [z]");
            }
            return true;
        }

        private void handleAdd(CommandSender sender, String name, String[] args) {
            if (manager.exists(name)) {
                sender.sendMessage("§c已存在名为 " + name + " 的 bot。");
                return;
            }
            Location location;
            if (args.length >= 5) {
                try {
                    location = new Location(((Player) sender).getWorld(),
                            Double.parseDouble(args[2]), Double.parseDouble(args[3]), Double.parseDouble(args[4]));
                } catch (NumberFormatException exception) {
                    sender.sendMessage("§c坐标必须是数字。");
                    return;
                }
            } else if (sender instanceof Player player) {
                location = player.getLocation();
            } else {
                sender.sendMessage("§c控制台执行时必须提供 x y z。");
                return;
            }
            manager.add(name, location, BotSkin.DEFAULT);
            sender.sendMessage("§a已添加 bot §f" + name + " §7(默认 Steve 皮肤，可用 /bot skin " + name + " <玩家名|纹理> 修改)");
        }

        private void handleSkin(CommandSender sender, String name, String[] args) {
            if (!manager.exists(name)) {
                sender.sendMessage("§c不存在名为 " + name + " 的 bot。");
                return;
            }
            if (args.length < 3) {
                sender.sendMessage("§e用法: /bot skin " + name + " <玩家名|纹理base64|纹理URL>");
                return;
            }
            String input = args[2];
            manager.resolveSkin(name, input, skin -> {
                if (skin == null) {
                    sender.sendMessage("§c无法解析皮肤: " + input);
                    return;
                }
                manager.setSkin(name, skin);
                sender.sendMessage("§a已更新 bot §f" + name + " §a的皮肤。");
            });
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
            if (args.length == 1) {
                return filter(List.of("add", "remove", "skin"), args[0]);
            }
            if (args.length == 2) {
                return filter(new ArrayList<>(List.of("1", "2", "3")), args[1]);
            }
            return List.of();
        }

        private List<String> filter(List<String> candidates, String prefix) {
            List<String> result = new ArrayList<>();
            for (String candidate : candidates) {
                if (candidate.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
                    result.add(candidate);
                }
            }
            return result;
        }
    }

    public static UUID offlineUuid(String name) {
        return BotSkin.offlineUuid(name);
    }
}

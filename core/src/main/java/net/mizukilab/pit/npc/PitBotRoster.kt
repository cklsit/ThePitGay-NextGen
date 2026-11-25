package net.mizukilab.pit.npc

import cn.charlotte.pit.ThePit
import cn.charlotte.pit.data.PlayerProfile
import cn.mythicland.npclib.bot.FighterRobot
import cn.mythicland.npclib.bot.FighterRobotOptions.RosterBehavior
import net.mizukilab.pit.util.chat.CC
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import java.io.File
import java.io.InputStreamReader
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.logging.Level

class PitBotRoster(private val plugin: ThePit) : AutoCloseable {
    private val file = File(plugin.dataFolder, "bots.yml")
    private var config: PitBotConfiguration? = null
    private val active = linkedMapOf<String, ActiveBot>()
    private val temporary = linkedMapOf<String, FighterRobot>()
    private val forced = mutableSetOf<String>()
    private val suspended = mutableSetOf<String>()
    private val exhausted = mutableSetOf<String>()
    private val retryAt = mutableMapOf<String, Long>()
    private var running = true
    private var tick = 0L
    private var manualId = 0
    private var task: BukkitTask? = null

    private data class ActiveBot(val robot: FighterRobot, var nearbyAt: Long)

    fun start() {
        try {
            reload()
        } catch (failure: Exception) {
            plugin.logger.log(Level.WARNING, "[Bot] 加载 bots.yml 失败，可修正后执行 /bot reload", failure)
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable(::tickRoster), 1L, UPDATE_TICKS)
    }

    fun reload(): Int {
        val existing = YamlConfiguration()
        if (file.isFile) existing.load(file)
        if (file.isFile && existing.contains("version")) return apply(PitBotConfiguration(existing))

        val defaults = YamlConfiguration()
        val resource = checkNotNull(plugin.getResource("bots.yml")) { "插件缺少内置 bots.yml" }
        InputStreamReader(resource, Charsets.UTF_8).use(defaults::load)
        if (file.isFile) {
            val oldPoints = existing.getConfigurationSection("bots") ?: error("无法识别现有的 bots.yml 格式")
            require(existing.getKeys(false) == setOf("bots")) { "旧 bots.yml 有无法迁移的设置" }
            for (id in oldPoints.getKeys(false)) {
                val values = oldPoints.getConfigurationSection(id)?.getValues(false) ?: error("无效的旧出生点 $id")
                defaults.createSection("spawn-points.$id", values)
            }
            defaults.setComments("spawn-points", listOf("旧 /botspawn 命令保留的临时 Bot 出生点。"))
        }
        val loaded = PitBotConfiguration(defaults)
        save(defaults)
        plugin.logger.info("[Bot] 已载入旧服 Bot 名单，共 ${loaded.definitions.size} 个配置。")
        return apply(loaded)
    }

    private fun apply(next: PitBotConfiguration): Int {
        val old = config
        config = next
        for (id in active.keys.toList()) {
            if (old?.definitions?.get(id) != next.definitions[id] || old?.aliases != next.aliases) deactivate(id)
        }
        forced.retainAll(next.definitions.keys)
        suspended.retainAll(next.definitions.keys)
        exhausted.removeIf { old?.definitions?.get(it) != next.definitions[it] }
        retryAt.clear()
        return next.definitions.size
    }

    internal fun update(id: String, edit: (YamlConfiguration, String) -> Unit) {
        require(PitBotConfiguration.validId(id)) { "无效的 Bot ID" }
        val edited = YamlConfiguration()
        edited.load(file)
        edit(edited, "bots.$id")
        val next = PitBotConfiguration(edited)
        save(edited)
        apply(next)
    }

    private fun save(yaml: YamlConfiguration) {
        Files.createDirectories(file.toPath().parent)
        val temp = Files.createTempFile(file.toPath().parent, "bots-", ".yml.tmp")
        try {
            yaml.save(temp.toFile())
            try {
                Files.move(temp, file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temp, file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    internal fun definitions(): Collection<PitBotDefinition> = config?.definitions?.values.orEmpty()

    internal fun definition(id: String): PitBotDefinition = config?.definitions?.get(id) ?: error("找不到 Bot $id")

    internal fun temporaryIds(): Set<String> = temporary.keys

    internal fun status(id: String): String {
        val handle = active[id]?.robot ?: temporary[id]
        if (handle != null && handle.entity().bukkitEntity.isValid) return "存活"
        if (id in exhausted) return "已死亡"
        if (handle != null) return "等待重生"
        if (config?.definitions?.get(id)?.enabled == false) return "已禁用"
        if (!running || id in suspended) return "已暂停"
        if (id in retryAt) return "生成失败"
        return "休眠"
    }

    internal fun resume() {
        running = true
        suspended.clear()
        exhausted.clear()
    }

    internal fun spawn(id: String) {
        val definition = definition(id)
        require(definition.enabled) { "Bot $id 已禁用，先执行 /bot enable $id" }
        if (id !in active) activate(definition)
        suspended.remove(id)
        exhausted.remove(id)
        retryAt.remove(id)
        forced.add(id)
    }

    internal fun despawn(id: String) {
        temporary.remove(id)?.let { it.close(); return }
        definition(id)
        forced.remove(id)
        suspended.add(id)
        deactivate(id)
    }

    internal fun clear(): Int {
        running = false
        val count = active.size + temporary.size
        for (id in active.keys.toList()) deactivate(id)
        for (robot in temporary.values) robot.close()
        temporary.clear()
        forced.clear()
        return count
    }

    internal fun temporarySpawn(amount: Int, fallback: Location?): Int {
        val settings = checkNotNull(config) { "请先加载 bots.yml" }
        val positions = settings.spawnPoints.mapNotNull { it.resolve(settings.aliases) }
        val rosterPositions = if (positions.isEmpty()) {
            settings.definitions.values.filter { it.enabled }.mapNotNull { it.position.resolve(settings.aliases) }
        } else positions
        var count = 0
        repeat(amount) { index ->
            val location = positions.getOrNull(index) ?: fallback ?: rosterPositions.getOrNull(index) ?: return@repeat
            val id = "manual-${++manualId}"
            temporary[id] = plugin.npcFactory.npcLib.createFighterRobot(safeLocation(location)) { options ->
                options.name("Bot-$manualId")
                options.respawnDelay(200)
            }
            count++
        }
        return count
    }

    private fun tickRoster() {
        tick += UPDATE_TICKS
        val settings = config ?: return
        val viewers = Bukkit.getOnlinePlayers().filterNot(NpcIdentity::isNpc).map(Player::getLocation)
        var attempts = 0
        for ((id, definition) in settings.definitions) {
            if (!definition.enabled || id in suspended || id in exhausted) {
                deactivate(id)
                continue
            }
            val spawn = definition.position.resolve(settings.aliases)
            if (spawn == null) {
                deactivate(id)
                continue
            }
            val live = active[id]
            val location = live?.robot?.entity()?.bukkitEntity?.takeIf { it.isValid }?.location ?: spawn
            val nearby = !settings.spawnNearPlayers || viewers.any {
                it.world == location.world && it.distanceSquared(location) <= ACTIVATION_DISTANCE * ACTIVATION_DISTANCE
            }
            val manuallySpawned = id in forced
            if (live != null) {
                if (nearby) live.nearbyAt = tick
                if (!manuallySpawned && (!running || !settings.autoSpawn || tick - live.nearbyAt >= IDLE_TICKS)) deactivate(id)
                continue
            }
            if (attempts >= SPAWNS_PER_UPDATE || tick < (retryAt[id] ?: 0)) continue
            if (manuallySpawned || (running && settings.autoSpawn && nearby)) {
                attempts++
                try {
                    activate(definition)
                    retryAt.remove(id)
                } catch (failure: RuntimeException) {
                    retryAt[id] = tick + RETRY_TICKS
                    plugin.logger.log(Level.WARNING, "[Bot] 无法生成 $id", failure)
                }
            }
        }
    }

    private fun activate(definition: PitBotDefinition) {
        val aliases = checkNotNull(config).aliases
        val position = definition.position.resolve(aliases) ?: error("世界 ${definition.position.world} 未加载")
        val spawn = safeLocation(position)
        val waypoints = definition.waypoints.map { point ->
            val location = point.resolve(aliases) ?: error("路线世界 ${point.world} 未加载")
            require(location.world == spawn.world && validHeight(location)) { "路线点超出 Bot 所在世界" }
            location
        }
        val robot = plugin.npcFactory.npcLib.createFighterRobot(spawn) { options ->
            options.name(definition.id)
            options.skin(definition.skin)
            options.radius(definition.radius)
            options.movementSpeed(definition.speed)
            if (definition.respawnTicks >= 0) options.respawnDelay(definition.respawnTicks)
            else options.onDeath { exhausted.add(definition.id) }
            options.targetFilter { player -> !NpcIdentity.isNpc(player) &&
                PlayerProfile.getPlayerProfileByUuid(player.uniqueId).isInArena }
            options.rosterBehavior(RosterBehavior(waypoints, definition.mode, definition.reach, definition.attackTicks,
                CC.translate(definition.name), definition.showNameplate) { player -> preparePlayer(player, definition) })
        }
        active[definition.id] = ActiveBot(robot, tick)
    }

    private fun preparePlayer(player: Player, definition: PitBotDefinition) {
        player.gameMode = GameMode.SURVIVAL
        player.getAttribute(Attribute.MAX_HEALTH)?.baseValue = definition.health
        player.health = definition.health
        for ((slot, material) in definition.inventory) {
            val item = ItemStack(material)
            if (material.maxDurability > 0) item.editMeta { it.isUnbreakable = true }
            player.inventory.setItem(slot, item)
        }
    }

    private fun deactivate(id: String) {
        active.remove(id)?.robot?.close()
    }

    private fun validHeight(location: Location): Boolean =
        location.y >= location.world.minHeight && location.y + 1 < location.world.maxHeight

    private fun safeLocation(original: Location): Location {
        val location = original.clone()
        require(validHeight(location)) { "出生点高度 ${location.y} 超出世界范围" }
        val block = location.block
        if (!block.isPassable) location.add(0.0, 1.0, 0.0)
        require(validHeight(location) && location.block.isPassable &&
            location.clone().add(0.0, 1.0, 0.0).block.isPassable) { "出生点被方块占用" }
        return location
    }

    override fun close() {
        task?.cancel()
        clear()
    }

    private companion object {
        const val UPDATE_TICKS = 5L
        const val ACTIVATION_DISTANCE = 40.0
        const val IDLE_TICKS = 600L
        const val RETRY_TICKS = 600L
        const val SPAWNS_PER_UPDATE = 2
    }
}

package net.mizukilab.pit.npc

import cn.mythicland.npclib.api.skin.Skin
import cn.mythicland.npclib.bot.FighterRobotOptions.CombatMode
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.util.Locale

internal data class PitBotPosition(val world: String, val x: Double, val y: Double, val z: Double,
                                   val yaw: Float = 0f, val pitch: Float = 0f) {
    fun resolve(aliases: Map<String, String>): Location? {
        val resolved = aliases[world]?.let(Bukkit::getWorld) ?: Bukkit.getWorld(world) ?: return null
        return Location(resolved, x, y, z, yaw, pitch)
    }

    companion object {
        fun read(values: Map<*, *>, fallbackWorld: String? = null): PitBotPosition {
            val world = values["world"] as? String ?: fallbackWorld
            require(!world.isNullOrBlank()) { "缺少世界名称" }
            fun number(key: String, default: Double? = null): Double {
                val value = values[key] ?: default
                return (value as? Number)?.toDouble()?.takeIf { it.isFinite() }
                    ?: throw IllegalArgumentException("$key 必须为数字")
            }
            val x = number("x")
            val z = number("z")
            require(kotlin.math.abs(x) < 30_000_000 && kotlin.math.abs(z) < 30_000_000) { "坐标超出世界边界" }
            val yaw = number("yaw", 0.0).toFloat()
            val pitch = number("pitch", 0.0).toFloat()
            require(yaw.isFinite() && pitch in -90f..90f) { "朝向无效" }
            return PitBotPosition(world, x, number("y"), z, yaw, pitch)
        }
    }
}

internal data class PitBotDefinition(
    val id: String,
    val enabled: Boolean,
    val position: PitBotPosition,
    val waypoints: List<PitBotPosition>,
    val skin: Skin?,
    val inventory: Map<Int, Material>,
    val name: String,
    val showNameplate: Boolean,
    val mode: CombatMode,
    val respawnTicks: Int,
    val speed: Double,
    val radius: Double,
    val health: Double,
    val reach: Double,
    val attackTicks: Int
)

internal class PitBotConfiguration(yaml: YamlConfiguration) {
    val autoSpawn = yaml.getBoolean("auto-spawn", true)
    val spawnNearPlayers = yaml.getBoolean("spawn-near-players", true)
    val aliases = yaml.getConfigurationSection("world-aliases")?.let { section ->
        section.getKeys(false).associateWith { key -> requiredString(section, key) }
    }.orEmpty()
    val spawnPoints = yaml.getConfigurationSection("spawn-points")?.children()?.map { (_, section) ->
        PitBotPosition.read(section.getValues(false))
    }.orEmpty()
    val definitions: Map<String, PitBotDefinition>

    init {
        require(yaml.getInt("version") == 1) { "不支持的 bots.yml 配置版本" }
        val defaults = yaml.getConfigurationSection("defaults")
        val skins = yaml.getConfigurationSection("skins")?.children()?.mapValues { (_, section) ->
            Skin(requiredString(section, "value"), requiredString(section, "signature"))
        }.orEmpty()
        val routes = yaml.getConfigurationSection("routes")?.children()?.mapValues { (_, section) ->
            val world = section.getString("world")
            val points = section.getList("points") ?: throw IllegalArgumentException("缺少路线点 ${section.name}")
            val ordered = points.map { PitBotPosition.read(it as? Map<*, *> ?: error("无效的路线点"), world) }
            if (section.getBoolean("reverse-at-end") && ordered.size > 2) {
                ordered + ordered.subList(1, ordered.lastIndex).asReversed()
            } else ordered
        }.orEmpty()
        val roster = yaml.getConfigurationSection("bots") ?: error("缺少 bots 配置节")
        definitions = roster.children().mapValues { (id, section) ->
            try {
                require(validId(id)) { "ID 只可包含小写字母、数字、下划线或连字符" }
                fun value(key: String, fallback: Any): Any = section.get(key) ?: defaults?.get(key) ?: fallback
                fun number(key: String, fallback: Number, range: ClosedFloatingPointRange<Double>): Double {
                    val result = (value(key, fallback) as? Number)?.toDouble()
                    require(result != null && result in range) { "$key 超出允许范围 $range" }
                    return result
                }
                fun ticks(key: String, fallback: Int, minimum: Int): Int {
                    val result = number(key, fallback, minimum.toDouble()..Int.MAX_VALUE.toDouble())
                    require(result.toInt().toDouble() == result) { "$key 必须为整数" }
                    return result.toInt()
                }
                val position = PitBotPosition.read(section.getValues(false))
                val route = section.getString("route")?.let { routes[it] ?: error("找不到路线 $it") }.orEmpty()
                val waypoints = section.getList("waypoints")?.map {
                    PitBotPosition.read(it as? Map<*, *> ?: error("无效的路线点"), position.world)
                } ?: route
                val skin = section.getString("skin")?.let { skins[it] ?: error("找不到皮肤 $it") }
                val inventory = section.getConfigurationSection("inventory")?.getValues(false)?.mapKeys { (slot, _) ->
                    slot.toIntOrNull()?.takeIf { it in 0..40 } ?: error("无效的物品栏槽位 $slot")
                }?.mapValues { (_, item) ->
                    Material.matchMaterial(item.toString())?.takeIf { it.isItem && !it.isAir }
                        ?: error("无效的物品 $item")
                }.orEmpty()
                val name = value("name", "&cBot").toString()
                require(name.isNotBlank()) { "名称不可留空" }
                PitBotDefinition(id, section.getBoolean("enabled", true), position, waypoints, skin, inventory, name,
                    value("show-nameplate", true) as Boolean, mode(value("mode", "patrol").toString()),
                    ticks("respawn-ticks", 10, -1), number("speed", 1.0, 0.0..5.0),
                    number("radius", 8.0, 1.0..32.0), number("health", 20.0, 1.0..1024.0),
                    number("reach", 2.5, 0.1..6.0), ticks("attack-ticks", 30, 1))
            } catch (failure: RuntimeException) {
                throw IllegalArgumentException("Bot $id：${failure.message}", failure)
            }
        }
    }

    companion object {
        private val ID_PATTERN = Regex("[a-z0-9_-]{1,32}")

        fun validId(id: String): Boolean = id.matches(ID_PATTERN)

        fun mode(value: String): CombatMode = CombatMode.valueOf(value.replace('-', '_').uppercase(Locale.ROOT))

        private fun requiredString(section: ConfigurationSection, key: String): String =
            section.getString(key)?.takeIf { it.isNotBlank() } ?: error("缺少 ${section.currentPath}.$key")

        private fun ConfigurationSection.children(): Map<String, ConfigurationSection> = getKeys(false).associateWith {
            getConfigurationSection(it) ?: error("无效的配置节 $currentPath.$it")
        }
    }
}

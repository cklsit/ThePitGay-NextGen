package cn.irina.thepitaddon

import cn.charlotte.pit.ThePit
import cn.charlotte.pit.events.trigger.type.IEpicEvent
import cn.irina.thepitaddon.command.admin.*
import cn.irina.thepitaddon.command.player.*
import cn.irina.thepitaddon.command.player.buyItems.BuyEgg
import cn.irina.thepitaddon.command.player.buyItems.BuyExDiamondItem
import cn.irina.thepitaddon.command.player.buyItems.BuyIronHelmet
import cn.irina.thepitaddon.command.player.buyItems.BuyPhysicalCoin
import cn.irina.thepitaddon.enchantment.EnchantmentManager
import cn.irina.thepitaddon.manager.FilterManager
import cn.irina.thepitaddon.runnable.Announcer
import cn.irina.thepitaddon.runnable.FreeCE
import cn.irina.thepitaddon.utils.BannerUtil
import cn.irina.thepitaddon.utils.DynamicInvoke
import cn.irina.thepitaddon.utils.HideAccess
import cn.irina.thepitaddon.utils.Log.send
import dev.rollczi.litecommands.LiteCommands
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory
import me.dw1e.kbm.KnockbackManager
import me.dw1e.kbm.api.KnockbackManagerAPI
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.music.NBSDecoder
import net.mizukilab.pit.util.music.Song
import org.black_ixx.playerpoints.PlayerPoints
import org.black_ixx.playerpoints.PlayerPointsAPI
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.reflections.Reflections
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


class Main : JavaPlugin() {
    init {
        instance = this
    }

    private val songs: MutableMap<String, Song> = HashMap()
    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(3)
    private var serverLoaded = false

    private val depends = listOf(
        "LuckPerms",
        "ThePitUltimate",
        "PlayerPoints",
        "KnockbackManager"
    )

    private val file: File = File("plugins/ThePitAddon", "config.yml")
    private val cfg: FileConfiguration = YamlConfiguration.loadConfiguration(file)
    private val PlayerDataPath: String = cfg.getString("PlayerDataPath") ?: ""
    private val filterManager: FilterManager = FilterManager(instance)
    val PREFIX: String = CC.translate(instance.config.getString("Prefix") ?: "&8[&bI&fRINA&8] &f| ")
    val pointsAPI: PlayerPointsAPI?
        get() {
            val playerPoints = Bukkit.getPluginManager().getPlugin("PlayerPoints")
            return if (playerPoints is PlayerPoints) playerPoints.api else null
        }
    var kbmAPI: KnockbackManagerAPI? = null
        private set
    fun getFilterManager(): FilterManager{
        return filterManager
    }

    private fun modifyRarityPrefix() {
        try {
            val prefixField = EnchantmentRarity::class.java.getDeclaredField("prefix")

            prefixField.isAccessible = true

            prefixField[EnchantmentRarity.RARE] = "&d稀有! "
            prefixField[EnchantmentRarity.RAGE_RARE] = "&4稀有! "
            prefixField[EnchantmentRarity.DARK_RARE] = "&5稀有! "
            prefixField[EnchantmentRarity.AUCTION_LIMITED] = "&6拍卖! "
            prefixField[EnchantmentRarity.AUCTION_LIMITED_RARE] = "&6拍卖! "

            val messages = listOf(
                "&fRARE: " + EnchantmentRarity.RARE.prefix,
                "&fRAGE RARE: " + EnchantmentRarity.RAGE_RARE.prefix,
                "&fDARK RARE: " + EnchantmentRarity.DARK_RARE.prefix,
                "&fAUCTION: " + EnchantmentRarity.AUCTION_LIMITED.prefix,
                "&fAUCTION RARE: " + EnchantmentRarity.AUCTION_LIMITED_RARE.prefix
                        + "&f"
            )

            Bukkit.getConsoleSender().sendMessage(CC.translate(PREFIX + messages))
        } catch (e: Exception) {
            Bukkit.getLogger().severe("错误! 无法反射并修改 Enchantment Rarity $e")
        }
    }

    @HideAccess
    @DynamicInvoke
    override fun onEnable() {
        instance = this
        loadMusicResources()

        BannerUtil.send()

        send("&e天坑斗斗终极版扩展 启动中...")
        send("&7作者: &fIrina &7| &fhttps://github.com/BaiJiuSama")

        saveResource("config.yml", false)
        saveResource("filter.yml", false)

        Bukkit.getScheduler().runTaskLater(this, {
            depends.forEach {
                val depend = Bukkit.getPluginManager().getPlugin(it)
                if (depend == null || !depend.isEnabled) {
                    send("$PREFIX&c前置 &e$it &c未加载或缺失!")
                    Bukkit.shutdown()
                    return@runTaskLater
                }
            }
            send("&a无缺失前置, 等待 &fThePit Ultimate &a加载完毕后加载...")

            object : BukkitRunnable() {
                var index = 0
                override fun run() {
                    index++
                    if (index >= 120) {
                        send("&fThePit Ultimate &c无法加载, 请检查您的 &f授权码 &c或 &f配置文件")
                        send("&c进入线程睡眠模式, 请通过 &f终止任务 &c退出")
                        this.cancel()
                        while (true) {
                            Thread.sleep(Long.MAX_VALUE)
                        }
                    }

                    if (ThePit.getApi() == null) return

                    send("&fThePit Ultimate &a已加载完毕, 正在加载...")
                    this.cancel()
                    setUp()
                    Bukkit.getScheduler().scheduleSyncDelayedTask(instance, {
                        serverLoaded = true
                    }, 5 * 20L)
                }
            }.runTaskTimerAsynchronously(this, 0, 5 * 20L)
        }, 21L)
    }

    private fun setUp() {
        kbmAPI = KnockbackManager.getInstance().api
        clearEpicEvents()
        loadEnchantmentManager()
        registerCommands()
        loadListener()
        modifyRarityPrefix()

        if (config.getBoolean("DamageValidRange.Enable")) send("&e玩家伤害已被改动! 请注意!")
        send("&a天坑乱斗终极版扩展 已启动!")
        send("&aPowered by Irina, ShanguanLinG, MIYU")

        if (config.getBoolean("FreeCoinAndExperience.Enable")) {
            Bukkit.getScheduler().runTaskTimer(this, FreeCE(), 0L, 5 * 20L * 60L)
        } else {
            send("&c未启用 挂机池")
        }
        scheduler.scheduleWithFixedDelay(Announcer(), 0L, 5L, TimeUnit.MINUTES)
    }

    @HideAccess
    @DynamicInvoke
    override fun onDisable() {
        send("&c天坑斗斗终极版扩展 关闭中!")
        Bukkit.setWhitelist(true)

        if (this.liteCommands != null) this.liteCommands!!.unregister();

        if (PlayerDataPath.isEmpty()) {
            send("&cPlayerDataPath 为空")
        } else {
            val path = Paths.get(PlayerDataPath)
            try {
                Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
                    @Throws(IOException::class)
                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        Files.delete(file) // 删除文件
                        return FileVisitResult.CONTINUE
                    }

                    @Throws(IOException::class)
                    override fun postVisitDirectory(dir: Path?, exc: IOException?): FileVisitResult {
                        dir?.let { Files.delete(it) } // 删除文件夹
                        return FileVisitResult.CONTINUE
                    }
                })
                send("&a\"PlayerData\" 已删除, 准备关闭!")
            } catch (e: IOException) {
                send("&c无法删除 \"PlayerData\"")
            }
        }
    }

    private fun clearEpicEvents() {
        try {
            val eventFactoryClass = Class.forName("cn.charlotte.pit.events.EventFactory")
            val thePitInstance = ThePit.getInstance()
            val eventFactoryField = ThePit::class.java.getDeclaredField("eventFactory")
            eventFactoryField.isAccessible = true
            val eventFactoryInstance = eventFactoryField.get(thePitInstance)
            val epicEventsField = eventFactoryClass.getDeclaredField("epicEvents")
            epicEventsField.isAccessible = true
            val emptyList = java.util.Collections.emptyList<IEpicEvent>()
            epicEventsField.set(eventFactoryInstance, emptyList)
            send("&a事件清空成功!")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var liteCommands: LiteCommands<CommandSender>? = null
    private fun registerCommands() {
        this.liteCommands = LiteBukkitFactory.builder("ThePitAddon", this)
            .commands(
                AdminChangeGameMode(),
                CommandEnchant(),
                unEnchant(),
                UnGem(),
                AdminCrashClient(),
                AdminHealSelf(),
                AdminClearBounty(),
                PlayerHat(),
                Ping(),
                AdminOrganizeItem(),
                LockCommand(),
                SetHitDelay(),
                Private(),
                ChangeUserMeta(),
                BuyExDiamondItem(),
                BuyEgg(),
                BuyPhysicalCoin(),
                BuyIronHelmet(),
                PlayerOpenTrash(),
                PlayerSuicide(),
                ShowDevelopmentCommand(),
                FixWipe(),
                WorldTp(),
                Broadcast(),
                PlayerProving(),
                OpenMenu(),
                ReloadConfig(),
            )
            .build()
    }

    private fun loadMusicResources() {
        val musicResources = listOf(
            "FlowerDance.nbs",
            "BadApple.nbs",
            "FugueInDMinor.nbs",
            "GerudoValley.nbs",
            "MortalKombat.nbs",
            "RainbowTylenol.nbs",
            "GirlsBandCry.nbs",
            "Megalovania.nbs",
            "NeverGonnaGiveYouUp.nbs",
        )

        for (filePath in musicResources) {
            try {
                javaClass.classLoader.getResourceAsStream(filePath).use { stream ->
                    if (stream != null) {
                        val song = NBSDecoder.parse(stream)
                        songs.put(filePath, song)
                    } else {
                        logger.warning("寻找资源失败: $filePath")
                    }
                }
            } catch (e: Exception) {
                logger.severe("加载音乐资源失败: " + filePath + ", " + e.message)
            }
        }
    }

    fun getSong(fileName: String): Song? {
        return songs[fileName]
    }

    private fun loadEnchantmentManager() {
        val enchantmentManager = EnchantmentManager()
        enchantmentManager.registerEnchantment()
    }

    private val dontLoads = listOf(
        "EnderBow"
    )


    private fun loadListener() {
        val reflections = Reflections("cn.irina.thepitaddon")
        val classes = reflections.getSubTypesOf(Listener::class.java)

        send("&e扫描到的监听类数量: &f${classes.size}")
        for (clazz in classes) {
            if (dontLoads.contains(clazz.simpleName)) {
                send("&c跳过注册: &f${clazz.simpleName}")
                continue
            }
            send("&a注册: &f${clazz.simpleName}")
            val listener = clazz.getDeclaredConstructor().newInstance() as Listener
            Bukkit.getPluginManager().registerEvents(listener, this)
        }
    }

    companion object OverallResourceHolder {
        @JvmStatic
        lateinit var instance: Main
            private set
    }

    fun isServerLoaded(): Boolean {
        return serverLoaded
    }
}
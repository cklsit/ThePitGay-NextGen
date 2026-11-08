package cn.irina.thepitaddon.enchantment

import cn.charlotte.pit.ThePit
import cn.irina.thepitaddon.Main
import cn.irina.thepitaddon.utils.Log
import lombok.Getter
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity.*
import net.mizukilab.pit.util.chat.CC.translate
import org.bukkit.Bukkit
import org.reflections.Reflections


//@Getter
//class EnchantmentManager {
//    private val enchantmentClasses: MutableList<Class<out AbstractEnchantment>> = ArrayList()
//
//    private val formatEnchantList: MutableList<String> = ArrayList()
//    private val opEnchants: MutableList<String> = ArrayList()
//    private val rareEnchants: MutableList<String> = ArrayList()
//    private val normalEnchants: MutableList<String> = ArrayList()
//    private val rageEnchants: MutableList<String> = ArrayList()
//    private val rageRareEnchants: MutableList<String> = ArrayList()
//    private val darkEnchants: MutableList<String> = ArrayList()
//    private val darkRareEnchants: MutableList<String> = ArrayList()
//
//    fun registerEnchantment() {
////        val classes = ClassUtil.getClassesInPackage(Main.instance, "cn.irina.thepitaddon.enchantment.type")!!
//        val classes: Array<Class<*>> = arrayOf(VolleyA::class.java)
//        Bukkit.getConsoleSender().sendMessage(translate("$PREFIX&e扫描到的附魔类数量: ${classes.size}"))
//        for (clazz in classes) {
//            if (clazz == null || !AbstractEnchantment::class.java.isAssignableFrom(clazz)) continue
//            val newInstance = clazz.getDeclaredConstructor().newInstance() as AbstractEnchantment
//            val enchantName = newInstance.enchantName
//            when (newInstance.rarity) {
//                OP -> opEnchants.add(translate("&c限定 &f$enchantName"))
//                RARE -> rareEnchants.add(translate("&d稀有 &f$enchantName"))
//                NORMAL -> normalEnchants.add(translate("&7普通 &f$enchantName"))
//                RAGE -> rageEnchants.add(translate("&4暴怒 &f$enchantName"))
//                RAGE_RARE -> rageRareEnchants.add(translate("&4暴怒稀有 &f$enchantName"))
//                DARK_NORMAL -> darkEnchants.add(translate("&5暗黑 &f$enchantName"))
//                DARK_RARE -> darkRareEnchants.add(translate("&5暗黑稀有 &f$enchantName"))
//                else -> continue
//            }
//            @Suppress("UNCHECKED_CAST")
//            enchantmentClasses.add(clazz as Class<out AbstractEnchantment>)
//        }
//
//        formatEnchantList.addAll(opEnchants)
//        formatEnchantList.addAll(rageEnchants)
//        formatEnchantList.addAll(rageRareEnchants)
//        formatEnchantList.addAll(darkEnchants)
//        formatEnchantList.addAll(darkRareEnchants)
//        formatEnchantList.addAll(rareEnchants)
//        formatEnchantList.addAll(normalEnchants)
//
//        for (enchant in formatEnchantList) {
//            Bukkit.getConsoleSender().sendMessage(translate("$PREFIX&a附魔加载: $enchant"))
//        }
//
//        ThePit.getInstance().enchantmentFactor.init(enchantmentClasses)
//    }

@Getter
class EnchantmentManager {
    val prefix = Main.instance.PREFIX

    private val filteredEnchants: MutableList<Class<out AbstractEnchantment>> = ArrayList()
    private val formatEnchantList: MutableList<String> = ArrayList()
    private val opEnchants: MutableList<String> = ArrayList()
    private val rareEnchants: MutableList<String> = ArrayList()
    private val normalEnchants: MutableList<String> = ArrayList()
    private val rageEnchants: MutableList<String> = ArrayList()
    private val rageRareEnchants: MutableList<String> = ArrayList()
    private val darkEnchants: MutableList<String> = ArrayList()
    private val darkRareEnchants: MutableList<String> = ArrayList()

    private fun removalEnchants(): List<String> {
        return ArrayList(
            listOf(
                "stifle",
                "volley_enchant_B",
                "interdiction",
                //"fugue_in_d_minor_dj",
                //"diamond_breaker",
                "control",
                "clotting",
                //"girls_band_cry_dj",
                //"rationalist",
                //"kill_angels",
                "parasite",
                "Combo_Broken_String",
                //"break_armor",
                //"winter",
                //"ender_sword",
                "micro_anti_gravity",
                "gamble_enchant",
                //"bad_apple_dj",
                //"gerudo_valley_dj",
                //"volley_enchant",
                "unpredictably_enchant",
                "combo_ladder",
                "theswiftwind_enchant",
                "revengeance",
                "doom_pact",
                "devil_chicken",
                //"everybody_dance_now_dj",
                //"flower_dance_dj",
                //"mortal_kombat_dj",
                "FightOrDie",
                //"rainbow_tylenol_dj",
                "verdict",
                "exploration_specialist",
                "last_stand",
                "back_stab",
                "combo_radiant_gold",
                "judgment_strike",
                "divine_miracle_enchant",
                "lunar_deity",
                "athena",
                //"tide"
                //"war_pay"
                //"terminator"
                "ender_bow_enchant",
                //"gravitational_backtracking"
                //"electrolytes_enchant"
                //"regularity",
                //"like_french"
                "temporal_ripple",
                "desperation",
                "coin_glorious",
                //"frost"
                "outnumbered_by_the_enemy",
                //"tear"
                //"vital_echo",
                //"lonely_red_blood"
                //"steal"
                //"piercing_bow"
                //"ddj"
                //"please_stop"
                //"poet"
                //"miners_enchant"
                //"soul_of_frozen_clarity"
                //"think_of_the_people"
                //"bounty_hunter_hunter"
                //"shattered_astral_crossbow"
                //"piercing_bow"
            )
        )
    }

    fun registerEnchantment() {
        val reflections = Reflections("cn.irina.thepitaddon")
        val enchantmentClasses = reflections.getSubTypesOf(
            AbstractEnchantment::class.java
        )

        Bukkit.getConsoleSender().sendMessage(translate("$prefix&e扫描到的附魔类数量: ${enchantmentClasses.size}"))
        for (clazz in enchantmentClasses) {
            val newInstance = clazz.getDeclaredConstructor().newInstance()
            val enchantName = newInstance.enchantName

            if (removalEnchants().contains(newInstance.nbtName)) {
                Log.send("&c跳过加载: &f$enchantName")
                continue
            }

            filteredEnchants.add(clazz)

            when (newInstance.rarity) {
                OP -> opEnchants.add(translate("&c限定 &f$enchantName"))
                RARE -> rareEnchants.add(translate("&d稀有 &f$enchantName"))
                NORMAL -> normalEnchants.add(translate("&7普通 &f$enchantName"))
                RAGE -> rageEnchants.add(translate("&4暴怒 &f$enchantName"))
                RAGE_RARE -> rageRareEnchants.add(translate("&4暴怒稀有 &f$enchantName"))
                DARK_NORMAL -> darkEnchants.add(translate("&5暗黑 &f$enchantName"))
                DARK_RARE -> darkRareEnchants.add(translate("&5暗黑稀有 &f$enchantName"))
                else -> continue
            }
        }

        formatEnchantList.addAll(opEnchants)
        formatEnchantList.addAll(rageEnchants)
        formatEnchantList.addAll(rageRareEnchants)
        formatEnchantList.addAll(darkEnchants)
        formatEnchantList.addAll(darkRareEnchants)
        formatEnchantList.addAll(rareEnchants)
        formatEnchantList.addAll(normalEnchants)

        if (filteredEnchants.isEmpty()) {
            Log.send("&c扩展附魔列表为空!")
            return
        }

        for (enchant in formatEnchantList) {
            Log.send("&a附魔加载: $enchant")
        }

        ThePit.getInstance().enchantmentFactor.init(filteredEnchants)
        ThePit.getInstance().enchTable.reload()
//        for (removalEnchants in removalEnchants()) {
//            ThePit.getInstance().enchantmentFactor.unregister(removalEnchants,null)
//        }
    }
}


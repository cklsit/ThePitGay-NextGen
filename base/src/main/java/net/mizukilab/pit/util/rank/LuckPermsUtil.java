package net.mizukilab.pit.util.rank;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * LuckPerms 适配层。
 * <p>
 * LuckPerms 是<strong>可选依赖</strong>：未安装时，所有方法返回安全默认值，
 * 绝不因 {@code LuckPermsProvider.get()} 抛出 NoClassDefFoundError。
 */
public class LuckPermsUtil {

    private static final boolean AVAILABLE = Bukkit.getPluginManager().isPluginEnabled("LuckPerms");

    static LuckPerms luckPerms;
    static UserManager userManager;
    static GroupManager groupManager;

    static {
        if (AVAILABLE) {
            try {
                luckPerms = LuckPermsProvider.get();
                userManager = luckPerms.getUserManager();
                groupManager = luckPerms.getGroupManager();
            } catch (Throwable ignored) {
                luckPerms = null;
                userManager = null;
                groupManager = null;
            }
        }
    }

    public LuckPermsUtil() {
    }

    public static boolean isAvailable() {
        return AVAILABLE && luckPerms != null;
    }

    public static String getPrefix(UUID uuid) {
        User user = getUser(uuid);
        if (user != null) {
            String prefix = user.getCachedData().getMetaData().getPrefix();
            return prefix != null ? ChatColor.translateAlternateColorCodes('&', prefix) + " " : "§7";
        }
        return "§7";
    }

    public static String getSuffix(UUID uuid) {
        User user = getUser(uuid);
        return user != null && user.getCachedData().getMetaData().getSuffix() != null
                ? " " + ChatColor.translateAlternateColorCodes('&', user.getCachedData().getMetaData().getSuffix())
                : "§7";
    }

    public static User getUser(UUID uuid) {
        if (userManager == null) {
            return null;
        }
        try {
            if (userManager.isLoaded(uuid)) {
                return userManager.getUser(uuid);
            } else {
                CompletableFuture<User> userCompletableFuture = userManager.loadUser(uuid);
                return userCompletableFuture.join();
            }
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static UUID lookupUniqueId(String name) {
        if (userManager == null) {
            return null;
        }
        try {
            return userManager.lookupUniqueId(name).get();
        } catch (Throwable ignored) {
            return null;
        }
    }
}

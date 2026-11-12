package net.mizukilab.pit.menu.admin.item;

import cn.charlotte.pit.ThePit;
import net.mizukilab.pit.enchantment.AbstractEnchantment;
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase;
import net.mizukilab.pit.item.type.mythic.MythicHammerItem;
import net.mizukilab.pit.util.chat.CC;
import net.mizukilab.pit.util.chat.RomanUtil;
import net.mizukilab.pit.util.item.ItemBuilder;
import net.mizukilab.pit.util.menu.Button;
import net.mizukilab.pit.util.menu.pagination.PaginatedMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 神话之锤附魔选择：点击任意附魔即获得一把带该附魔（3 级）的锤子。
 */
public class HammerEnchantMenu extends PaginatedMenu {

    @Override
    public String getPrePaginatedTitle(Player player) {
        return "&b神话之锤附魔";
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = new LinkedHashMap<>();
        List<AbstractEnchantment> enchants = ThePit.getInstance().getEnchantmentFactor()
                .getEnchantmentMap()
                .values()
                .stream()
                .filter(enchantment -> enchantment instanceof HammerEnchantBase)
                .sorted(Comparator.comparing(AbstractEnchantment::getNbtName))
                .toList();

        int index = 0;
        for (AbstractEnchantment enchantment : enchants) {
            buttons.put(index++, new HammerEnchantButton(enchantment));
        }
        return buttons;
    }

    private static final class HammerEnchantButton extends Button {
        private final AbstractEnchantment enchantment;

        private HammerEnchantButton(AbstractEnchantment enchantment) {
            this.enchantment = enchantment;
        }

        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.GOLDEN_AXE)
                    .name("&e" + enchantment.getEnchantName())
                    .lore("",
                            "&7稀有度: &f" + enchantment.getRarity().name(),
                            "&7等级: &f3 / " + RomanUtil.convert(enchantment.getMaxEnchantLevel()),
                            "&7效果: &f" + enchantment.getUsefulnessLore(3),
                            "",
                            "&e左键 &7获得带该附魔的锤子")
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            MythicHammerItem hammer = new MythicHammerItem();
            hammer.getEnchantments().put(enchantment, 3);
            player.getInventory().addItem(hammer.toItemStack());
            player.sendMessage(CC.translate("&a已获得 &e" + enchantment.getEnchantName() + " &a的神话之锤"));
            Button.playSuccess(player);
        }
    }
}

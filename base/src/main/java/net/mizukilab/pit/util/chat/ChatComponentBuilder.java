package net.mizukilab.pit.util.chat;

/**
 * @Author: EmptyIrony
 * @Date: 2020/12/30 22:54
 */

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本组件构建器。
 * <p>
 * 1.21 适配：Paper 内置的 {@code ComponentBuilder} 已改为 final（Adventure 桥接实现），无法再继承，
 * 这里改为<strong>组合</strong>方式，对外 API 与原实现保持一致。
 */
public class ChatComponentBuilder {

    private final List<BaseComponent> parts = new ArrayList<>();
    private TextComponent current = new TextComponent("");

    public ChatComponentBuilder(String text) {
        this.parse(text);
    }

    public TextComponent getCurrent() {
        return current;
    }

    public void setCurrent(TextComponent tc) {
        this.current = tc;
    }

    @SuppressWarnings("rawtypes")
    public List getParts() {
        return parts;
    }

    public ChatComponentBuilder setCurrentHoverEvent(HoverEvent hoverEvent) {
        this.current.setHoverEvent(hoverEvent);
        return this;
    }

    public ChatComponentBuilder setCurrentClickEvent(ClickEvent clickEvent) {
        this.current.setClickEvent(clickEvent);
        return this;
    }

    public ChatComponentBuilder attachToEachPart(HoverEvent hoverEvent) {
        for (BaseComponent part : parts) {
            if (part instanceof TextComponent text && text.getHoverEvent() == null) {
                text.setHoverEvent(hoverEvent);
            }
        }
        this.current.setHoverEvent(hoverEvent);
        return this;
    }

    public ChatComponentBuilder attachToEachPart(ClickEvent clickEvent) {
        for (BaseComponent part : parts) {
            if (part instanceof TextComponent text && text.getClickEvent() == null) {
                text.setClickEvent(clickEvent);
            }
        }
        this.current.setClickEvent(clickEvent);
        return this;
    }

    public ChatComponentBuilder parse(String text) {
        if (text == null || text.isEmpty()) {
            return this;
        }
        BaseComponent[] parsed = TextComponent.fromLegacyText(text);
        for (BaseComponent component : parsed) {
            parts.add(component);
            if (component instanceof TextComponent textComponent) {
                current = textComponent;
            }
        }
        return this;
    }

    public ChatComponentBuilder append(BaseComponent[] components) {
        for (BaseComponent component : components) {
            parts.add(component);
            if (component instanceof TextComponent textComponent) {
                current = textComponent;
            }
        }
        return this;
    }

    public ChatComponentBuilder append(TextComponent textComponent) {
        parts.add(textComponent);
        current = textComponent;
        return this;
    }

    public BaseComponent[] create() {
        return parts.toArray(new BaseComponent[0]);
    }

    /**
     * 保留旧实现的颜色常量入口，避免调用方改动。
     */
    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * 兼容旧代码中的链式构建：{@code new ChatComponentBuilder(text).append(...)}。
     */
    public ChatComponentBuilder append(String text) {
        return parse(text);
    }
}

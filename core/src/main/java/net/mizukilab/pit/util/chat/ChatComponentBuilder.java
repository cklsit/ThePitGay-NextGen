package net.mizukilab.pit.util.chat;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: EmptyIrony
 * @Date: 2020/12/30 22:54
 *
 * <p>可交互聊天消息构建器（悬停/点击）。</p>
 *
 * <p>1.21 适配：BungeeCord 新版 chat API 把 {@code ComponentBuilder} 变成了 final，
 * 原来「继承 ComponentBuilder + 反射改 {@code current}/{@code parts} 字段」的做法无法编译，
 * 这里改成自持分段列表：颜色/格式码按原版语义作用到其后的文本，
 * 悬停与点击事件附着在最后一段。</p>
 */
public class ChatComponentBuilder {

    private static final Pattern COLOR_CODE = Pattern.compile("[&\u00a7]([0-9a-fk-orA-FK-OR])");

    private final List<TextComponent> parts = new ArrayList<>();

    public ChatComponentBuilder(String text) {
        this.parse(text);
    }

    private static TextComponent segment(String text,
                                         ChatColor color,
                                         boolean bold,
                                         boolean italic,
                                         boolean underlined,
                                         boolean strikethrough,
                                         boolean obfuscated) {
        TextComponent component = new TextComponent(text);
        if (color != null) {
            component.setColor(color);
        }
        component.setBold(bold);
        component.setItalic(italic);
        component.setUnderlined(underlined);
        component.setStrikethrough(strikethrough);
        component.setObfuscated(obfuscated);
        return component;
    }

    public ChatComponentBuilder parse(String text) {
        String input = text == null ? "" : text;
        ChatColor color = ChatColor.WHITE;
        boolean bold = false;
        boolean italic = false;
        boolean underlined = false;
        boolean strikethrough = false;
        boolean obfuscated = false;

        Matcher matcher = COLOR_CODE.matcher(input);
        int cursor = 0;
        while (matcher.find()) {
            String chunk = input.substring(cursor, matcher.start());
            if (!chunk.isEmpty()) {
                this.parts.add(segment(chunk, color, bold, italic, underlined, strikethrough, obfuscated));
            }
            cursor = matcher.end();
            char code = Character.toLowerCase(matcher.group(1).charAt(0));
            switch (code) {
                case 'k' -> obfuscated = true;
                case 'l' -> bold = true;
                case 'm' -> strikethrough = true;
                case 'n' -> underlined = true;
                case 'o' -> italic = true;
                case 'r' -> {
                    color = ChatColor.WHITE;
                    bold = italic = underlined = strikethrough = obfuscated = false;
                }
                default -> {
                    ChatColor parsed = ChatColor.getByChar(code);
                    if (parsed != null) {
                        color = parsed;
                        bold = italic = underlined = strikethrough = obfuscated = false;
                    }
                }
            }
        }
        String tail = input.substring(cursor);
        if (!tail.isEmpty()) {
            this.parts.add(segment(tail, color, bold, italic, underlined, strikethrough, obfuscated));
        }
        if (this.parts.isEmpty()) {
            this.parts.add(segment("", color, bold, italic, underlined, strikethrough, obfuscated));
        }
        return this;
    }

    public List<TextComponent> getParts() {
        return this.parts;
    }

    public TextComponent getCurrent() {
        return this.parts.isEmpty() ? null : this.parts.get(this.parts.size() - 1);
    }

    public void setCurrent(TextComponent component) {
        if (this.parts.isEmpty()) {
            this.parts.add(component);
        } else {
            this.parts.set(this.parts.size() - 1, component);
        }
    }

    public ChatComponentBuilder setCurrentHoverEvent(HoverEvent hoverEvent) {
        TextComponent current = this.getCurrent();
        if (current != null) {
            current.setHoverEvent(hoverEvent);
        }
        return this;
    }

    public ChatComponentBuilder setCurrentClickEvent(ClickEvent clickEvent) {
        TextComponent current = this.getCurrent();
        if (current != null) {
            current.setClickEvent(clickEvent);
        }
        return this;
    }

    public ChatComponentBuilder attachToEachPart(HoverEvent hoverEvent) {
        for (TextComponent component : this.parts) {
            if (component.getHoverEvent() == null) {
                component.setHoverEvent(hoverEvent);
            }
        }
        return this;
    }

    public ChatComponentBuilder attachToEachPart(ClickEvent clickEvent) {
        for (TextComponent component : this.parts) {
            if (component.getClickEvent() == null) {
                component.setClickEvent(clickEvent);
            }
        }
        return this;
    }

    public ChatComponentBuilder append(String text) {
        this.parts.add(segment(text == null ? "" : text, ChatColor.WHITE, false, false, false, false, false));
        return this;
    }

    public ChatComponentBuilder append(TextComponent textComponent) {
        if (textComponent != null) {
            this.parts.add(textComponent);
        }
        return this;
    }

    public ChatComponentBuilder append(BaseComponent[] components) {
        if (components != null) {
            for (BaseComponent component : components) {
                if (component instanceof TextComponent textComponent) {
                    this.parts.add(textComponent);
                }
            }
        }
        return this;
    }

    public BaseComponent[] create() {
        return this.parts.toArray(new BaseComponent[0]);
    }
}

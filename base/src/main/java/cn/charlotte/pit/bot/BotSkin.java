package cn.charlotte.pit.bot;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bot 皮肤数据（base64 纹理值 + 签名）。
 * <p>
 * 与现有 NPC 皮肤机制保持一致：都使用 Minecraft 的 textures 属性。
 * 默认皮肤为空，客户端会回退到 Steve。
 * <p>
 * 说明：这里不直接依赖 {@code com.mojang.authlib}，属性以 ProtocolLib 的
 * {@code WrappedGameProfile} 约定格式（{@code value;signature}）写入，避免编译期对 authlib 的依赖。
 */
public final class BotSkin {

    public static final BotSkin DEFAULT = new BotSkin(null, null);

    private static final Pattern URL_PATTERN = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");

    private final String value;
    private final String signature;

    public BotSkin(String value, String signature) {
        this.value = value;
        this.signature = signature;
    }

    public String getValue() {
        return value;
    }

    public String getSignature() {
        return signature;
    }

    public boolean isEmpty() {
        return value == null || value.isEmpty();
    }

    /**
     * 解析命令参数，支持三种写法：
     * <ul>
     *   <li>base64 纹理值（以 ey 开头）——直接使用，缺签名时按无签名处理</li>
     *   <li>纹理 URL——包装成标准 textures JSON 后再编码</li>
     *   <li>玩家名——返回 null，由调用方异步查询 Mojang 后再设置</li>
     * </ul>
     */
    public static BotSkin parseInline(String input) {
        if (input == null || input.isEmpty()) {
            return DEFAULT;
        }
        if (input.startsWith("ey")) {
            return new BotSkin(input, null);
        }
        if (input.startsWith("http://") || input.startsWith("https://")) {
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + input + "\"}}}";
            return new BotSkin(Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8)), null);
        }
        return null;
    }

    public static String extractUrl(String base64Value) {
        try {
            String json = new String(Base64.getDecoder().decode(base64Value), StandardCharsets.UTF_8);
            Matcher matcher = URL_PATTERN.matcher(json);
            return matcher.find() ? matcher.group(1) : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    /**
     * 写入 ProtocolLib 的属性表：{@code textures -> value} 或 {@code value;signature}。
     */
    public void applyTo(Map<String, String> properties) {
        if (isEmpty()) {
            return;
        }
        if (signature == null || signature.isEmpty()) {
            properties.put("textures", value);
        } else {
            properties.put("textures", value + ";" + signature);
        }
    }

    public static UUID offlineUuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }
}

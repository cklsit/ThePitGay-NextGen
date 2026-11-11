package cn.mythicland.npclib.bot.v1_8_R3;

import cn.mythicland.npclib.api.skin.Skin;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;

import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * 机器人实体：以 Paper 的真实登录流程建立 {@link ServerPlayer}，实体由服务端正常追踪，
 * 可见性另行由 {@code BotVisibility} 按玩家裁剪。移动与攻击由 Bot 模块逐 tick 驱动。
 */
public class BotEntity extends ServerPlayer {
    private final boolean walking;
    private final String sourceName;
    private final BiConsumer<BotEntity, DamageSource> deathHandler;
    private final BiConsumer<BotEntity, Runnable> stepHandler;
    private BotConnection connectionHandle;

    public record Options(
            String name,
            String displayName,
            Skin skin,
            boolean walking,
            BiConsumer<BotEntity, DamageSource> deathHandler,
            BiConsumer<BotEntity, Runnable> stepHandler
    ) {
        public Options {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(deathHandler, "deathHandler");
            Objects.requireNonNull(stepHandler, "stepHandler");
        }
    }

    public static BotEntity create(Location location, Options options) {
        BotEntity entity = new BotEntity(location, profile(options.skin()), options);
        entity.spawn();
        return entity;
    }

    private static GameProfile profile(Skin skin) {
        UUID id = UUID.randomUUID();
        GameProfile profile = new GameProfile(id, "b" + id.toString().replace("-", "").substring(0, 15));
        if (skin != null) {
            profile.getProperties().put("textures", new Property("textures", skin.value(), skin.signature()));
        }
        return profile;
    }

    private BotEntity(Location location, GameProfile profile, Options options) {
        super(
                MinecraftServer.getServer(), ((CraftWorld) location.getWorld()).getHandle(),
                profile, ClientInformation.createDefault()
        );
        this.walking = options.walking();
        this.sourceName = options.name();
        this.deathHandler = options.deathHandler();
        this.stepHandler = options.stepHandler();
        this.setPos(location.getX(), location.getY(), location.getZ());
        this.setYRot(0.0F);
        this.setXRot(0.0F);
        this.setYHeadRot(0.0F);
        this.setYBodyRot(0.0F);
        this.setDeltaMovement(Vec3.ZERO);
        this.getBukkitEntity().setDisplayName(options.displayName());
        this.getBukkitEntity().setPlayerListName(options.displayName());
    }

    protected final void spawn() {
        MinecraftServer server = MinecraftServer.getServer();
        BotConnection connection = new BotConnection();
        connection.attach();
        this.connectionHandle = connection;
        server.getPlayerList().placeNewPlayer(
                connection, this, CommonListenerCookie.createInitial(this.getGameProfile(), false)
        );
    }

    /**
     * 假连接：保留真实 {@link net.minecraft.network.Connection} 结构（地址、通道、流水线都在），
     * 但所有出站包直接丢弃，避免发包队列无界增长。
     *
     * <p>继承处必须写全限定名：{@code LivingEntity} 实现了 {@code WaypointTransmitter}，
     * 它的嵌套接口 {@code Connection} 会遮蔽 {@code net.minecraft.network.Connection} 的导入。</p>
     */
    static final class BotConnection extends net.minecraft.network.Connection {
        BotConnection() {
            super(PacketFlow.SERVERBOUND);
        }

        void attach() {
            EmbeddedChannel channel = new EmbeddedChannel(this);
            this.channel = channel;
            this.address = channel.remoteAddress();
            this.configurePacketHandler(channel.pipeline());
        }

        @Override
        public void send(Packet<?> packet) {
        }

        @Override
        public void send(Packet<?> packet, ChannelFutureListener listener) {
        }
    }

    @Override
    public void tick() {
    }

    @Override
    public void doTick() {
    }

    /**
     * 由 Bot 模块每 tick 调用；承担 1.8 实现里 {@code tickState()} 与实体基础 tick 的职责。
     */
    public void tickState() {
        if (this.invulnerableTime > 0) {
            this.invulnerableTime--;
        }
        if (this.hurtTime > 0) {
            this.hurtTime--;
        }
        super.tick();
    }

    public void step(float strafe, float forward, double speed, boolean jump) {
        if (this.isRemoved()) return;
        this.updateSprinting();
        double multiplier = this.getBukkitEntity().getWalkSpeed() / 0.2D;
        double movement = 0.1D * speed * multiplier;
        AttributeInstance attribute = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute != null) {
            attribute.setBaseValue(movement);
        }
        this.setSpeed((float) movement);
        this.xxa = strafe;
        this.zza = forward;
        this.jumping = jump && this.onGround;
        if (this.jumping) {
            this.jumpFromGround();
        }
        this.travel(new Vec3(strafe, 0.0D, forward));
        this.setYHeadRot(this.getYRot());
        this.setYBodyRot(this.getYRot());
    }

    public void hit(org.bukkit.entity.Entity target) {
        this.setSprinting(true);
        this.attack(((CraftEntity) target).getHandle());
        this.updateSprinting();
    }

    /**
     * 巡逻机器人不疾跑、战斗机器人疾跑；只在状态不符时切换，避免每 tick 反复增删疾跑修饰符。
     */
    private void updateSprinting() {
        if (this.isSprinting() == this.walking) {
            this.setSprinting(!this.walking);
        }
    }

    @Override
    public void swing(InteractionHand hand) {
        super.swing(hand);
    }

    @Override
    public void die(DamageSource source) {
        this.deathHandler.accept(this, source);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        this.fallDistance = 0;
        return false;
    }

    @Override
    protected void checkFallDamage(double heightDifference, boolean onGround, BlockState state, BlockPos pos) {
        this.fallDistance = 0;
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return this.sourceName == null ? super.getDisplayName()
                : net.minecraft.network.chat.Component.literal(this.sourceName);
    }

    /**
     * 传送到新的位置；同维度走位置包，跨维度回落到 Bukkit 传送以复用维度切换流程。
     */
    public void transfer(Location location) {
        ServerLevel target = ((CraftWorld) location.getWorld()).getHandle();
        if (target == this.level()) {
            this.teleportSetPosition(new PositionMoveRotation(
                    new Vec3(location.getX(), location.getY(), location.getZ()), Vec3.ZERO,
                    this.getYRot(), this.getXRot()
            ), java.util.Set.of());
        } else {
            this.getBukkitEntity().teleport(location);
        }
        this.setDeltaMovement(Vec3.ZERO);
    }

    /**
     * 下线：机器人以真实登录身份存在，必须走 {@code PlayerList.remove} 才能从在线列表、
     * 统计与实体追踪里干净摘除（会触发 {@code PlayerQuitEvent}，与原版玩家一致）。
     */
    public void destroy() {
        MinecraftServer server = this.getServer();
        if (this.isRemoved()) return;
        server.getPlayerList().remove(this);
        this.discard();
        BotConnection connection = this.connectionHandle;
        if (connection != null && connection.channel != null) {
            connection.channel.close();
        }
        this.connectionHandle = null;
    }
}

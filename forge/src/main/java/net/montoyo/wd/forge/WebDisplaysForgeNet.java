package net.montoyo.wd.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.platform.WDNet;
import net.montoyo.wd.platform.WDPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Forge networking through {@code SimpleChannel}.
 * <p>
 * One {@code Msg} envelope class is registered once per packet type; the encoder/decoder lambdas
 * capture the concrete type, so the wire format stays identical to the other loaders.
 */
public class WebDisplaysForgeNet implements WDNet {

    private static final String PROTOCOL = "1";

    public static final class Msg {
        public final WDPacket packet;

        public Msg(WDPacket packet) {
            this.packet = packet;
        }
    }

    private final SimpleChannel channel = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(WebDisplays.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private final Map<Class<?>, Integer> ids = new HashMap<>();
    private int nextId = 0;

    @Override
    public <T extends WDPacket> void register(ResourceLocation id, Class<T> type,
                                              Function<FriendlyByteBuf, T> decoder) {
        int idx = nextId++;
        ids.put(type, idx);

        channel.messageBuilder(Msg.class, idx)
                .encoder((msg, buf) -> msg.packet.encode(buf))
                .decoder(buf -> new Msg(decoder.apply(buf)))
                .consumerMainThread((msg, ctx) -> {
                    msg.packet.handle(ctx.get().getSender());
                    ctx.get().setPacketHandled(true);
                })
                .add();
    }

    private Msg wrap(WDPacket packet) {
        if (!ids.containsKey(packet.getClass()))
            throw new IllegalStateException("Unregistered packet " + packet.getClass().getName());

        return new Msg(packet);
    }

    @Override
    public void sendToServer(WDPacket packet) {
        channel.sendToServer(wrap(packet));
    }

    @Override
    public void sendToPlayer(WDPacket packet, ServerPlayer player) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), wrap(packet));
    }

    @Override
    public void sendToAllAround(WDPacket packet, BlockPos pos, double radius) {
        var server = net.montoyo.wd.platform.Platform.get().server();

        if (server == null)
            return;

        Vec3 center = Vec3.atCenterOf(pos);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.position().distanceToSqr(center) <= radius * radius)
                sendToPlayer(packet, player);
        }
    }

    @Override
    public void sendToAll(WDPacket packet) {
        channel.send(PacketDistributor.ALL.noArg(), wrap(packet));
    }
}

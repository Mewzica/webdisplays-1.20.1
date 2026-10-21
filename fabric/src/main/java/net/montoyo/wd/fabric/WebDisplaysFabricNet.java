package net.montoyo.wd.fabric;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.montoyo.wd.platform.WDNet;
import net.montoyo.wd.platform.WDPacket;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Fabric networking, built on the legacy channel API that fabric-api 1.3.x still ships for 1.20.1.
 * <p>
 * Everything that touches {@code net.fabricmc.fabric.api.client} lives in
 * {@link WebDisplaysFabricNetClient}. Referring to those classes from here would make the dedicated
 * server fail during class verification, because Fabric's loader refuses to hand out client classes
 * in a {@code SERVER} environment.
 */
public class WebDisplaysFabricNet implements WDNet {

    private final Map<ResourceLocation, Function<FriendlyByteBuf, WDPacket>> decoders = new LinkedHashMap<>();
    private final Map<Class<?>, ResourceLocation> ids = new HashMap<>();

    /** Set from the client entrypoint; stays null on a dedicated server. */
    @Nullable
    private WebDisplaysFabricNetClient clientSide;

    /**
     * Called by {@link WebDisplaysFabricClient} once the client is up. Replays every packet type
     * registered so far, since the common init runs before the client entrypoint.
     */
    void installClientSide(WebDisplaysFabricNetClient client) {
        this.clientSide = client;
        client.replay(decoders);
    }

    @Override
    public <T extends WDPacket> void register(ResourceLocation id, Class<T> type,
                                              Function<FriendlyByteBuf, T> decoder) {
        decoders.put(id, decoder::apply);
        ids.put(type, id);

        ServerPlayNetworking.registerGlobalReceiver(id, (server, player, handler, buf, sender) ->
                server.execute(() -> decoder.apply(buf).handle(player)));

        if (clientSide != null)
            clientSide.registerReceiver(id, decoder);
    }

    ResourceLocation idOf(WDPacket packet) {
        ResourceLocation id = ids.get(packet.getClass());

        if (id == null)
            throw new IllegalStateException("Unregistered packet " + packet.getClass().getName());

        return id;
    }

    FriendlyByteBuf encode(WDPacket packet) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        packet.encode(buf);
        return buf;
    }

    @Override
    public void sendToServer(WDPacket packet) {
        if (clientSide == null)
            throw new IllegalStateException("sendToServer() called outside the client");

        clientSide.send(idOf(packet), encode(packet));
    }

    @Override
    public void sendToPlayer(WDPacket packet, ServerPlayer player) {
        ServerPlayNetworking.send(player, idOf(packet), encode(packet));
    }

    @Override
    public void sendToAllAround(WDPacket packet, BlockPos pos, double radius) {
        var server = net.montoyo.wd.platform.Platform.get().server();

        if (server == null)
            return;

        double r2 = radius * radius;
        Vec3 center = Vec3.atCenterOf(pos);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.position().distanceToSqr(center) <= r2)
                sendToPlayer(packet, player);
        }
    }

    @Override
    public void sendToAll(WDPacket packet) {
        var server = net.montoyo.wd.platform.Platform.get().server();

        if (server == null)
            return;

        for (ServerPlayer player : server.getPlayerList().getPlayers())
            sendToPlayer(packet, player);
    }
}

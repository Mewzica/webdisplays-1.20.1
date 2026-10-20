package net.montoyo.wd.fabric;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.montoyo.wd.platform.WDNet;
import net.montoyo.wd.platform.WDPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Fabric networking, built on the legacy channel API that fabric-api 1.3.x still ships for 1.20.1.
 */
public class WebDisplaysFabricNet implements WDNet {

    private final Map<ResourceLocation, Function<FriendlyByteBuf, WDPacket>> decoders = new HashMap<>();
    private final Map<Class<?>, ResourceLocation> ids = new HashMap<>();

    @Override
    public <T extends WDPacket> void register(ResourceLocation id, Class<T> type,
                                              Function<FriendlyByteBuf, T> decoder) {
        decoders.put(id, decoder::apply);
        ids.put(type, id);

        ServerPlayNetworking.registerGlobalReceiver(id, (server, player, handler, buf, sender) ->
                server.execute(() -> decoder.apply(buf).handle(player)));

        if (net.montoyo.wd.platform.Platform.get().isClient())
            registerClient(id, decoder);
    }

    private <T extends WDPacket> void registerClient(ResourceLocation id,
                                                     Function<FriendlyByteBuf, T> decoder) {
        ClientPlayNetworking.registerGlobalReceiver(id, (client, handler, buf, sender) ->
                client.execute(() -> decoder.apply(buf).handle(client.player)));
    }

    private ResourceLocation idOf(WDPacket packet) {
        ResourceLocation id = ids.get(packet.getClass());

        if (id == null)
            throw new IllegalStateException("Unregistered packet " + packet.getClass().getName());

        return id;
    }

    private FriendlyByteBuf encode(WDPacket packet) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        packet.encode(buf);
        return buf;
    }

    @Override
    public void sendToServer(WDPacket packet) {
        ClientPlayNetworking.send(idOf(packet), encode(packet));
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

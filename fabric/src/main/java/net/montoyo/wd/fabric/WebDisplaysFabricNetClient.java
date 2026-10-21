package net.montoyo.wd.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.montoyo.wd.platform.WDPacket;

import java.util.Map;
import java.util.function.Function;

/**
 * Client-only half of {@link WebDisplaysFabricNet}.
 * <p>
 * Kept in its own class on purpose: this file references
 * {@code net.fabricmc.fabric.api.client.networking}, and Fabric's class loader throws when such a
 * class is touched in a {@code SERVER} environment.
 */
final class WebDisplaysFabricNetClient {

    <T extends WDPacket> void registerReceiver(ResourceLocation id, Function<FriendlyByteBuf, T> decoder) {
        ClientPlayNetworking.registerGlobalReceiver(id, (client, handler, buf, sender) ->
                client.execute(() -> decoder.apply(buf).handle(client.player)));
    }

    void replay(Map<ResourceLocation, Function<FriendlyByteBuf, WDPacket>> decoders) {
        decoders.forEach(this::registerReceiver);
    }

    void send(ResourceLocation id, FriendlyByteBuf buf) {
        ClientPlayNetworking.send(id, buf);
    }
}

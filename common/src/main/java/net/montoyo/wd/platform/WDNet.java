package net.montoyo.wd.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.function.Function;

/**
 * Networking surface, implemented once per loader.
 * <p>
 * Packets are plain POJOs in {@code net.montoyo.wd.net}; each loader registers a serializer for them
 * and routes delivery through its own channel implementation.
 */
public interface WDNet {

    /**
     * Registers a packet type. {@code decoder} must be able to rebuild the packet from the buffer.
     */
    <T extends WDPacket> void register(ResourceLocation id, Class<T> type, Function<FriendlyByteBuf, T> decoder);

    void sendToServer(WDPacket packet);

    void sendToPlayer(WDPacket packet, ServerPlayer player);

    /**
     * Sends to every player that has the chunk containing {@code pos} loaded, within {@code radius}.
     */
    void sendToAllAround(WDPacket packet, BlockPos pos, double radius);

    void sendToAll(WDPacket packet);

    /**
     * Convenience: sends to {@code player} when it is a {@link ServerPlayer}, otherwise to the server.
     */
    default void sendTo(WDPacket packet, Player player) {
        if (player instanceof ServerPlayer sp) {
            sendToPlayer(packet, sp);
        } else {
            sendToServer(packet);
        }
    }
}

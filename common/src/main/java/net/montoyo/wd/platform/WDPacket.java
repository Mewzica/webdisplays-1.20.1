package net.montoyo.wd.platform;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

/**
 * A single network message. Loaders only need to know how to move the bytes around; the side-specific
 * logic lives in {@link #handle(Player)}.
 */
public interface WDPacket {

    void encode(FriendlyByteBuf buf);

    /**
     * Runs on the network thread of the receiving side.
     */
    void handle(Player sender);
}

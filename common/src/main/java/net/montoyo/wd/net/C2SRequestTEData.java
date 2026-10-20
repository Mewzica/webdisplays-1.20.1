package net.montoyo.wd.net;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.platform.Platform;
import net.montoyo.wd.platform.WDPacket;
import net.montoyo.wd.utilities.Vector3i;

/**
 * Client asks the server for the full state of a screen block entity, right after the chunk that holds
 * it was loaded.
 */
public class C2SRequestTEData implements WDPacket {

    public Vector3i pos;

    public C2SRequestTEData() {
    }

    public C2SRequestTEData(Vector3i pos) {
        this.pos = pos;
    }

    public C2SRequestTEData(FriendlyByteBuf buf) {
        pos = new Vector3i(buf);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        pos.writeTo(buf);
    }

    @Override
    public void handle(Player sender) {
        if (!(sender instanceof ServerPlayer player))
            return;

        BlockPos bp = pos.toBlock();

        if (!player.level().isLoaded(bp))
            return;

        if (player.level().getBlockEntity(bp) instanceof ScreenBlockEntity tes)
            tes.sendFullState(player);
    }
}

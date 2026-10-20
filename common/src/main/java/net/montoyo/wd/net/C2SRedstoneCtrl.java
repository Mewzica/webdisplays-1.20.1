package net.montoyo.wd.net;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.montoyo.wd.entity.PeripheralBlockEntity;
import net.montoyo.wd.platform.WDPacket;
import net.montoyo.wd.utilities.Vector3i;

/**
 * Redstone controller configuration, sent when the player toggles a channel in its GUI.
 */
public class C2SRedstoneCtrl implements WDPacket {

    public Vector3i pos;
    public int index;
    public boolean enabled;

    public C2SRedstoneCtrl() {
    }

    public C2SRedstoneCtrl(Vector3i pos, int index, boolean enabled) {
        this.pos = pos;
        this.index = index;
        this.enabled = enabled;
    }

    public C2SRedstoneCtrl(FriendlyByteBuf buf) {
        pos = new Vector3i(buf);
        index = buf.readByte();
        enabled = buf.readBoolean();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        pos.writeTo(buf);
        buf.writeByte(index);
        buf.writeBoolean(enabled);
    }

    @Override
    public void handle(Player sender) {
        if (!(sender instanceof ServerPlayer player))
            return;

        BlockPos bp = pos.toBlock();

        if (!player.level().isLoaded(bp))
            return;

        if (player.level().getBlockEntity(bp) instanceof PeripheralBlockEntity peripheral)
            peripheral.setRedstoneChannel(player, index, enabled);
    }
}

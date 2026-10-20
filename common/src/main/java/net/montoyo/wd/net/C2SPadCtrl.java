package net.montoyo.wd.net;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.montoyo.wd.entity.PeripheralBlockEntity;
import net.montoyo.wd.platform.WDPacket;
import net.montoyo.wd.utilities.Vector2i;
import net.montoyo.wd.utilities.Vector3i;

/**
 * minePad input relayed from the client GUI to the server-side keyboard peripheral.
 */
public class C2SPadCtrl implements WDPacket {

    public static final int CLICK = 0;
    public static final int TYPE = 1;
    public static final int KEY_EVENT = 2;

    public Vector3i pos;
    public int action;
    public Vector2i vec;
    public String str;

    public C2SPadCtrl() {
    }

    public C2SPadCtrl(FriendlyByteBuf buf) {
        pos = new Vector3i(buf);
        action = buf.readByte();

        if (action == CLICK || action == KEY_EVENT)
            vec = new Vector2i(buf);

        if (action == TYPE || action == KEY_EVENT)
            str = buf.readUtf(32767);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        pos.writeTo(buf);
        buf.writeByte(action);

        if (action == CLICK || action == KEY_EVENT)
            vec.writeTo(buf);

        if (action == TYPE || action == KEY_EVENT)
            buf.writeUtf(str == null ? "" : str, 32767);
    }

    @Override
    public void handle(Player sender) {
        if (!(sender instanceof ServerPlayer player))
            return;

        BlockPos bp = pos.toBlock();

        if (!player.level().isLoaded(bp))
            return;

        if (player.level().getBlockEntity(bp) instanceof PeripheralBlockEntity peripheral)
            peripheral.handlePadInput(player, this);
    }
}

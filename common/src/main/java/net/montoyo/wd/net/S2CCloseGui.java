package net.montoyo.wd.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.montoyo.wd.client.ClientGuiOpener;
import net.montoyo.wd.platform.Platform;
import net.montoyo.wd.platform.WDPacket;
import net.montoyo.wd.utilities.Vector3i;

/**
 * Tells the client to close whatever WebDisplays screen is open, because the multiblock behind it was
 * destroyed.
 */
public class S2CCloseGui implements WDPacket {

    public Vector3i pos;

    public S2CCloseGui() {
    }

    public S2CCloseGui(Vector3i pos) {
        this.pos = pos;
    }

    public S2CCloseGui(FriendlyByteBuf buf) {
        pos = new Vector3i(buf);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        pos.writeTo(buf);
    }

    @Override
    public void handle(Player sender) {
        if (Platform.get().isClient())
            ClientGuiOpener.close(pos);
    }
}

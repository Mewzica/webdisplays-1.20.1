package net.montoyo.wd.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.montoyo.wd.client.ClientScreenUpdater;
import net.montoyo.wd.platform.Platform;
import net.montoyo.wd.platform.WDPacket;

/**
 * Sent on join: tells the client which port miniserv listens on (0 = disabled), so the browser side
 * can reach {@code wd://} URLs.
 */
public class S2CServerInfo implements WDPacket {

    public int miniservPort;

    public S2CServerInfo() {
    }

    public S2CServerInfo(int port) {
        miniservPort = port;
    }

    public S2CServerInfo(FriendlyByteBuf buf) {
        miniservPort = buf.readInt();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(miniservPort);
    }

    @Override
    public void handle(Player sender) {
        if (Platform.get().isClient())
            ClientScreenUpdater.handleServerInfo(this);
    }
}

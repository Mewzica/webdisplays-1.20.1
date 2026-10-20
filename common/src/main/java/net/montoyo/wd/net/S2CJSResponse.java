package net.montoyo.wd.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.montoyo.wd.client.ClientScreenUpdater;
import net.montoyo.wd.platform.Platform;
import net.montoyo.wd.platform.WDPacket;

/**
 * Reply to a {@code webdisplaysRequest(...)} call made from the browser's JavaScript side.
 */
public class S2CJSResponse implements WDPacket {

    public int reqId;
    public int status;
    public String message;
    public byte[] payload;

    public S2CJSResponse() {
    }

    public S2CJSResponse(int reqId, int status, String message) {
        this.reqId = reqId;
        this.status = status;
        this.message = message;
        this.payload = new byte[0];
    }

    public S2CJSResponse(int reqId, int status, byte[] payload) {
        this.reqId = reqId;
        this.status = status;
        this.message = "";
        this.payload = payload;
    }

    public S2CJSResponse(FriendlyByteBuf buf) {
        reqId = buf.readInt();
        status = buf.readInt();
        message = buf.readUtf(32767);
        payload = buf.readByteArray(1 << 20);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(reqId);
        buf.writeInt(status);
        buf.writeUtf(message == null ? "" : message, 32767);
        buf.writeByteArray(payload == null ? new byte[0] : payload);
    }

    @Override
    public void handle(Player sender) {
        if (Platform.get().isClient())
            ClientScreenUpdater.handleJsResponse(this);
    }
}

package net.montoyo.wd.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.montoyo.wd.client.ClientGuiOpener;
import net.montoyo.wd.platform.Platform;
import net.montoyo.wd.platform.WDPacket;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Vector3i;

/**
 * Asks the client to open one of WebDisplays' screens.
 * <p>
 * Replaces the 1.12.2 {@code GuiData} hierarchy ({@code SetURLData}, {@code ScreenConfigData},
 * {@code KeyboardData}, {@code RedstoneCtrlData}, {@code ServerData}), which each had their own
 * message class and serializer.
 */
public class S2COpenGui implements WDPacket {

    public static final String URL = "url";
    public static final String SCREEN_CONFIG = "screen_config";
    public static final String KEYBOARD = "keyboard";
    public static final String REDSTONE_CTRL = "redstone_ctrl";
    public static final String SERVER = "server";

    public Vector3i pos;
    public BlockSide side;
    public String gui;
    /** Extra payload, already JSON-encoded by the sender. */
    public String data;

    public S2COpenGui() {
    }

    public S2COpenGui(Vector3i pos, BlockSide side, String gui, String data) {
        this.pos = pos;
        this.side = side;
        this.gui = gui;
        this.data = data;
    }

    public S2COpenGui(FriendlyByteBuf buf) {
        pos = new Vector3i(buf);
        side = BlockSide.fromInt(buf.readByte());
        gui = buf.readUtf();
        data = buf.readUtf(32767);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        pos.writeTo(buf);
        buf.writeByte(side.ordinal());
        buf.writeUtf(gui);
        buf.writeUtf(data, 32767);
    }

    @Override
    public void handle(Player sender) {
        if (Platform.get().isClient())
            ClientGuiOpener.open(this);
    }
}

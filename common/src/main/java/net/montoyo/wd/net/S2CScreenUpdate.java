package net.montoyo.wd.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.montoyo.wd.client.ClientScreenUpdater;
import net.montoyo.wd.platform.Platform;
import net.montoyo.wd.platform.WDPacket;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.NameUUIDPair;
import net.montoyo.wd.utilities.Vector2i;
import net.montoyo.wd.utilities.Vector3i;

/**
 * Server to client screen mutations.
 * <p>
 * In 1.12.2 this was {@code CMessageScreenUpdate} with a dozen static factories, plus
 * {@code CMessageAddScreen} for the initial sync. Both are folded into one discriminator here.
 */
public class S2CScreenUpdate implements WDPacket {

    public static final int ADD = 0;
    public static final int REMOVE = 1;
    public static final int SET_URL = 2;
    public static final int SET_RESOLUTION = 3;
    public static final int SET_ROTATION = 4;
    public static final int SET_OWNER = 5;
    public static final int SET_AUTO_VOLUME = 6;
    public static final int UPGRADES = 7;
    public static final int CLICK = 8;
    public static final int TYPE = 9;
    public static final int RUN_JS = 10;
    public static final int JS_REDSTONE = 11;

    public static final int MOUSE_CLICK = 0;
    public static final int MOUSE_DOWN = 1;
    public static final int MOUSE_UP = 2;
    public static final int MOUSE_MOVE = 3;

    public int action;
    public Vector3i pos;
    public BlockSide side;

    /** ADD / UPGRADES: screen snapshot. */
    public net.minecraft.nbt.CompoundTag tag;
    public String str;
    public Vector2i vec;
    public NameUUIDPair owner;
    public int a;
    public int b;

    public S2CScreenUpdate() {
    }

    public S2CScreenUpdate(FriendlyByteBuf buf) {
        action = buf.readByte();
        pos = new Vector3i(buf);
        side = BlockSide.fromInt(buf.readByte());

        switch (action) {
            case ADD:
                tag = buf.readNbt();
                break;
            case SET_URL:
            case RUN_JS:
                str = buf.readUtf(1 << 20);
                break;
            case TYPE:
                str = buf.readUtf(1 << 20);
                break;
            case SET_RESOLUTION:
                vec = new Vector2i(buf);
                break;
            case SET_ROTATION:
            case SET_AUTO_VOLUME:
                a = buf.readByte();
                break;
            case SET_OWNER:
                owner = new NameUUIDPair(buf);
                break;
            case UPGRADES:
                tag = buf.readNbt();
                break;
            case CLICK:
                a = buf.readByte();
                if (a != MOUSE_UP)
                    vec = new Vector2i(buf);
                break;
            case JS_REDSTONE:
                vec = new Vector2i(buf);
                a = buf.readByte();
                break;
            default:
                break;
        }
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(action);
        pos.writeTo(buf);
        buf.writeByte(side.ordinal());

        switch (action) {
            case ADD:
            case UPGRADES:
                buf.writeNbt(tag);
                break;
            case SET_URL:
            case RUN_JS:
            case TYPE:
                buf.writeUtf(str == null ? "" : str, 1 << 20);
                break;
            case SET_RESOLUTION:
                vec.writeTo(buf);
                break;
            case SET_ROTATION:
            case SET_AUTO_VOLUME:
                buf.writeByte(a);
                break;
            case SET_OWNER:
                owner.writeTo(buf);
                break;
            case CLICK:
                buf.writeByte(a);
                if (a != MOUSE_UP)
                    vec.writeTo(buf);
                break;
            case JS_REDSTONE:
                vec.writeTo(buf);
                buf.writeByte(a);
                break;
            default:
                break;
        }
    }

    @Override
    public void handle(Player sender) {
        if (Platform.get().isClient())
            ClientScreenUpdater.handle(this);
    }

    // ---- factories -------------------------------------------------------

    public static S2CScreenUpdate remove(Vector3i pos, BlockSide side) {
        S2CScreenUpdate p = base(REMOVE, pos, side);
        return p;
    }

    public static S2CScreenUpdate setUrl(Vector3i pos, BlockSide side, String url) {
        S2CScreenUpdate p = base(SET_URL, pos, side);
        p.str = url;
        return p;
    }

    public static S2CScreenUpdate setResolution(Vector3i pos, BlockSide side, Vector2i res) {
        S2CScreenUpdate p = base(SET_RESOLUTION, pos, side);
        p.vec = res;
        return p;
    }

    public static S2CScreenUpdate setRotation(Vector3i pos, BlockSide side, int rotation) {
        S2CScreenUpdate p = base(SET_ROTATION, pos, side);
        p.a = rotation;
        return p;
    }

    public static S2CScreenUpdate setOwner(Vector3i pos, BlockSide side, NameUUIDPair owner) {
        S2CScreenUpdate p = base(SET_OWNER, pos, side);
        p.owner = owner;
        return p;
    }

    public static S2CScreenUpdate setAutoVolume(Vector3i pos, BlockSide side, boolean av) {
        S2CScreenUpdate p = base(SET_AUTO_VOLUME, pos, side);
        p.a = av ? 1 : 0;
        return p;
    }

    public static S2CScreenUpdate click(Vector3i pos, BlockSide side, int action, Vector2i vec) {
        S2CScreenUpdate p = base(CLICK, pos, side);
        p.a = action;
        p.vec = vec;
        return p;
    }

    public static S2CScreenUpdate type(Vector3i pos, BlockSide side, String text) {
        S2CScreenUpdate p = base(TYPE, pos, side);
        p.str = text;
        return p;
    }

    public static S2CScreenUpdate runJs(Vector3i pos, BlockSide side, String code) {
        S2CScreenUpdate p = base(RUN_JS, pos, side);
        p.str = code;
        return p;
    }

    public static S2CScreenUpdate jsRedstone(Vector3i pos, BlockSide side, Vector2i vec, int level) {
        S2CScreenUpdate p = base(JS_REDSTONE, pos, side);
        p.vec = vec;
        p.a = level;
        return p;
    }

    private static S2CScreenUpdate base(int action, Vector3i pos, BlockSide side) {
        S2CScreenUpdate p = new S2CScreenUpdate();
        p.action = action;
        p.pos = pos;
        p.side = side;
        return p;
    }
}

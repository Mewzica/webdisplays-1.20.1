package net.montoyo.wd.net;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.platform.WDPacket;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.NameUUIDPair;
import net.montoyo.wd.utilities.Rotation;
import net.montoyo.wd.utilities.Util;
import net.montoyo.wd.utilities.Vector2i;
import net.montoyo.wd.utilities.Vector3i;

/**
 * Client to server screen mutations coming from the config GUI.
 */
public class C2SScreenAction implements WDPacket {

    public static final int SET_URL = 0;
    public static final int SET_RESOLUTION = 1;
    public static final int SET_ROTATION = 2;
    public static final int SET_AUTO_VOLUME = 3;
    public static final int ADD_FRIEND = 4;
    public static final int REMOVE_FRIEND = 5;
    public static final int SET_RIGHTS = 6;
    public static final int SET_OWNER = 7;

    public int action;
    public Vector3i pos;
    public BlockSide side;
    public String str;
    public Vector2i vec;
    public NameUUIDPair friend;
    public int a;
    public int b;

    public C2SScreenAction() {
    }

    public C2SScreenAction(FriendlyByteBuf buf) {
        action = buf.readByte();
        pos = new Vector3i(buf);
        side = BlockSide.fromInt(buf.readByte());

        switch (action) {
            case SET_URL:
                str = buf.readUtf(32767);
                break;
            case SET_RESOLUTION:
                vec = new Vector2i(buf);
                break;
            case SET_ROTATION:
            case SET_AUTO_VOLUME:
                a = buf.readByte();
                break;
            case ADD_FRIEND:
            case REMOVE_FRIEND:
                friend = new NameUUIDPair(buf);
                break;
            case SET_RIGHTS:
                a = buf.readByte();
                b = buf.readByte();
                break;
            case SET_OWNER:
                friend = new NameUUIDPair(buf);
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
            case SET_URL:
                buf.writeUtf(str == null ? "" : str, 32767);
                break;
            case SET_RESOLUTION:
                vec.writeTo(buf);
                break;
            case SET_ROTATION:
            case SET_AUTO_VOLUME:
                buf.writeByte(a);
                break;
            case ADD_FRIEND:
            case REMOVE_FRIEND:
            case SET_OWNER:
                friend.writeTo(buf);
                break;
            case SET_RIGHTS:
                buf.writeByte(a);
                buf.writeByte(b);
                break;
            default:
                break;
        }
    }

    @Override
    public void handle(Player sender) {
        if (!(sender instanceof ServerPlayer player))
            return;

        BlockPos bp = pos.toBlock();

        if (!player.level().isLoaded(bp) || !(player.level().getBlockEntity(bp) instanceof ScreenBlockEntity tes))
            return;

        ScreenBlockEntity.Screen scr = tes.getScreen(side);

        if (scr == null)
            return;

        int rights = scr.rightsFor(player);

        switch (action) {
            case SET_URL:
                if ((rights & ScreenRights.CHANGE_URL) == 0) {
                    Util.toast(player, "restrictions");
                    return;
                }

                tes.setScreenURL(side, str);
                break;

            case SET_RESOLUTION:
                if ((rights & ScreenRights.CHANGE_RESOLUTION) == 0) {
                    Util.toast(player, "restrictions");
                    return;
                }

                tes.setResolution(side, vec);
                break;

            case SET_ROTATION:
                if ((rights & ScreenRights.CHANGE_RESOLUTION) == 0) {
                    Util.toast(player, "restrictions");
                    return;
                }

                tes.setRotation(side, Rotation.values()[a]);
                break;

            case SET_AUTO_VOLUME:
                tes.setAutoVolume(side, a != 0);
                break;

            case ADD_FRIEND:
                if ((rights & ScreenRights.MANAGE_FRIEND_LIST) == 0) {
                    Util.toast(player, "restrictions");
                    return;
                }

                tes.addFriend(player, side, friend);
                break;

            case REMOVE_FRIEND:
                if ((rights & ScreenRights.MANAGE_FRIEND_LIST) == 0) {
                    Util.toast(player, "restrictions");
                    return;
                }

                tes.removeFriend(player, side, friend);
                break;

            case SET_RIGHTS:
                if ((rights & ScreenRights.MANAGE_OTHER_RIGHTS) == 0) {
                    Util.toast(player, "restrictions");
                    return;
                }

                tes.setRights(player, side, a, b);
                break;

            case SET_OWNER:
                if (!scr.owner.uuid.equals(player.getUUID())) {
                    Util.toast(player, "restrictions");
                    return;
                }

                tes.setOwner(side, player);
                break;

            default:
                break;
        }
    }

    public static C2SScreenAction of(int action, Vector3i pos, BlockSide side) {
        C2SScreenAction p = new C2SScreenAction();
        p.action = action;
        p.pos = pos;
        p.side = side;
        return p;
    }
}

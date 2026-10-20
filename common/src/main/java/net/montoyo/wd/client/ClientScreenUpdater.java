/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.net.S2CJSResponse;
import net.montoyo.wd.net.S2CScreenUpdate;
import net.montoyo.wd.net.S2CServerInfo;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Rotation;

/**
 * Applies server-driven screen mutations on the client.
 */
public final class ClientScreenUpdater {

    /** miniserv port announced by the server, 0 when disabled. */
    public static int miniservPort = 0;

    private ClientScreenUpdater() {
    }

    public static void handle(S2CScreenUpdate msg) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null)
            return;

        BlockPos bp = msg.pos.toBlock();

        if (!mc.level.isLoaded(bp))
            return;

        if (!(mc.level.getBlockEntity(bp) instanceof ScreenBlockEntity tes))
            return;

        switch (msg.action) {
            case S2CScreenUpdate.ADD -> {
                if (msg.tag != null)
                    tes.applyClientScreen(msg.side, msg.tag);
            }
            case S2CScreenUpdate.REMOVE -> tes.removeClientScreen(msg.side);
            case S2CScreenUpdate.SET_URL -> tes.setScreenURL(msg.side, msg.str);
            case S2CScreenUpdate.SET_RESOLUTION -> {
                if (msg.vec != null)
                    tes.setResolution(msg.side, msg.vec);
            }
            case S2CScreenUpdate.SET_ROTATION -> tes.setRotation(msg.side, Rotation.values()[msg.a]);
            case S2CScreenUpdate.SET_AUTO_VOLUME -> tes.setAutoVolume(msg.side, msg.a != 0);
            case S2CScreenUpdate.SET_OWNER -> {
                ScreenBlockEntity.Screen scr = tes.getScreen(msg.side);

                if (scr != null && msg.owner != null) {
                    scr.owner = msg.owner;
                    ClientGuiOpener.updateOpenGui(msg.pos, msg.side);
                }
            }
            case S2CScreenUpdate.CLICK -> tes.handleMouseEvent(msg.side, msg.a, msg.vec);
            case S2CScreenUpdate.TYPE -> tes.type(msg.side, msg.str, null);
            case S2CScreenUpdate.RUN_JS -> tes.evalJS(msg.side, msg.str);
            case S2CScreenUpdate.JS_REDSTONE -> {
                if (msg.vec != null)
                    tes.updateJSRedstone(msg.side, msg.vec, msg.a);
            }
            default -> {
            }
        }
    }

    public static void handleServerInfo(S2CServerInfo msg) {
        miniservPort = msg.miniservPort;
    }

    public static void handleJsResponse(S2CJSResponse msg) {
        // Responses are delivered to the browser that issued the query; the 1.20.1 port only logs them
        // because the JS query dispatcher is not ported yet.
        net.montoyo.wd.utilities.Log.info("JS response #%d: status=%d %s", msg.reqId, msg.status, msg.message);
    }

    /**
     * Convenience for the GUI: re-reads the screen state after the server changed it.
     */
    public static void refresh(BlockPos pos, BlockSide side) {
        ClientGuiOpener.updateOpenGui(new net.montoyo.wd.utilities.Vector3i(pos), side);
    }
}

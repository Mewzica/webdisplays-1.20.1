/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.montoyo.wd.client.gui.MinePadScreen;
import net.montoyo.wd.client.gui.RedstoneCtrlScreen;
import net.montoyo.wd.client.gui.ScreenConfigScreen;
import net.montoyo.wd.client.gui.UrlScreen;
import net.montoyo.wd.net.S2COpenGui;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.Vector3i;

/**
 * Turns a {@link S2COpenGui} into an actual client screen.
 * <p>
 * 1.12.2 spread this across {@code GuiData#openClientGui} on every data class.
 */
public final class ClientGuiOpener {

    private ClientGuiOpener() {
    }

    public static void open(S2COpenGui p) {
        Minecraft mc = Minecraft.getInstance();

        try {
            switch (p.gui) {
                case S2COpenGui.URL -> mc.setScreen(new UrlScreen(p.pos, p.side, p.data == null ? "" : p.data));

                case S2COpenGui.SCREEN_CONFIG -> {
                    JsonObject obj = p.data == null || p.data.isEmpty()
                            ? new JsonObject()
                            : JsonParser.parseString(p.data).getAsJsonObject();
                    mc.setScreen(new ScreenConfigScreen(p.pos, p.side, obj));
                }

                case S2COpenGui.REDSTONE_CTRL -> mc.setScreen(new RedstoneCtrlScreen(p.pos));

                case S2COpenGui.KEYBOARD ->
                        net.montoyo.wd.utilities.Util.toast(mc.player, "keyboardNoGui");

                case S2COpenGui.SERVER ->
                        net.montoyo.wd.utilities.Util.toast(mc.player, "serverNoGui");

                default -> Log.warning("Unknown GUI id '%s'", p.gui);
            }
        } catch (Throwable t) {
            Log.warningEx("Could not open GUI '%s'", t, p.gui);
        }
    }

    public static void openMinePad(ItemStack stack, int padId) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new MinePadScreen(stack, padId));
    }

    public static void close(Vector3i pos) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.screen instanceof net.montoyo.wd.client.gui.WDScreen wds && wds.isFor(pos))
            mc.setScreen(null);
    }

    /**
     * Called when the server pushed a new screen state and a config GUI is open on that screen.
     */
    public static void updateOpenGui(Vector3i pos, BlockSide side) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.screen instanceof net.montoyo.wd.client.gui.WDScreen wds && wds.isFor(pos))
            wds.onServerUpdate();
    }
}

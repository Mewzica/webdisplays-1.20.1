/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.montoyo.wd.utilities.Vector3i;
import org.jetbrains.annotations.Nullable;

/**
 * Common base for the ported GUIs. 1.12.2 had its own widget toolkit under {@code client/gui/controls};
 * the port uses vanilla widgets instead, so this base only carries the screen position bookkeeping.
 */
public abstract class WDScreen extends Screen {

    @Nullable
    protected final Vector3i pos;

    protected WDScreen(Component title, @Nullable Vector3i pos) {
        super(title);
        this.pos = pos;
    }

    public boolean isFor(Vector3i other) {
        return pos != null && pos.equals(other);
    }

    /**
     * Called when the server pushed a new state for the screen this GUI edits.
     */
    public void onServerUpdate() {
    }
}

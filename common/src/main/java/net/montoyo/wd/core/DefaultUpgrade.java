/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import net.minecraft.world.item.ItemStack;
import net.montoyo.wd.item.UpgradeItem;

public enum DefaultUpgrade {

    LASER_MOUSE("lasermouse", "Laser_Sensor"),
    REDSTONE_INPUT("redinput", "Redstone_Input_Port"),
    REDSTONE_OUTPUT("redoutput", "Redstone_Output_Port"),
    GPS("gps", "GPS_Module");

    private final String name;
    private final String wikiName;

    DefaultUpgrade(String n, String wn) {
        name = n;
        wikiName = wn;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * 1.12.2 stored the variant in the item metadata. 1.20.1 has no metadata, so the variant lives in
     * the stack's NBT ({@code WDUpgrade}); see {@link UpgradeItem}.
     */
    public boolean matches(ItemStack is) {
        return !is.isEmpty() && is.getItem() instanceof UpgradeItem && UpgradeItem.getUpgrade(is) == this;
    }

    public static String getWikiName(int meta) {
        DefaultUpgrade[] values = values();
        return (meta >= 0 && meta < values.length) ? values[meta].wikiName : null;
    }

    public String getName() {
        return name;
    }

    public String getWikiName() {
        return wikiName;
    }
}

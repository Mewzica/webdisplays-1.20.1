/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * The peripheral block variants.
 * <p>
 * 1.12.2 selected the variant with item metadata and registered one {@code TileEntity} class per
 * variant. 1.20.1 needs a single block entity type per block, so every variant shares
 * {@link net.montoyo.wd.entity.PeripheralBlockEntity} and the variant is a block state property.
 * <p>
 * {@link #CC_INTERFACE} and {@link #OC_INTERFACE} are kept so the ordinals stay stable, but they are
 * inert: ComputerCraft and OpenComputers support was dropped from the 1.20.1 port.
 */
public enum DefaultPeripheral implements StringRepresentable {

    KEYBOARD("keyboard", "Keyboard", true),
    CC_INTERFACE("ccinterface", "ComputerCraft_Interface", true),
    OC_INTERFACE("cointerface", "OpenComputers_Interface", true),
    REMOTE_CONTROLLER("remotectrl", "Remote_Controller", false),
    REDSTONE_CONTROLLER("redstonectrl", "Redstone_Controller", false),
    SERVER("server", "Server", false);

    private final String name;
    private final String wikiName;
    private final boolean hasFacing;

    DefaultPeripheral(String name, String wikiName, boolean hasFacing) {
        this.name = name;
        this.wikiName = wikiName;
        this.hasFacing = hasFacing;
    }

    @Override
    @NotNull
    public String getSerializedName() {
        return name;
    }

    public String getName() {
        return name;
    }

    public String getWikiName() {
        return wikiName;
    }

    public boolean hasFacing() {
        return hasFacing;
    }

    public boolean isImplemented() {
        return this != CC_INTERFACE && this != OC_INTERFACE;
    }

    public static DefaultPeripheral fromMetadata(int meta) {
        if ((meta & 3) == 3)
            return values()[(((meta >> 2) & 3) | 4) - 1]; //Without facing
        else
            return values()[meta & 3]; //With facing
    }

    public int toMetadata(int facing) {
        int ret = ordinal();

        if (ret < 3) //With facing
            ret |= facing << 2;
        else //Without facing
            ret = (((ret + 1) & 3) << 2) | 3;

        return ret;
    }
}

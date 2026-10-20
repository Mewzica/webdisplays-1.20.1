/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Marker for WebDisplays items that only exists to add the "hold shift for the wiki" tooltip.
 */
public interface WDItem {

    default void addInformation(@NotNull List<Component> tooltip) {
        tooltip.add(Component.translatable("item.webdisplays.wiki").withStyle(ChatFormatting.GRAY));
    }

    default String getWikiName(ItemStack is) {
        return null;
    }
}

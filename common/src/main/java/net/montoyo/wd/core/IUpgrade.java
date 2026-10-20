/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.BlockSide;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IUpgrade {

    void onInstall(@NotNull ScreenBlockEntity tes, @NotNull BlockSide screenSide, @Nullable Player player, @NotNull ItemStack is);
    boolean onRemove(@NotNull ScreenBlockEntity tes, @NotNull BlockSide screenSide, @Nullable Player player, @NotNull ItemStack is); //Return true to prevent dropping
    boolean isSameUpgrade(@NotNull ItemStack myStack, @NotNull ItemStack otherStack); //myStack.getItem() is an instance of this class
    String getJSName(@NotNull ItemStack is); //modname:upgradename

}

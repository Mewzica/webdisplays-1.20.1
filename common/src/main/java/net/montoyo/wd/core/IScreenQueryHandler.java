/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import net.montoyo.mcef.api.IJSQueryCallback;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.BlockSide;

import org.jetbrains.annotations.NotNull;

public interface IScreenQueryHandler {

    //args is an array of Doubles or Strings
    //The screen DOES exist, so scr.getScreen(side) is never null
    void handleQuery(@NotNull IJSQueryCallback cb, @NotNull ScreenBlockEntity scr, @NotNull BlockSide side, @NotNull Object[] args);

}

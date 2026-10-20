/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.montoyo.wd.WDContent;
import net.montoyo.wd.core.DefaultUpgrade;
import net.montoyo.wd.core.IUpgrade;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.BlockSide;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The four screen upgrades, all sharing one item.
 * <p>
 * 1.12.2 used metadata; 1.20.1 has none, so the variant is stored under {@value #TAG}.
 */
public class UpgradeItem extends Item implements IUpgrade, WDItem {

    public static final String TAG = "WDUpgrade";

    public UpgradeItem() {
        super(new Item.Properties().stacksTo(64));
    }

    public static ItemStack create(DefaultUpgrade du) {
        ItemStack is = new ItemStack(WDContent.UPGRADE.get());
        CompoundTag tag = is.getOrCreateTag();
        tag.putString(TAG, du.getName());
        return is;
    }

    @Nullable
    public static DefaultUpgrade getUpgrade(ItemStack is) {
        if (is.isEmpty() || !(is.getItem() instanceof UpgradeItem) || !is.hasTag())
            return null;

        String name = is.getTag().getString(TAG);

        for (DefaultUpgrade du : DefaultUpgrade.values()) {
            if (du.getName().equals(name))
                return du;
        }

        return null;
    }

    @Override
    @NotNull
    public Component getName(@NotNull ItemStack is) {
        DefaultUpgrade du = getUpgrade(is);
        return du == null
                ? super.getName(is)
                : Component.translatable("item.webdisplays.upgrade." + du.getName() + ".name");
    }

    @Override
    public void onInstall(@NotNull ScreenBlockEntity tes, @NotNull BlockSide screenSide,
                          @Nullable Player player, @NotNull ItemStack is) {
    }

    @Override
    public boolean onRemove(@NotNull ScreenBlockEntity tes, @NotNull BlockSide screenSide,
                            @Nullable Player player, @NotNull ItemStack is) {
        if (getUpgrade(is) == DefaultUpgrade.LASER_MOUSE)
            tes.clearLaserUser(screenSide);

        return false;
    }

    @Override
    public boolean isSameUpgrade(@NotNull ItemStack myStack, @NotNull ItemStack otherStack) {
        return otherStack.getItem() == this && getUpgrade(otherStack) == getUpgrade(myStack);
    }

    @Override
    public String getJSName(@NotNull ItemStack is) {
        DefaultUpgrade du = getUpgrade(is);
        return "webdisplays:" + (du == null ? "wtf" : du.getName());
    }

    @Override
    public String getWikiName(ItemStack is) {
        DefaultUpgrade du = getUpgrade(is);
        return du == null ? null : du.getWikiName();
    }
}

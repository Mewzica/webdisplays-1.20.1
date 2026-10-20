/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.montoyo.wd.WDContent;
import net.montoyo.wd.core.CraftComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The crafting components, all sharing one item; the variant lives under {@value #TAG}.
 */
public class CraftComponentItem extends Item implements WDItem {

    public static final String TAG = "WDPart";

    public CraftComponentItem() {
        super(new Item.Properties().stacksTo(64));
    }

    public static ItemStack create(CraftComponent part) {
        ItemStack is = new ItemStack(WDContent.CRAFT_COMPONENT.get());
        CompoundTag tag = is.getOrCreateTag();
        tag.putString(TAG, part.getName());
        return is;
    }

    @Nullable
    public static CraftComponent getPart(ItemStack is) {
        if (is.isEmpty() || !(is.getItem() instanceof CraftComponentItem) || !is.hasTag())
            return null;

        String name = is.getTag().getString(TAG);

        for (CraftComponent part : CraftComponent.values()) {
            if (part.getName().equals(name))
                return part;
        }

        return null;
    }

    @Override
    @NotNull
    public Component getName(@NotNull ItemStack is) {
        CraftComponent part = getPart(is);
        return part == null
                ? super.getName(is)
                : Component.translatable("item.webdisplays.craftcomp." + part.getName() + ".name");
    }

    @Override
    public String getWikiName(ItemStack is) {
        CraftComponent part = getPart(is);
        return part == null ? null : part.getWikiName();
    }
}

/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.client.ClientGuiOpener;
import net.montoyo.wd.platform.Platform;
import net.montoyo.wd.utilities.Vector3i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The minePad: a handheld browser. Right-clicking opens the pad GUI, which hosts its own MCEF browser.
 */
public class MinePadItem extends Item implements WDItem {

    public static final String TAG_PAD_ID = "PadID";

    public MinePadItem() {
        super(new Item.Properties().stacksTo(1));
    }

    public static int getPadId(ItemStack is) {
        CompoundTag tag = is.getTag();
        return (tag != null && tag.contains(TAG_PAD_ID)) ? tag.getInt(TAG_PAD_ID) : -1;
    }

    public static void setPadId(ItemStack is, int id) {
        is.getOrCreateTag().putInt(TAG_PAD_ID, id);
    }

    @Override
    @NotNull
    public InteractionResultHolder<ItemStack> use(@NotNull Level world, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide)
            return InteractionResultHolder.success(stack);

        if (!Platform.get().isClient())
            return InteractionResultHolder.pass(stack);

        int id = getPadId(stack);

        if (id < 0) {
            // The server hands out the id on first toss/use; fall back to a locally derived one so the
            // pad is usable in single player before the server round trip.
            id = WebDisplays.getNextAvailablePadID();
            setPadId(stack, id);
        }

        ClientGuiOpener.openMinePad(stack, id);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level world, @NotNull List<Component> tt,
                                @NotNull TooltipFlag flags) {
        int id = getPadId(stack);

        if (id >= 0)
            tt.add(Component.translatable("webdisplays.minepad.id", id).withStyle(ChatFormatting.GRAY));

        addInformation(tt);
    }

    @Override
    public String getWikiName(ItemStack is) {
        return "MinePad";
    }
}

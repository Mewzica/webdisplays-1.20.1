/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.montoyo.wd.block.ScreenBlock;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.entity.PeripheralBlockEntity;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Multiblock;
import net.montoyo.wd.utilities.Util;
import net.montoyo.wd.utilities.Vector3i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Two-step linking tool: click a screen, then click a peripheral.
 */
public class LinkerItem extends Item implements WDItem {

    public LinkerItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    @NotNull
    public InteractionResult useOn(@NotNull UseOnContext ctx) {
        Level world = ctx.getLevel();
        BlockPos clicked = ctx.getClickedPos();
        Player player = ctx.getPlayer();

        if (player == null || world.isClientSide)
            return InteractionResult.SUCCESS;

        ItemStack stack = ctx.getItemInHand();
        CompoundTag tag = stack.getTag();

        // Second click: a screen was selected, so try to bind the peripheral.
        if (tag != null && tag.contains("ScreenX")) {
            BlockEntity be = world.getBlockEntity(clicked);

            if (!(be instanceof PeripheralBlockEntity peripheral)) {
                if (player.isShiftKeyDown()) {
                    Util.toast(player, ChatFormatting.GOLD, "linkAbort");
                    stack.setTag(null);
                } else
                    Util.toast(player, "peripheral");

                return InteractionResult.SUCCESS;
            }

            peripheral.link = new Vector3i(tag.getInt("ScreenX"), tag.getInt("ScreenY"), tag.getInt("ScreenZ"));
            peripheral.setChanged();
            Util.toast(player, ChatFormatting.AQUA, "linked");
            stack.setTag(null);
            return InteractionResult.SUCCESS;
        }

        // First click: select the screen.
        if (!(world.getBlockState(clicked).getBlock() instanceof ScreenBlock)) {
            Util.toast(player, "notAScreen");
            return InteractionResult.SUCCESS;
        }

        Vector3i pos = new Vector3i(clicked);
        BlockSide side = BlockSide.values()[ctx.getClickedFace().ordinal()];
        Multiblock.findOrigin(world, pos, side, null);

        if (!(world.getBlockEntity(pos.toBlock()) instanceof ScreenBlockEntity tes)) {
            Util.toast(player, "turnOn");
            return InteractionResult.SUCCESS;
        }

        ScreenBlockEntity.Screen scr = tes.getScreen(side);

        if (scr == null) {
            Util.toast(player, "turnOn");
        } else if ((scr.rightsFor(player) & ScreenRights.MANAGE_UPGRADES) == 0) {
            Util.toast(player, "restrictions");
        } else {
            CompoundTag nt = new CompoundTag();
            nt.putInt("ScreenX", pos.x);
            nt.putInt("ScreenY", pos.y);
            nt.putInt("ScreenZ", pos.z);
            nt.putByte("ScreenSide", (byte) side.ordinal());
            stack.setTag(nt);
            Util.toast(player, ChatFormatting.AQUA, "screenSet2");
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level world, @NotNull List<Component> tt,
                                @NotNull TooltipFlag flags) {
        CompoundTag tag = stack.getTag();

        if (tag != null && tag.contains("ScreenX")) {
            tt.add(Component.translatable("webdisplays.linker.selectPeripheral"));
            tt.add(Component.translatable("webdisplays.linker.posInfo",
                    tag.getInt("ScreenX"), tag.getInt("ScreenY"), tag.getInt("ScreenZ")));
        } else
            tt.add(Component.translatable("webdisplays.linker.selectScreen"));

        addInformation(tt);
    }

    @Override
    public String getWikiName(ItemStack is) {
        return "Linking_Tool";
    }
}

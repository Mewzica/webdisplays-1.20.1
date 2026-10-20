/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.montoyo.wd.block.ScreenBlock;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.net.S2COpenGui;
import net.montoyo.wd.platform.Platform;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Multiblock;
import net.montoyo.wd.utilities.Util;
import net.montoyo.wd.utilities.Vector3i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Right-click a screen with this to open its configuration GUI.
 */
public class ScreenConfiguratorItem extends Item implements WDItem {

    public ScreenConfiguratorItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    @NotNull
    public InteractionResult useOn(@NotNull UseOnContext ctx) {
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();

        if (player == null || player.isShiftKeyDown() || !(world.getBlockState(pos).getBlock() instanceof ScreenBlock))
            return InteractionResult.PASS;

        if (world.isClientSide)
            return InteractionResult.SUCCESS;

        Vector3i origin = new Vector3i(pos);
        BlockSide side = BlockSide.values()[ctx.getClickedFace().ordinal()];

        Multiblock.findOrigin(world, origin, side, null);
        BlockEntity be = world.getBlockEntity(origin.toBlock());

        if (!(be instanceof ScreenBlockEntity tes)) {
            Util.toast(player, "turnOn");
            return InteractionResult.SUCCESS;
        }

        ScreenBlockEntity.Screen scr = tes.getScreen(side);

        if (scr == null) {
            Util.toast(player, "turnOn");
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer sp)
            Platform.get().net().sendToPlayer(
                    new S2COpenGui(origin, side, S2COpenGui.SCREEN_CONFIG, tes.describeScreen(scr)), sp);

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack is, @Nullable Level world, @NotNull List<Component> tt,
                                @NotNull TooltipFlag flags) {
        addInformation(tt);
    }

    @Override
    public String getWikiName(ItemStack is) {
        return "Screen_Configurator";
    }
}

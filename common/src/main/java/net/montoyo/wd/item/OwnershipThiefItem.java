/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.montoyo.wd.WDConfig;
import net.montoyo.wd.block.ScreenBlock;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Multiblock;
import net.montoyo.wd.utilities.Util;
import net.montoyo.wd.utilities.Vector3i;
import org.jetbrains.annotations.NotNull;

/**
 * Takes ownership of a screen without asking.
 */
public class OwnershipThiefItem extends Item implements WDItem {

    public OwnershipThiefItem() {
        super(new Item.Properties().stacksTo(1).durability(16));
    }

    @Override
    @NotNull
    public InteractionResult useOn(@NotNull UseOnContext ctx) {
        Level world = ctx.getLevel();
        BlockPos clicked = ctx.getClickedPos();
        Player player = ctx.getPlayer();

        if (player == null)
            return InteractionResult.PASS;

        if (WDConfig.get().disableOwnershipThief) {
            if (!world.isClientSide)
                Util.toast(player, "thiefDisabled");

            return InteractionResult.SUCCESS;
        }

        if (world.isClientSide || !(world.getBlockState(clicked).getBlock() instanceof ScreenBlock))
            return InteractionResult.PASS;

        Vector3i origin = new Vector3i(clicked);
        BlockSide side = BlockSide.values()[ctx.getClickedFace().ordinal()];
        Multiblock.findOrigin(world, origin, side, null);

        if (!(world.getBlockEntity(origin.toBlock()) instanceof ScreenBlockEntity tes))
            return InteractionResult.PASS;

        if (tes.getScreen(side) == null)
            return InteractionResult.PASS;

        tes.setOwner(side, player);
        Util.toast(player, ChatFormatting.AQUA, "ownerChanged");

        ctx.getItemInHand().hurtAndBreak(1, player, p -> p.broadcastBreakEvent(ctx.getHand()));
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(@NotNull net.minecraft.world.item.ItemStack stack) {
        return true;
    }

    @Override
    public String getWikiName(net.minecraft.world.item.ItemStack is) {
        return "Ownership_Thief";
    }
}

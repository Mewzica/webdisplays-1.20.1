/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.montoyo.wd.block.ScreenBlock;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.platform.Platform;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Multiblock;
import net.montoyo.wd.utilities.Util;
import net.montoyo.wd.utilities.Vector2i;
import net.montoyo.wd.utilities.Vector3i;
import org.jetbrains.annotations.NotNull;

/**
 * Laser pointer: right-clicking a screen with the laser sensor upgrade injects a click at the aimed
 * pixel.
 */
public class LaserPointerItem extends Item implements WDItem {

    public LaserPointerItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    @NotNull
    public InteractionResultHolder<ItemStack> use(@NotNull Level world, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (world.isClientSide)
            return InteractionResultHolder.success(stack);

        HitResult hit = player.pick(32.0, 0.0f, false);

        if (!(hit instanceof BlockHitResult bhr) || hit.getType() != HitResult.Type.BLOCK)
            return InteractionResultHolder.pass(stack);

        BlockPos clicked = bhr.getBlockPos();

        if (!(world.getBlockState(clicked).getBlock() instanceof ScreenBlock))
            return InteractionResultHolder.pass(stack);

        Vector3i origin = new Vector3i(clicked);
        BlockSide side = BlockSide.values()[bhr.getDirection().ordinal()];
        Multiblock.findOrigin(world, origin, side, null);

        if (!(world.getBlockEntity(origin.toBlock()) instanceof ScreenBlockEntity tes))
            return InteractionResultHolder.pass(stack);

        ScreenBlockEntity.Screen scr = tes.getScreen(side);

        if (scr == null)
            return InteractionResultHolder.pass(stack);

        Vector2i px = new Vector2i();

        if (!ScreenBlock.hit2pixels(side, clicked, origin, scr,
                (float) bhr.getLocation().x - clicked.getX(),
                (float) bhr.getLocation().y - clicked.getY(),
                (float) bhr.getLocation().z - clicked.getZ(), px))
            return InteractionResultHolder.pass(stack);

        tes.laserDownMove(side, player, px, true);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public String getWikiName(ItemStack is) {
        return "Laser_Pointer";
    }
}

/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.network.chat.Component;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.montoyo.wd.block.PeripheralBlock;
import net.montoyo.wd.core.DefaultPeripheral;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Block item for one peripheral variant.
 * <p>
 * 1.12.2 shipped six metadata values of one item; 1.20.1 needs six items, each stamping its variant
 * into the block state when placed.
 */
public class PeripheralItem extends BlockItem implements WDItem {

    private final DefaultPeripheral kind;

    public PeripheralItem(Block block, DefaultPeripheral kind) {
        super(block, new Properties());
        this.kind = kind;
    }

    public DefaultPeripheral getKind() {
        return kind;
    }

    @Override
    @Nullable
    public BlockState getPlacementState(@NotNull BlockPlaceContext ctx) {
        BlockState state = super.getPlacementState(ctx);
        return state == null ? null : state.setValue(PeripheralBlock.KIND, kind);
    }

    @Override
    @NotNull
    public Component getName(@NotNull ItemStack is) {
        return Component.translatable("block.webdisplays.peripheral." + kind.getName());
    }

    @Override
    public String getWikiName(ItemStack is) {
        return kind.getWikiName();
    }
}

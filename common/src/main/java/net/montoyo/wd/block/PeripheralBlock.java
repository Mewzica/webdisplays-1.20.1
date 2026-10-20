/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.montoyo.wd.core.DefaultPeripheral;
import net.montoyo.wd.entity.PeripheralBlockEntity;
import net.montoyo.wd.net.S2COpenGui;
import net.montoyo.wd.platform.Platform;
import net.montoyo.wd.utilities.Util;
import net.montoyo.wd.utilities.Vector3i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One block for every peripheral variant; the variant is a block state property.
 * <p>
 * 1.12.2 used item metadata for this, which 1.20.1 no longer supports, so
 * {@link net.montoyo.wd.item.PeripheralItem} injects the variant at placement time.
 */
public class PeripheralBlock extends BaseEntityBlock {

    public static final EnumProperty<DefaultPeripheral> KIND =
            EnumProperty.create("kind", DefaultPeripheral.class);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public PeripheralBlock() {
        super(Properties.of().strength(1.5f, 10.f).noOcclusion());
        registerDefaultState(stateDefinition.any()
                .setValue(KIND, DefaultPeripheral.KEYBOARD)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(KIND, FACING);
    }

    @Override
    @NotNull
    public RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new PeripheralBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext ctx) {
        BlockState state = super.getStateForPlacement(ctx);

        if (state == null)
            return null;

        return state.setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state,
                            @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
    }

    @Override
    @NotNull
    public InteractionResult use(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos,
                                 @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (world.isClientSide)
            return InteractionResult.SUCCESS;

        if (!(world.getBlockEntity(pos) instanceof PeripheralBlockEntity peripheral))
            return InteractionResult.PASS;

        Vector3i vec = new Vector3i(pos);
        DefaultPeripheral kind = state.getValue(KIND);

        switch (kind) {
            case KEYBOARD -> Platform.get().net().sendToPlayer(
                    new S2COpenGui(vec, net.montoyo.wd.utilities.BlockSide.NORTH, S2COpenGui.KEYBOARD,
                            peripheral.link == null ? "" : peripheral.link.toString()),
                    (net.minecraft.server.level.ServerPlayer) player);

            case SERVER -> Platform.get().net().sendToPlayer(
                    new S2COpenGui(vec, net.montoyo.wd.utilities.BlockSide.NORTH, S2COpenGui.SERVER, ""),
                    (net.minecraft.server.level.ServerPlayer) player);

            case REDSTONE_CONTROLLER -> Platform.get().net().sendToPlayer(
                    new S2COpenGui(vec, net.montoyo.wd.utilities.BlockSide.NORTH, S2COpenGui.REDSTONE_CTRL, ""),
                    (net.minecraft.server.level.ServerPlayer) player);

            default -> Util.toast(player, "peripheralUnsupported");
        }

        return InteractionResult.SUCCESS;
    }
}

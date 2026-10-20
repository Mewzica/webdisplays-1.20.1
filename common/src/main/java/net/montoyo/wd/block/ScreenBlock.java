/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.montoyo.wd.WDConfig;
import net.montoyo.wd.WDContent;
import net.montoyo.wd.core.DefaultUpgrade;
import net.montoyo.wd.core.IUpgrade;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.net.S2COpenGui;
import net.montoyo.wd.platform.Platform;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.Multiblock;
import net.montoyo.wd.utilities.Util;
import net.montoyo.wd.utilities.Vector2i;
import net.montoyo.wd.utilities.Vector3f;
import net.montoyo.wd.utilities.Vector3i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The screen block. Placing a wall of them and right-clicking the face turns the wall into one screen.
 * <p>
 * Ported from {@code BlockScreen} (1.12.2). The unlisted block state properties that drove the border
 * icon in the baked model are gone; the border is drawn by the block entity renderer instead.
 */
public class ScreenBlock extends BaseEntityBlock {

    public static final BooleanProperty HAS_TE = BooleanProperty.create("haste");
    public static final BooleanProperty EMITTING = BooleanProperty.create("emitting");

    public ScreenBlock() {
        super(Properties.of()
                .strength(1.5f, 10.f)
                .pushReaction(PushReaction.BLOCK)
                .noOcclusion());

        registerDefaultState(stateDefinition.any()
                .setValue(HAS_TE, false)
                .setValue(EMITTING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(HAS_TE, EMITTING);
    }

    @Override
    @NotNull
    public RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return state.getValue(HAS_TE) ? new ScreenBlockEntity(pos, state) : null;
    }

    public static boolean isntScreenBlock(BlockGetter world, Vector3i pos) {
        return world.getBlockState(pos.toBlock()).getBlock() != WDContent.SCREEN_BLOCK.get();
    }

    @Override
    @NotNull
    public InteractionResult use(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos bpos,
                                 @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        ItemStack heldItem = player.getItemInHand(hand);

        if (!heldItem.isEmpty() && !(heldItem.getItem() instanceof IUpgrade))
            return InteractionResult.PASS;

        if (world.isClientSide)
            return InteractionResult.SUCCESS;

        boolean sneaking = player.isShiftKeyDown();
        Vector3i pos = new Vector3i(bpos);
        BlockSide side = BlockSide.values()[hit.getDirection().ordinal()];

        Multiblock.findOrigin(world, pos, side, null);
        BlockEntity be = world.getBlockEntity(pos.toBlock());
        ScreenBlockEntity tes = (be instanceof ScreenBlockEntity s) ? s : null;

        if (tes != null && tes.getScreen(side) != null) {
            ScreenBlockEntity.Screen scr = tes.getScreen(side);

            if (sneaking) { //Set URL
                if ((scr.rightsFor(player) & ScreenRights.CHANGE_URL) == 0)
                    Util.toast(player, "restrictions");
                else
                    Platform.get().net().sendToPlayer(
                            new S2COpenGui(new Vector3i(pos), scr.side, S2COpenGui.URL, scr.url == null ? "" : scr.url),
                            (net.minecraft.server.level.ServerPlayer) player);

                return InteractionResult.SUCCESS;
            } else if (!heldItem.isEmpty() && !tes.hasUpgrade(side, heldItem)) { //Add upgrade
                if ((scr.rightsFor(player) & ScreenRights.MANAGE_UPGRADES) == 0) {
                    Util.toast(player, "restrictions");
                    return InteractionResult.SUCCESS;
                }

                if (tes.addUpgrade(side, heldItem, player, false)) {
                    if (!player.getAbilities().instabuild)
                        heldItem.shrink(1);

                    Util.toast(player, ChatFormatting.AQUA, "upgradeOk");
                } else
                    Util.toast(player, "upgradeError");

                return InteractionResult.SUCCESS;
            } else { //Click
                if ((scr.rightsFor(player) & ScreenRights.CLICK) == 0) {
                    Util.toast(player, "restrictions");
                    return InteractionResult.SUCCESS;
                }

                Vector2i tmp = new Vector2i();

                if (hit2pixels(side, bpos, pos, scr, (float) hit.getLocation().x - bpos.getX(),
                        (float) hit.getLocation().y - bpos.getY(), (float) hit.getLocation().z - bpos.getZ(), tmp))
                    tes.click(side, tmp);

                return InteractionResult.SUCCESS;
            }
        } else if (sneaking) {
            Util.toast(player, "turnOn");
            return InteractionResult.SUCCESS;
        }

        WDConfig cfg = WDConfig.get();
        Vector2i size = Multiblock.measure(world, pos, side);

        if (size.x < 2 || size.y < 2) {
            Util.toast(player, "tooSmall");
            return InteractionResult.SUCCESS;
        }

        if (size.x > cfg.maxScreenSizeX || size.y > cfg.maxScreenSizeY) {
            Util.toast(player, "tooBig", cfg.maxScreenSizeX, cfg.maxScreenSizeY);
            return InteractionResult.SUCCESS;
        }

        Vector3i err = Multiblock.check(world, pos, size, side);

        if (err != null) {
            Util.toast(player, "invalid", err.toString());
            return InteractionResult.SUCCESS;
        }

        boolean created = false;
        Log.info("Player %s (UUID %s) created a screen at %s of size %dx%d",
                player.getName().getString(), player.getUUID(), pos.toString(), size.x, size.y);

        if (tes == null) {
            BlockPos bp = pos.toBlock();
            world.setBlock(bp, world.getBlockState(bp).setValue(HAS_TE, true), 3);
            be = world.getBlockEntity(bp);
            tes = (be instanceof ScreenBlockEntity s) ? s : null;
            created = true;
        }

        if (tes != null)
            tes.addScreen(side, size, null, player, !created);

        return InteractionResult.SUCCESS;
    }

    public static boolean hit2pixels(BlockSide side, BlockPos bpos, Vector3i pos, ScreenBlockEntity.Screen scr,
                                     float hitX, float hitY, float hitZ, Vector2i dst) {
        if (side.right.x < 0)
            hitX -= 1.f;

        if (side.right.z < 0 || side == BlockSide.TOP || side == BlockSide.BOTTOM)
            hitZ -= 1.f;

        Vector3f rel = new Vector3f(bpos.getX(), bpos.getY(), bpos.getZ());
        rel.sub((float) pos.x, (float) pos.y, (float) pos.z);
        rel.add(hitX, hitY, hitZ);

        float cx = rel.dot(side.right.toFloat()) - 2.f / 16.f;
        float cy = rel.dot(side.up.toFloat()) - 2.f / 16.f;
        float sw = ((float) scr.size.x) - 4.f / 16.f;
        float sh = ((float) scr.size.y) - 4.f / 16.f;

        cx /= sw;
        cy /= sh;

        if (cx >= 0.f && cx <= 1.0 && cy >= 0.f && cy <= 1.f) {
            if (side != BlockSide.BOTTOM)
                cy = 1.f - cy;

            switch (scr.rotation) {
                case ROT_90 -> cy = 1.0f - cy;
                case ROT_180 -> {
                    cx = 1.0f - cx;
                    cy = 1.0f - cy;
                }
                case ROT_270 -> cx = 1.0f - cx;
                default -> {
                }
            }

            cx *= (float) scr.resolution.x;
            cy *= (float) scr.resolution.y;

            if (scr.rotation.isVertical) {
                dst.x = (int) cy;
                dst.y = (int) cx;
            } else {
                dst.x = (int) cx;
                dst.y = (int) cy;
            }

            return true;
        }

        return false;
    }

    // ---- redstone --------------------------------------------------------

    @Override
    public boolean isSignalSource(@NotNull BlockState state) {
        return state.getValue(EMITTING);
    }

    @Override
    public int getSignal(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull Direction side) {
        return state.getValue(EMITTING) ? 15 : 0;
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos,
                                @NotNull Block block, @NotNull BlockPos source, boolean isMoving) {
        if (block == this || world.isClientSide || state.getValue(EMITTING))
            return;

        for (BlockSide side : BlockSide.values()) {
            Vector3i vec = new Vector3i(pos);
            Multiblock.findOrigin(world, vec, side, null);

            if (!(world.getBlockEntity(vec.toBlock()) instanceof ScreenBlockEntity tes))
                continue;

            if (!tes.hasUpgrade(side, DefaultUpgrade.REDSTONE_INPUT))
                continue;

            Direction facing = Direction.values()[side.reverse().ordinal()];
            vec.sub(pos.getX(), pos.getY(), pos.getZ()).neg();
            tes.updateJSRedstone(side, new Vector2i(vec.dot(side.right), vec.dot(side.up)),
                    world.getSignal(pos, facing));
        }
    }

    // ---- destruction -----------------------------------------------------

    private void onDestroy(Level world, BlockPos pos, @Nullable Player ply) {
        if (world.isClientSide)
            return;

        Vector3i bp = new Vector3i(pos);
        Multiblock.BlockOverride override = new Multiblock.BlockOverride(bp, Multiblock.OverrideAction.SIMULATE);

        for (BlockSide bs : BlockSide.values())
            destroySide(world, bp.clone(), bs, override, ply);
    }

    private void destroySide(Level world, Vector3i pos, BlockSide side, Multiblock.BlockOverride override,
                             @Nullable Player source) {
        Multiblock.findOrigin(world, pos, side, override);
        BlockPos bp = pos.toBlock();
        BlockEntity be = world.getBlockEntity(bp);

        if (be instanceof ScreenBlockEntity tes) {
            tes.onDestroy(source);
            world.setBlock(bp, world.getBlockState(bp).setValue(HAS_TE, false), 3);
        }
    }

    @Override
    public void playerWillDestroy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state,
                                  @NotNull Player player) {
        onDestroy(world, pos, player);
        super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    public void wasExploded(@NotNull Level world, @NotNull BlockPos pos, @NotNull Explosion explosion) {
        onDestroy(world, pos, null);
        super.wasExploded(world, pos, explosion);
    }

    @Override
    public void setPlacedBy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state,
                            @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);

        if (world.isClientSide)
            return;

        Multiblock.BlockOverride override = new Multiblock.BlockOverride(new Vector3i(pos), Multiblock.OverrideAction.IGNORE);
        Vector3i[] neighbors = new Vector3i[6];

        neighbors[0] = new Vector3i(pos.getX() + 1, pos.getY(), pos.getZ());
        neighbors[1] = new Vector3i(pos.getX() - 1, pos.getY(), pos.getZ());
        neighbors[2] = new Vector3i(pos.getX(), pos.getY() + 1, pos.getZ());
        neighbors[3] = new Vector3i(pos.getX(), pos.getY() - 1, pos.getZ());
        neighbors[4] = new Vector3i(pos.getX(), pos.getY(), pos.getZ() + 1);
        neighbors[5] = new Vector3i(pos.getX(), pos.getY(), pos.getZ() - 1);

        for (Vector3i neighbor : neighbors) {
            if (world.getBlockState(neighbor.toBlock()).getBlock() instanceof ScreenBlock) {
                for (BlockSide bs : BlockSide.values())
                    destroySide(world, neighbor.clone(), bs, override, (placer instanceof Player p) ? p : null);
            }
        }
    }
}

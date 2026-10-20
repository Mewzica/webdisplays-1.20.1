/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.montoyo.wd.WDContent;
import net.montoyo.wd.block.PeripheralBlock;
import net.montoyo.wd.core.DefaultPeripheral;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.net.C2SPadCtrl;
import net.montoyo.wd.net.S2CScreenUpdate;
import net.montoyo.wd.platform.Platform;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.Multiblock;
import net.montoyo.wd.utilities.Vector2i;
import net.montoyo.wd.utilities.Vector3i;
import org.jetbrains.annotations.NotNull;

/**
 * Block entity shared by every peripheral variant.
 * <p>
 * 1.12.2 had one {@code TileEntity} subclass per variant
 * ({@code TileEntityKeyboard}, {@code TileEntityServer}, {@code TileEntityRCtrl},
 * {@code TileEntityRedCtrl}, plus a shared {@code TileEntityInterfaceBase}). Since 1.20.1 requires a
 * single block entity type per block, the shared state lives here and the variant is read from the
 * block state.
 */
public class PeripheralBlockEntity extends BlockEntity {

    /** Redstone controller channels. */
    private final boolean[] channels = new boolean[4];
    /** Linked screen origin, set by the linker item. */
    public Vector3i link = null;
    /** minePad id currently bound to this keyboard, -1 when unbound. */
    public int padId = -1;

    public PeripheralBlockEntity(BlockPos pos, BlockState state) {
        super(WDContent.PERIPHERAL_BE_TYPE.get(), pos, state);
    }

    public DefaultPeripheral kind() {
        BlockState bs = getBlockState();
        return bs.getBlock() instanceof PeripheralBlock ? bs.getValue(PeripheralBlock.KIND) : DefaultPeripheral.KEYBOARD;
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);

        for (int i = 0; i < channels.length; i++)
            channels[i] = tag.getBoolean("Channel" + i);

        if (tag.contains("LinkX"))
            link = new Vector3i(tag.getInt("LinkX"), tag.getInt("LinkY"), tag.getInt("LinkZ"));
        else
            link = null;

        padId = tag.contains("PadId") ? tag.getInt("PadId") : -1;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);

        for (int i = 0; i < channels.length; i++)
            tag.putBoolean("Channel" + i, channels[i]);

        if (link != null) {
            tag.putInt("LinkX", link.x);
            tag.putInt("LinkY", link.y);
            tag.putInt("LinkZ", link.z);
        }

        tag.putInt("PadId", padId);
    }

    public boolean getChannel(int idx) {
        return idx >= 0 && idx < channels.length && channels[idx];
    }

    public void setRedstoneChannel(Player player, int index, boolean enabled) {
        if (level == null || level.isClientSide)
            return;

        if (index < 0 || index >= channels.length)
            return;

        if (kind() != DefaultPeripheral.REDSTONE_CONTROLLER)
            return;

        channels[index] = enabled;
        setChanged();
        level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    /**
     * Routes minePad input to the linked screen, after checking that the player is allowed to click it.
     */
    public void handlePadInput(ServerPlayer player, C2SPadCtrl msg) {
        if (level == null || level.isClientSide)
            return;

        if (link == null) {
            Log.warning("minePad input received on an unlinked peripheral at %s", worldPosition);
            return;
        }

        if (!(level.getBlockEntity(link.toBlock()) instanceof ScreenBlockEntity tes))
            return;

        BlockSide side = sideTowards(link);
        ScreenBlockEntity.Screen scr = side == null ? null : tes.getScreen(side);

        if (scr == null)
            return;

        if ((scr.rightsFor(player) & ScreenRights.CLICK) == 0) {
            net.montoyo.wd.utilities.Util.toast(player, "restrictions");
            return;
        }

        Vector3i screenPos = new Vector3i(link);

        switch (msg.action) {
            case C2SPadCtrl.CLICK ->
                    Platform.get().net().sendToAllAround(
                            S2CScreenUpdate.click(screenPos, side, S2CScreenUpdate.MOUSE_CLICK, msg.vec),
                            link.toBlock(), 64.0);
            case C2SPadCtrl.TYPE ->
                    Platform.get().net().sendToAllAround(
                            S2CScreenUpdate.type(screenPos, side, msg.str),
                            link.toBlock(), 64.0);
            case C2SPadCtrl.KEY_EVENT ->
                    Platform.get().net().sendToAllAround(
                            S2CScreenUpdate.type(screenPos, side, msg.str),
                            link.toBlock(), 64.0);
            default -> {
            }
        }
    }

    /**
     * Guesses which screen face of the linked multiblock faces this peripheral.
     */
    private BlockSide sideTowards(Vector3i screenOrigin) {
        Vector3i delta = new Vector3i(worldPosition).sub(screenOrigin);
        Vector3i abs = new Vector3i(Math.abs(delta.x), Math.abs(delta.y), Math.abs(delta.z));

        if (abs.y > abs.x && abs.y > abs.z)
            return delta.y > 0 ? BlockSide.TOP : BlockSide.BOTTOM;

        if (abs.x > abs.z)
            return delta.x > 0 ? BlockSide.EAST : BlockSide.WEST;

        return delta.z > 0 ? BlockSide.SOUTH : BlockSide.NORTH;
    }
}

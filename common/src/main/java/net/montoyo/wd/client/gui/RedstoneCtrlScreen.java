/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.montoyo.wd.entity.PeripheralBlockEntity;
import net.montoyo.wd.net.C2SRedstoneCtrl;
import net.montoyo.wd.platform.Platform;
import net.montoyo.wd.utilities.Vector3i;
import org.jetbrains.annotations.NotNull;

/**
 * Toggles the four redstone controller channels.
 */
public class RedstoneCtrlScreen extends WDScreen {

    private final Button[] buttons = new Button[4];

    public RedstoneCtrlScreen(Vector3i pos) {
        super(Component.translatable("gui.webdisplays.redstone.title"), pos);
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = height / 2 - 40;

        for (int i = 0; i < 4; i++) {
            final int index = i;
            buttons[i] = Button.builder(label(index), b -> {
                if (pos == null)
                    return;

                boolean next = !isChannelOn(index);
                Platform.get().net().sendToServer(new C2SRedstoneCtrl(pos, index, next));
                b.setMessage(label(index, next));
            }).bounds(cx - 80, y + i * 22, 160, 20).build();

            addRenderableWidget(buttons[i]);
        }
    }

    private boolean isChannelOn(int idx) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || pos == null)
            return false;

        BlockEntity be = mc.level.getBlockEntity(pos.toBlock());
        return be instanceof PeripheralBlockEntity p && p.getChannel(idx);
    }

    private Component label(int idx) {
        return label(idx, isChannelOn(idx));
    }

    private Component label(int idx, boolean on) {
        return Component.translatable("gui.webdisplays.redstone.channel", idx + 1,
                Component.translatable(on ? "gui.yes" : "gui.no"));
    }

    @Override
    public void render(@NotNull net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, height / 2 - 60, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

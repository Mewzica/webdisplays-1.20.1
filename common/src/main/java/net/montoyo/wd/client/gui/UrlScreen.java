/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.montoyo.wd.net.C2SScreenAction;
import net.montoyo.wd.platform.Platform;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Vector3i;
import org.jetbrains.annotations.NotNull;

/**
 * The "set URL" dialog, opened by sneak-right-clicking a screen.
 */
public class UrlScreen extends WDScreen {

    private final BlockSide side;
    private final String initialUrl;
    private EditBox urlField;

    public UrlScreen(Vector3i pos, BlockSide side, String url) {
        super(Component.translatable("gui.webdisplays.url.title"), pos);
        this.side = side;
        this.initialUrl = url;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;

        urlField = new EditBox(font, cx - 150, cy - 20, 300, 20, Component.translatable("gui.webdisplays.url"));
        urlField.setMaxLength(2048);
        urlField.setValue(initialUrl);
        addRenderableWidget(urlField);
        setInitialFocus(urlField);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> apply())
                .bounds(cx - 150, cy + 10, 145, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(cx + 5, cy + 10, 145, 20)
                .build());
    }

    private void apply() {
        String url = urlField.getValue().trim();

        if (!url.isEmpty() && pos != null) {
            C2SScreenAction p = C2SScreenAction.of(C2SScreenAction.SET_URL, pos, side);
            p.str = url;
            Platform.get().net().sendToServer(p);
        }

        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { // Enter / keypad enter
            apply();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(@NotNull net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, height / 2 - 40, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.montoyo.mcef.api.API;
import net.montoyo.mcef.api.IBrowser;
import net.montoyo.mcef.api.MCEFApi;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.item.MinePadItem;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.Vector3i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The minePad GUI: a browser floating over the world.
 * <p>
 * 1.12.2 rendered it with the same fixed pipeline code as the block screens; here the browser texture is
 * drawn through {@link IBrowser#draw}.
 */
public class MinePadScreen extends WDScreen {

    /** Resolution of the pad, derived from the 59/30 aspect ratio and the configured height. */
    private static final int PAD_HEIGHT = 480;
    private static final int PAD_WIDTH = (int) (PAD_HEIGHT * WebDisplays.PAD_RATIO);

    private final ItemStack pad;
    private final int padId;

    @Nullable
    private IBrowser browser;

    private int screenX;
    private int screenY;
    private int screenW;
    private int screenH;

    public MinePadScreen(ItemStack pad, int padId) {
        super(Component.translatable("gui.webdisplays.minepad.title"), (Vector3i) null);
        this.pad = pad;
        this.padId = padId;
    }

    @Override
    protected void init() {
        // Fit the pad into the window while keeping the 59:30 ratio.
        screenH = Math.min(height - 60, PAD_HEIGHT);
        screenW = (int) (screenH * WebDisplays.PAD_RATIO);

        if (screenW > width - 40) {
            screenW = width - 40;
            screenH = (int) (screenW / WebDisplays.PAD_RATIO);
        }

        screenX = (width - screenW) / 2;
        screenY = (height - screenH) / 2;

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 - 40, screenY + screenH + 6, 80, 18)
                .build());

        if (browser == null)
            browser = createBrowser();
    }

    @Nullable
    private IBrowser createBrowser() {
        if (!MCEFApi.isMCEFLoaded())
            return null;

        API api = MCEFApi.getAPI();
        String url = net.montoyo.wd.WDConfig.get().homepage;

        try {
            IBrowser b = api.createBrowser(WebDisplays.applyBlacklist(url));
            b.resize(PAD_WIDTH, PAD_HEIGHT);
            return b;
        } catch (Throwable t) {
            Log.warningEx("Could not create the minePad browser", t);
            return null;
        }
    }

    @Override
    public void render(@NotNull net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);

        g.fill(screenX - 2, screenY - 2, screenX + screenW + 2, screenY + screenH + 2, 0xFF202020);

        if (browser != null) {
            var pose = g.pose();
            pose.pushPose();
            pose.translate(screenX, screenY, 0);
            browser.draw(pose, 0, 0, screenW, screenH, 0xF000F0);
            pose.popPose();
        } else {
            g.drawCenteredString(font, Component.translatable("gui.webdisplays.mcefMissing"),
                    width / 2, height / 2, 0xFF5555);
        }

        g.drawString(font, Component.literal("minePad #" + padId), screenX, screenY - 12, 0xA0A0A0);
    }

    private int toBrowserX(double mouseX) {
        return (int) ((mouseX - screenX) * PAD_WIDTH / screenW);
    }

    private int toBrowserY(double mouseY) {
        return (int) ((mouseY - screenY) * PAD_HEIGHT / screenH);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (browser != null && mouseX >= screenX && mouseX < screenX + screenW
                && mouseY >= screenY && mouseY < screenY + screenH) {
            int bx = toBrowserX(mouseX);
            int by = toBrowserY(mouseY);
            browser.injectMouseMove(bx, by, 0, false);
            browser.injectMouseButton(bx, by, 0, button + 1, true, 1);
            browser.injectMouseButton(bx, by, 0, button + 1, false, 1);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (browser != null && mouseX >= screenX && mouseX < screenX + screenW
                && mouseY >= screenY && mouseY < screenY + screenH) {
            browser.injectMouseWheel(toBrowserX(mouseX), toBrowserY(mouseY), 0, 1, (int) (delta * 20));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (browser != null) {
            browser.injectKeyTyped(codePoint, 0);
            return true;
        }

        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        if (browser != null) {
            browser.close();
            browser = null;
        }

        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.net.C2SScreenAction;
import net.montoyo.wd.platform.Platform;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.NameUUIDPair;
import net.montoyo.wd.utilities.Rotation;
import net.montoyo.wd.utilities.Vector2i;
import net.montoyo.wd.utilities.Vector3i;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The screen configuration GUI, opened with the screen configurator item.
 * <p>
 * 1.12.2 had a bespoke widget toolkit ({@code client/gui/controls}, ~2500 lines including the JSON
 * driven loader). The port uses vanilla widgets and keeps the same capabilities: URL, resolution,
 * rotation, auto volume, owner and the friend list.
 */
public class ScreenConfigScreen extends WDScreen {

    private final BlockSide side;
    private final JsonObject data;

    private EditBox urlField;
    private EditBox resXField;
    private EditBox resYField;

    private String ownerName = "";
    private int friendRights;
    private int otherRights;
    private boolean autoVolume = true;
    private Rotation rotation = Rotation.ROT_0;
    private final List<NameUUIDPair> friends = new ArrayList<>();

    public ScreenConfigScreen(Vector3i pos, BlockSide side, JsonObject data) {
        super(Component.translatable("gui.webdisplays.screencfg.title"), pos);
        this.side = side;
        this.data = data;
        parse(data);
    }

    private void parse(JsonObject obj) {
        if (obj.has("owner"))
            ownerName = obj.get("owner").getAsString();

        if (obj.has("autoVolume"))
            autoVolume = obj.get("autoVolume").getAsBoolean();

        if (obj.has("rotation"))
            rotation = Rotation.values()[Math.max(0, Math.min(Rotation.values().length - 1, obj.get("rotation").getAsInt()))];

        if (obj.has("friendRights"))
            friendRights = obj.get("friendRights").getAsInt();

        if (obj.has("otherRights"))
            otherRights = obj.get("otherRights").getAsInt();

        friends.clear();

        if (obj.has("friends")) {
            JsonArray arr = obj.getAsJsonArray("friends");

            for (JsonElement el : arr) {
                JsonObject fo = el.getAsJsonObject();
                friends.add(new NameUUIDPair(fo.get("name").getAsString(), UUID.fromString(fo.get("uuid").getAsString())));
            }
        }
    }

    private String str(String key, String def) {
        return data.has(key) ? data.get(key).getAsString() : def;
    }

    private int num(String key, int def) {
        return data.has(key) ? data.get(key).getAsInt() : def;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int top = 40;

        urlField = new EditBox(font, cx - 150, top, 220, 18, Component.translatable("gui.webdisplays.url"));
        urlField.setMaxLength(2048);
        urlField.setValue(str("url", ""));
        addRenderableWidget(urlField);

        addRenderableWidget(Button.builder(Component.translatable("gui.webdisplays.apply"), b -> {
            if (pos == null)
                return;

            C2SScreenAction p = C2SScreenAction.of(C2SScreenAction.SET_URL, pos, side);
            p.str = urlField.getValue().trim();
            Platform.get().net().sendToServer(p);
        }).bounds(cx + 75, top, 75, 18).build());

        resXField = new EditBox(font, cx - 150, top + 26, 60, 18, Component.literal("X"));
        resXField.setMaxLength(5);
        resXField.setValue(Integer.toString(num("resX", 640)));
        addRenderableWidget(resXField);

        resYField = new EditBox(font, cx - 80, top + 26, 60, 18, Component.literal("Y"));
        resYField.setMaxLength(5);
        resYField.setValue(Integer.toString(num("resY", 480)));
        addRenderableWidget(resYField);

        addRenderableWidget(Button.builder(Component.translatable("gui.webdisplays.apply"), b -> {
            if (pos == null)
                return;

            int x = parseInt(resXField.getValue(), 640);
            int y = parseInt(resYField.getValue(), 480);
            C2SScreenAction p = C2SScreenAction.of(C2SScreenAction.SET_RESOLUTION, pos, side);
            p.vec = new Vector2i(x, y);
            Platform.get().net().sendToServer(p);
        }).bounds(cx + 5, top + 26, 70, 18).build());

        addRenderableWidget(Button.builder(rotationLabel(), b -> {
            if (pos == null)
                return;

            rotation = Rotation.values()[(rotation.ordinal() + 1) % Rotation.values().length];
            C2SScreenAction p = C2SScreenAction.of(C2SScreenAction.SET_ROTATION, pos, side);
            p.a = rotation.ordinal();
            Platform.get().net().sendToServer(p);
            b.setMessage(rotationLabel());
        }).bounds(cx - 150, top + 52, 140, 18).build());

        addRenderableWidget(Button.builder(autoVolumeLabel(), b -> {
            if (pos == null)
                return;

            autoVolume = !autoVolume;
            C2SScreenAction p = C2SScreenAction.of(C2SScreenAction.SET_AUTO_VOLUME, pos, side);
            p.a = autoVolume ? 1 : 0;
            Platform.get().net().sendToServer(p);
            b.setMessage(autoVolumeLabel());
        }).bounds(cx + 5, top + 52, 145, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.webdisplays.addMe"), b -> {
            if (pos == null || Minecraft.getInstance().player == null)
                return;

            C2SScreenAction p = C2SScreenAction.of(C2SScreenAction.ADD_FRIEND, pos, side);
            p.friend = new NameUUIDPair(Minecraft.getInstance().player.getGameProfile());
            Platform.get().net().sendToServer(p);
        }).bounds(cx - 150, top + 78, 140, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(cx + 5, top + 78, 145, 18)
                .build());
    }

    private static int parseInt(String str, int def) {
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private Component rotationLabel() {
        return Component.translatable("gui.webdisplays.rotation",
                Component.translatable("gui.webdisplays.rotation." + rotation.name().toLowerCase()));
    }

    private Component autoVolumeLabel() {
        return Component.translatable("gui.webdisplays.autovolume",
                Component.translatable(autoVolume ? "gui.yes" : "gui.no"));
    }

    @Override
    public void onServerUpdate() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || pos == null)
            return;

        BlockEntity be = mc.level.getBlockEntity(pos.toBlock());

        if (be instanceof ScreenBlockEntity tes) {
            ScreenBlockEntity.Screen scr = tes.getScreen(side);

            if (scr != null)
                parse(net.montoyo.wd.WebDisplays.GSON.fromJson(tes.describeScreen(scr), JsonObject.class));
        }
    }

    @Override
    public void render(@NotNull net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);

        int cx = width / 2;
        int y = 40 + 102;

        g.drawCenteredString(font, title, cx, 24, 0xFFFFFF);
        g.drawString(font, Component.translatable("gui.webdisplays.owner", ownerName), cx - 150, y, 0xA0A0A0);

        y += 12;
        g.drawString(font, Component.translatable("gui.webdisplays.friends", friends.size()), cx - 150, y, 0xA0A0A0);

        for (NameUUIDPair f : friends) {
            y += 10;

            if (y > height - 20)
                break;

            g.drawString(font, Component.literal(" - " + f.name), cx - 140, y, 0x8080FF);
        }

        y += 14;

        if (y < height - 20)
            g.drawString(font, Component.translatable("gui.webdisplays.rights",
                    Integer.toHexString(friendRights), Integer.toHexString(otherRights)), cx - 150, y, 0x707070);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

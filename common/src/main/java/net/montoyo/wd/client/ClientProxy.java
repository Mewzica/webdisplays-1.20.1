/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.montoyo.mcef.api.API;
import net.montoyo.mcef.api.IBrowser;
import net.montoyo.mcef.api.MCEFApi;
import net.montoyo.wd.WDConfig;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.Rotation;
import net.montoyo.wd.utilities.Vector2i;
import net.montoyo.wd.utilities.Vector3f;
import net.montoyo.wd.utilities.Vector3i;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Client-side counterpart of the 1.12.2 {@code ClientProxy}.
 * <p>
 * It keeps the list of screens the client knows about, creates their MCEF browsers lazily, and draws
 * them once per frame. The 1.12.2 version was a {@code TileEntitySpecialRenderer}; 1.20.1's block
 * entity renderer cannot extend its culling box without loader-specific code, so the port draws every
 * tracked screen from a world render callback instead.
 */
public final class ClientProxy {

    private static ClientProxy instance;

    private final List<ScreenBlockEntity> tracked = new ArrayList<>();

    private ClientProxy() {
    }

    public static ClientProxy get() {
        if (instance == null)
            instance = new ClientProxy();

        return instance;
    }

    /**
     * Called by the loader entry point once, on the client only.
     */
    public void init() {
        var platform = net.montoyo.wd.platform.Platform.get();
        platform.onClientTick(this::tick);
        platform.onWorldRender(this::render);
    }

    @Nullable
    public API getMcef() {
        return MCEFApi.isMCEFLoaded() ? MCEFApi.getAPI() : null;
    }

    public void trackScreen(ScreenBlockEntity tes, boolean add) {
        if (add) {
            if (!tracked.contains(tes))
                tracked.add(tes);
        } else {
            tracked.remove(tes);

            for (int i = 0; i < tes.screenCount(); i++) {
                ScreenBlockEntity.Screen scr = tes.getScreen(i);

                if (scr.browser != null) {
                    scr.browser.close();
                    scr.browser = null;
                }
            }
        }
    }

    private void tick() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) {
            if (!tracked.isEmpty()) {
                for (ScreenBlockEntity tes : tracked)
                    unloadScreens(tes);

                tracked.clear();
            }

            return;
        }

        WDConfig cfg = WDConfig.get();
        Iterator<ScreenBlockEntity> it = tracked.iterator();

        while (it.hasNext()) {
            ScreenBlockEntity tes = it.next();

            if (tes.isRemoved()) {
                it.remove();
                continue;
            }

            double distSq = tes.getBlockPos().distSqr(mc.player.blockPosition());

            if (distSq > cfg.unloadDistanceSq()) {
                tes.unload();
            } else if (distSq <= cfg.loadDistanceSq()) {
                tes.load();
            }

            if (tes.isLoaded() && cfg.enableAutoVolume)
                tes.updateTrackDistance(distSq, mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MASTER));
        }
    }

    private void unloadScreens(ScreenBlockEntity tes) {
        for (int i = 0; i < tes.screenCount(); i++) {
            ScreenBlockEntity.Screen scr = tes.getScreen(i);

            if (scr.browser != null) {
                scr.browser.close();
                scr.browser = null;
            }
        }
    }

    private void render(PoseStack pose, float partialTick, Vec3 cameraPos) {
        if (tracked.isEmpty())
            return;

        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null)
            return;

        for (ScreenBlockEntity tes : tracked) {
            if (!tes.isLoaded())
                continue;

            for (int i = 0; i < tes.screenCount(); i++) {
                ScreenBlockEntity.Screen scr = tes.getScreen(i);
                ensureBrowser(scr);

                if (scr.browser == null)
                    continue;

                renderScreen(pose, cameraPos, tes, scr);
            }
        }
    }

    private void ensureBrowser(ScreenBlockEntity.Screen scr) {
        if (scr.browser != null)
            return;

        API api = getMcef();

        if (api == null) {
            Log.warning("MCEF is not loaded, screens will stay blank");
            return;
        }

        try {
            scr.browser = api.createBrowser(WebDisplays.applyBlacklist(scr.url == null ? WDConfig.get().homepage : scr.url));

            if (scr.rotation.isVertical)
                scr.browser.resize(scr.resolution.y, scr.resolution.x);
            else
                scr.browser.resize(scr.resolution.x, scr.resolution.y);

            scr.doTurnOnAnim = true;
            scr.turnOnTime = System.currentTimeMillis();
        } catch (Throwable t) {
            Log.warningEx("Could not create a browser for screen %s", t, scr.url);
            scr.browser = null;
        }
    }

    private void renderScreen(PoseStack pose, Vec3 cameraPos, ScreenBlockEntity tes, ScreenBlockEntity.Screen scr) {
        BlockSide side = scr.side;
        Vector3i origin = new Vector3i(tes.getBlockPos());

        // Centre of the whole wall, in world coordinates.
        Vector3f mid = new Vector3f(origin.x + 0.5f, origin.y + 0.5f, origin.z + 0.5f);
        Vector3i span = new Vector3i(side.right).mul(scr.size.x).addMul(side.up, scr.size.y);
        mid.addMul(span.toFloat(), 0.5f);
        mid.addMul(side.left.toFloat(), 0.5f);
        mid.addMul(side.down.toFloat(), 0.5f);

        pose.pushPose();
        pose.translate(mid.x - cameraPos.x, mid.y - cameraPos.y, mid.z - cameraPos.z);

        switch (side) {
            case BOTTOM -> pose.mulPose(Axis.XP.rotationDegrees(90.f));
            case TOP -> pose.mulPose(Axis.XP.rotationDegrees(-90.f));
            case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(180.f));
            case SOUTH -> {
            }
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(-90.f));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(90.f));
        }

        if (scr.doTurnOnAnim) {
            long lt = System.currentTimeMillis() - scr.turnOnTime;
            float ft = ((float) lt) / 100.0f;

            if (ft >= 1.0f) {
                ft = 1.0f;
                scr.doTurnOnAnim = false;
            }

            pose.scale(ft, ft, 1.0f);
        }

        Rotation rot = scr.rotation;

        if (!rot.isNull)
            pose.mulPose(Axis.ZP.rotationDegrees(rot.angle));

        float sw = ((float) scr.size.x) * 0.5f - 2.f / 16.f;
        float sh = ((float) scr.size.y) * 0.5f - 2.f / 16.f;

        if (rot.isVertical) {
            float tmp = sw;
            sw = sh;
            sh = tmp;
        }

        scr.browser.draw(pose, -sw, -sh, sw, sh, 0xF000F0);
        pose.popPose();
    }

    // ---- GUI callbacks ---------------------------------------------------

    public void screenUpdateResolutionInGui(Vector3i pos, BlockSide side, Vector2i res) {
        ClientGuiOpener.updateOpenGui(pos, side);
    }

    public void screenUpdateRotationInGui(Vector3i pos, BlockSide side, Rotation rot) {
        ClientGuiOpener.updateOpenGui(pos, side);
    }

    public void screenUpdateAutoVolumeInGui(Vector3i pos, BlockSide side, boolean av) {
        ClientGuiOpener.updateOpenGui(pos, side);
    }
}

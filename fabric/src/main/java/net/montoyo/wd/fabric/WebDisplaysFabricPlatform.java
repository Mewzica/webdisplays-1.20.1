package net.montoyo.wd.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.montoyo.wd.platform.WDNet;
import net.montoyo.wd.platform.WDPlatform;
import net.montoyo.wd.platform.WDRegistrar;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WebDisplaysFabricPlatform implements WDPlatform {

    private final WebDisplaysFabricRegistrar registrar = new WebDisplaysFabricRegistrar();
    private final WebDisplaysFabricNet net = new WebDisplaysFabricNet();

    private final List<Consumer<ServerLevel>> worldSaves = new ArrayList<>();
    private MinecraftServer server;

    public WebDisplaysFabricPlatform() {
        ServerLifecycleEvents.SERVER_STARTED.register(s -> server = s);
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> {
            for (ServerLevel level : s.getAllLevels()) {
                for (Consumer<ServerLevel> cb : worldSaves)
                    cb.accept(level);
            }

            server = null;
        });
    }

    @Override
    public String id() {
        return "fabric";
    }

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public WDRegistrar registrar() {
        return registrar;
    }

    @Override
    public WDNet net() {
        return net;
    }

    @Override
    public Path gameDirectory() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public MinecraftServer server() {
        return server;
    }

    @Override
    public void onClientTick(Runnable callback) {
        ClientTickEvents.END_CLIENT_TICK.register(client -> callback.run());
    }

    @Override
    public void onWorldRender(WorldRenderCallback callback) {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx ->
                callback.render(ctx.matrixStack(), ctx.tickDelta(), ctx.camera().getPosition()));
    }

    @Override
    public void onClientShutdown(Runnable callback) {
        // Fabric has no shutdown event; MCEF installs its own JVM hook, so nothing to do here.
    }

    @Override
    public void onServerWorldLoad(Consumer<ServerLevel> callback) {
        ServerWorldEvents.LOAD.register((srv, level) -> callback.accept(level));
    }

    @Override
    public void onServerWorldSave(Consumer<ServerLevel> callback) {
        worldSaves.add(callback);
    }

    @Override
    public void onServerStopping(Runnable callback) {
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> callback.run());
    }

    @Override
    public void onPlayerJoin(Consumer<ServerPlayer> callback) {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, srv) -> callback.accept(handler.getPlayer()));
    }

    @Override
    public void onPlayerLeave(Consumer<ServerPlayer> callback) {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, srv) -> callback.accept(handler.getPlayer()));
    }
}

package net.montoyo.wd.fabric;

import net.fabricmc.api.EnvType;
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
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fabric platform. Safe to load on a dedicated server.
 * <p>
 * The three client hooks are forwarded to {@link WebDisplaysFabricClientEvents}, which is only
 * instantiated from the client entrypoint. Keeping the fabric client API out of this class matters:
 * Fabric's loader throws as soon as a {@code net.fabricmc.fabric.api.client} class is resolved in a
 * {@code SERVER} environment, even if the method that uses it never runs.
 */
public class WebDisplaysFabricPlatform implements WDPlatform {

    private final WebDisplaysFabricRegistrar registrar = new WebDisplaysFabricRegistrar();
    private final WebDisplaysFabricNet net = new WebDisplaysFabricNet();

    private final List<Consumer<ServerLevel>> worldSaves = new ArrayList<>();
    private MinecraftServer server;

    @Nullable
    private WebDisplaysFabricClientEvents clientEvents;

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

    /** Called from {@link WebDisplaysFabricClient}; never on a dedicated server. */
    void installClientSide(WebDisplaysFabricClientEvents events) {
        this.clientEvents = events;
    }

    WebDisplaysFabricNet netInternal() {
        return net;
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
        if (clientEvents != null)
            clientEvents.onTick(callback);
    }

    @Override
    public void onWorldRender(WorldRenderCallback callback) {
        if (clientEvents != null)
            clientEvents.onWorldRender(callback);
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

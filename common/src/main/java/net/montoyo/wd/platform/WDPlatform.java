package net.montoyo.wd.platform;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * The loader-specific surface WebDisplays needs. One instance per loader, installed by the loader
 * entry point before {@code WebDisplays.init()} runs.
 */
public interface WDPlatform {

    /**
     * @return {@code "fabric"}, {@code "forge"} or {@code "neoforge"}.
     */
    String id();

    /**
     * @return whether the physical client is running.
     */
    boolean isClient();

    WDRegistrar registrar();

    WDNet net();

    /**
     * @return the directory the game was started in ({@code .minecraft}), used for the config file and
     *         the CEF resource cache.
     */
    Path gameDirectory();

    MinecraftServer server();

    /**
     * Fired once per client tick, on the client thread.
     */
    void onClientTick(Runnable callback);

    /**
     * Fired once per frame while a world is being rendered, on the render thread. The pose stack is
     * already in camera space, so the callback must translate by {@code cameraPos} to reach world
     * coordinates.
     */
    void onWorldRender(WorldRenderCallback callback);

    interface WorldRenderCallback {
        void render(com.mojang.blaze3d.vertex.PoseStack pose, float partialTick,
                    net.minecraft.world.phys.Vec3 cameraPos);
    }

    /**
     * Fired when the client shuts down.
     */
    void onClientShutdown(Runnable callback);

    void onServerWorldLoad(Consumer<ServerLevel> callback);

    void onServerWorldSave(Consumer<ServerLevel> callback);

    void onServerStopping(Runnable callback);

    void onPlayerJoin(Consumer<ServerPlayer> callback);

    void onPlayerLeave(Consumer<ServerPlayer> callback);
}

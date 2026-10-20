package net.montoyo.wd.forge;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.montoyo.wd.platform.WDNet;
import net.montoyo.wd.platform.WDPlatform;
import net.montoyo.wd.platform.WDRegistrar;

import java.nio.file.Path;
import java.util.function.Consumer;

public class WebDisplaysForgePlatform implements WDPlatform {

    private final WebDisplaysForgeRegistrar registrar;
    private final WebDisplaysForgeNet net = new WebDisplaysForgeNet();

    public WebDisplaysForgePlatform(IEventBus modBus) {
        registrar = new WebDisplaysForgeRegistrar(modBus);
    }

    @Override
    public String id() {
        return "forge";
    }

    @Override
    public boolean isClient() {
        return FMLEnvironment.dist.isClient();
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
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public MinecraftServer server() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    @Override
    public void onClientTick(Runnable callback) {
        if (!isClient())
            return;

        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent ev) -> {
            if (ev.phase == TickEvent.Phase.END)
                callback.run();
        });
    }

    @Override
    public void onWorldRender(WorldRenderCallback callback) {
        if (!isClient())
            return;

        MinecraftForge.EVENT_BUS.addListener((RenderLevelStageEvent ev) -> {
            if (ev.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
                callback.render(ev.getPoseStack(), ev.getPartialTick(), ev.getCamera().getPosition());
        });
    }

    @Override
    public void onClientShutdown(Runnable callback) {
        // MCEF installs its own JVM shutdown hook, so nothing extra is needed here.
    }

    @Override
    public void onServerWorldLoad(Consumer<ServerLevel> callback) {
        MinecraftForge.EVENT_BUS.addListener((LevelEvent.Load ev) -> {
            if (ev.getLevel() instanceof ServerLevel level)
                callback.accept(level);
        });
    }

    @Override
    public void onServerWorldSave(Consumer<ServerLevel> callback) {
        MinecraftForge.EVENT_BUS.addListener((LevelEvent.Save ev) -> {
            if (ev.getLevel() instanceof ServerLevel level)
                callback.accept(level);
        });
    }

    @Override
    public void onServerStopping(Runnable callback) {
        MinecraftForge.EVENT_BUS.addListener((ServerStoppingEvent ev) -> callback.run());
    }

    @Override
    public void onPlayerJoin(Consumer<ServerPlayer> callback) {
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent ev) -> {
            if (ev.getEntity() instanceof ServerPlayer player)
                callback.accept(player);
        });
    }

    @Override
    public void onPlayerLeave(Consumer<ServerPlayer> callback) {
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent ev) -> {
            if (ev.getEntity() instanceof ServerPlayer player)
                callback.accept(player);
        });
    }
}

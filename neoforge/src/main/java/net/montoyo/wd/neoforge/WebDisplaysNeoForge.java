package net.montoyo.wd.neoforge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.montoyo.wd.WDContent;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.platform.Platform;

@Mod(WebDisplays.MOD_ID)
public class WebDisplaysNeoForge {

    public WebDisplaysNeoForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        Platform.install(new WebDisplaysNeoForgePlatform(modBus));
        WDContent.bootstrap();
        WebDisplays.init();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> WebDisplaysNeoForgeClient::init);
    }
}

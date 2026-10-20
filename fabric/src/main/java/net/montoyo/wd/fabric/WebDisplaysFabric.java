package net.montoyo.wd.fabric;

import net.fabricmc.api.ModInitializer;
import net.montoyo.wd.WDContent;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.platform.Platform;

public class WebDisplaysFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Platform.install(new WebDisplaysFabricPlatform());
        WDContent.bootstrap();
        WebDisplays.init();
    }
}

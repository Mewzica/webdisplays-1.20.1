package net.montoyo.wd.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.montoyo.wd.client.ClientProxy;
import net.montoyo.wd.platform.Platform;

public class WebDisplaysFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        WebDisplaysFabricPlatform platform = (WebDisplaysFabricPlatform) Platform.get();

        // The common init already ran and registered every packet type, so hand the network layer its
        // client half now and let it replay the receivers it collected.
        platform.installClientSide(new WebDisplaysFabricClientEvents());
        platform.netInternal().installClientSide(new WebDisplaysFabricNetClient());

        ClientProxy.get().init();
    }
}

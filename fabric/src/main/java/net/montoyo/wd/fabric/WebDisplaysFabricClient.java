package net.montoyo.wd.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.montoyo.wd.client.ClientProxy;

public class WebDisplaysFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientProxy.get().init();
    }
}

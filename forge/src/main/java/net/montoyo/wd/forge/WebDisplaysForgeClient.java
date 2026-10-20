package net.montoyo.wd.forge;

import net.montoyo.wd.client.ClientProxy;

/**
 * Client-only bootstrap, isolated in its own class so the dedicated server never loads it.
 */
public final class WebDisplaysForgeClient {

    private WebDisplaysForgeClient() {
    }

    public static void init() {
        ClientProxy.get().init();
    }
}

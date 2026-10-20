package net.montoyo.wd.neoforge;

import net.montoyo.wd.client.ClientProxy;

/**
 * Client-only bootstrap, isolated in its own class so the dedicated server never loads it.
 */
public final class WebDisplaysNeoForgeClient {

    private WebDisplaysNeoForgeClient() {
    }

    public static void init() {
        ClientProxy.get().init();
    }
}

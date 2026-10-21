package net.montoyo.wd.fabric;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.montoyo.wd.platform.WDPlatform;

/**
 * Client-only event wiring for the Fabric platform.
 * <p>
 * Instantiating this class on a dedicated server would trip Fabric's environment check, so it is
 * created exclusively from {@link WebDisplaysFabricClient}.
 */
final class WebDisplaysFabricClientEvents {

    void onTick(Runnable callback) {
        ClientTickEvents.END_CLIENT_TICK.register(client -> callback.run());
    }

    void onWorldRender(WDPlatform.WorldRenderCallback callback) {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx ->
                callback.render(ctx.matrixStack(), ctx.tickDelta(), ctx.camera().getPosition()));
    }
}

package net.montoyo.wd.net;

import net.minecraft.resources.ResourceLocation;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.platform.Platform;
import net.montoyo.wd.platform.WDNet;

/**
 * Packet registry.
 * <p>
 * The 1.12.2 mod had 17 message classes built on {@code SimpleNetworkWrapper}, one class per small
 * operation. That does not translate to 1.20.1 without a lot of per-loader plumbing, so the port
 * keeps two payload-carrying packets ({@link S2COpenGui}, {@link S2CScreenUpdate},
 * {@link C2SScreenAction}) with a discriminator inside, plus a few tiny ones.
 */
public final class WDNetwork {

    private WDNetwork() {
    }

    public static void registerAll() {
        WDNet net = Platform.get().net();

        net.register(id("open_gui"), S2COpenGui.class, S2COpenGui::new);
        net.register(id("close_gui"), S2CCloseGui.class, S2CCloseGui::new);
        net.register(id("screen_update"), S2CScreenUpdate.class, S2CScreenUpdate::new);
        net.register(id("server_info"), S2CServerInfo.class, S2CServerInfo::new);
        net.register(id("js_response"), S2CJSResponse.class, S2CJSResponse::new);

        net.register(id("request_te_data"), C2SRequestTEData.class, C2SRequestTEData::new);
        net.register(id("screen_action"), C2SScreenAction.class, C2SScreenAction::new);
        net.register(id("pad_ctrl"), C2SPadCtrl.class, C2SPadCtrl::new);
        net.register(id("redstone_ctrl"), C2SRedstoneCtrl.class, C2SRedstoneCtrl::new);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(WebDisplays.MOD_ID, path);
    }
}

package net.montoyo.wd;

import com.google.gson.Gson;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.montoyo.wd.miniserv.server.Server;
import net.montoyo.wd.net.WDNetwork;
import net.montoyo.wd.platform.Platform;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.Util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Shared entry point for WebDisplays on 1.20.1.
 * <p>
 * The 1.12.2 mod was one {@code @Mod} class that also owned the config, the proxy, the network
 * channel and every registry event handler. On 1.20.1 those responsibilities are split:
 * <ul>
 *   <li>loader entry points ({@code fabric/}, {@code forge/}, {@code neoforge/}) own registration and
 *       the lifecycle events, through {@link net.montoyo.wd.platform.WDPlatform};</li>
 *   <li>this class owns the config, the blacklist and the miniserv lifecycle;</li>
 *   <li>{@link WDContent} owns the registered entries.</li>
 * </ul>
 */
public final class WebDisplays {

    public static final String MOD_ID = "webdisplays";

    /** Mod version, carried over from upstream (1.12.2-1.1 -> 1.20.1-1.1). */
    public static final String VERSION = "1.1";

    public static final Gson GSON = new Gson();

    public static final String BLACKLIST_URL = "mod://webdisplays/blacklisted.html";

    /** minePad aspect ratio: 59/30. */
    public static final double PAD_RATIO = 59.0 / 30.0;

    private static int lastPadId;

    private WebDisplays() {
    }

    /**
     * Called once by each loader entry point, on both sides, after {@link WDContent#bootstrap()}.
     */
    public static void init() {
        WDConfig.load();
        WDNetwork.registerAll();

        var platform = Platform.get();

        platform.onServerWorldLoad(WebDisplays::onWorldLoad);
        platform.onServerWorldSave(WebDisplays::onWorldSave);
        platform.onServerStopping(() -> Server.getInstance().stopServer());

        Log.info("WebDisplays %s loaded on %s", VERSION, platform.id());
    }

    private static void onWorldLoad(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD)
            return;

        File worldDir = level.getServer().getWorldPath(LevelResource.ROOT).toFile();
        File f = new File(worldDir, "wd_next.txt");

        if (f.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String idx = br.readLine();

                if (idx == null || idx.trim().isEmpty())
                    throw new RuntimeException("Seems like the file is empty");

                lastPadId = Integer.parseInt(idx.trim());
            } catch (Throwable t) {
                Log.warningEx("Could not read last minePad ID from %s. I'm afraid this might break all minePads.",
                        t, f.getAbsolutePath());
            }
        }

        WDConfig cfg = WDConfig.get();

        if (cfg.miniservPort != 0) {
            Server sv = Server.getInstance();
            sv.setPort(cfg.miniservPort);
            sv.setDirectory(new File(worldDir, "wd_filehost"));
            sv.start();
        }
    }

    private static void onWorldSave(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD)
            return;

        File worldDir = level.getServer().getWorldPath(LevelResource.ROOT).toFile();
        File f = new File(worldDir, "wd_next.txt");

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
            bw.write(lastPadId + "\n");
        } catch (Throwable t) {
            Log.warningEx("Could not save last minePad ID (%d) to %s. I'm afraid this might break all minePads.",
                    t, lastPadId, f.getAbsolutePath());
        }
    }

    public static int getNextAvailablePadID() {
        return lastPadId++;
    }

    public static boolean isSiteBlacklisted(String url) {
        try {
            URL parsed = new URL(Util.addProtocol(url));
            String host = parsed.getHost();

            return WDConfig.get().blacklist.stream().anyMatch(host::equalsIgnoreCase);
        } catch (MalformedURLException ex) {
            return false;
        }
    }

    public static String applyBlacklist(String url) {
        return isSiteBlacklisted(url) ? BLACKLIST_URL : url;
    }
}

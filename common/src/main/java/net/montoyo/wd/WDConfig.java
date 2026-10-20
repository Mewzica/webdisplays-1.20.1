package net.montoyo.wd;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.montoyo.wd.platform.Platform;
import net.montoyo.wd.utilities.Log;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * WebDisplays configuration.
 * <p>
 * The 1.12.2 version used Forge's {@code Configuration} (a {@code .cfg} file). Forge Configuration is
 * Forge-only, so the port keeps the same keys in a plain JSON file, which every loader can read
 * without a platform hook.
 */
public final class WDConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static WDConfig instance;

    // [main]
    public List<String> blacklist = new ArrayList<>();
    public int padHeight = 480;
    public boolean hardRecipes = true;
    public String homepage = "mod://webdisplays/main.html";
    public boolean disableOwnershipThief = false;
    public int maxResolutionX = 1920;
    public int maxResolutionY = 1080;
    public int miniservPort = 25566;
    public long miniservQuotaKib = 1024L;
    public int maxScreenSizeX = 16;
    public int maxScreenSizeY = 16;

    // [client]
    public double loadDistance = 30.0;
    public double unloadDistance = 32.0;

    // [clientAutoVolume]
    public boolean enableAutoVolume = true;
    public double ytVolume = 100.0;
    public double dist100 = 10.0;
    public double dist0 = 30.0;

    private WDConfig() {
    }

    public static WDConfig get() {
        if (instance == null)
            throw new IllegalStateException("WDConfig.load() has not run yet");

        return instance;
    }

    public static void load() {
        Path file = Platform.get().gameDirectory().resolve("config").resolve("webdisplays.json");
        WDConfig cfg = new WDConfig();

        if (Files.isRegularFile(file)) {
            try (Reader r = Files.newBufferedReader(file)) {
                JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
                cfg = GSON.fromJson(obj, WDConfig.class);

                if (cfg == null)
                    cfg = new WDConfig();
            } catch (Throwable t) {
                Log.warningEx("Could not read %s, using defaults", t, file);
                cfg = new WDConfig();
            }
        }

        cfg.sanitize();
        instance = cfg;
        cfg.save(file);
    }

    private void sanitize() {
        if (blacklist == null)
            blacklist = new ArrayList<>();

        if (unloadDistance < loadDistance + 2.0)
            unloadDistance = loadDistance + 2.0;

        if (dist0 < dist100 + 0.1)
            dist0 = dist100 + 0.1;

        miniservPort = Math.max(0, Math.min(Short.MAX_VALUE, miniservPort));
        maxResolutionX = Math.max(16, maxResolutionX);
        maxResolutionY = Math.max(16, maxResolutionY);
        maxScreenSizeX = Math.max(1, maxScreenSizeX);
        maxScreenSizeY = Math.max(1, maxScreenSizeY);
        padHeight = Math.max(16, padHeight);
    }

    private void save(Path file) {
        try {
            Files.createDirectories(file.getParent());

            try (Writer w = Files.newBufferedWriter(file)) {
                GSON.toJson(this, w);
            }
        } catch (IOException e) {
            Log.warningEx("Could not write %s", e, file);
        }
    }

    public double loadDistanceSq() {
        return loadDistance * loadDistance;
    }

    public double unloadDistanceSq() {
        return unloadDistance * unloadDistance;
    }

    public long miniservQuota() {
        return miniservQuotaKib * 1024L;
    }
}

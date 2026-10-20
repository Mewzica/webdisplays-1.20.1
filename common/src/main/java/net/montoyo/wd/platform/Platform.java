package net.montoyo.wd.platform;

public final class Platform {

    private static WDPlatform instance;

    private Platform() {
    }

    public static void install(WDPlatform platform) {
        if (instance != null && instance != platform)
            throw new IllegalStateException("WebDisplays platform already installed: " + instance.id());

        instance = platform;
    }

    public static WDPlatform get() {
        if (instance == null)
            throw new IllegalStateException("WebDisplays platform has not been installed yet");

        return instance;
    }
}

package biomewaypoints;

import biomewaypoints.config.BiomeWaypointsConfig;
import biomewaypoints.platform.Services;

/** Loader-independent startup, called from each loader's client entrypoint. */
public final class BiomeWaypointsCommon {
    private BiomeWaypointsCommon() {
    }

    public static void init() {
        Constants.LOG.info("{} loaded on {}", Constants.MOD_NAME, Services.PLATFORM.getPlatformName());
        BiomeWaypointsConfig.HANDLER.load();
    }
}

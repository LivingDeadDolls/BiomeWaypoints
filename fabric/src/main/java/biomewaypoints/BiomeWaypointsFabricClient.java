package biomewaypoints;

import biomewaypoints.border.BorderLines;
import biomewaypoints.search.BiomeFinder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** The client entrypoint: settings, and every tick the search for the biomes and the lines of their borders. */
public final class BiomeWaypointsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BiomeWaypointsCommon.init();
        ClientTickEvents.END_CLIENT_TICK.register(BiomeFinder::tick);
        ClientTickEvents.END_CLIENT_TICK.register(BorderLines::tick);
    }
}

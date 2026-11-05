package biomewaypoints.waypoints;

import biomewaypoints.Constants;
import biomewaypoints.search.BiomeFinder;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import waypointsapi.api.KindDefinition;
import waypointsapi.api.WaypointKind;
import waypointsapi.api.WaypointRegistry;
import waypointsapi.api.WaypointsPlugin;

/**
 * Registers the kind of waypoints of the biomes found (the {@code waypointsapi} entrypoint): one group per biome, with
 * its settings in this mod's config ({@link BiomeKindSettings}).
 */
public final class BiomeWaypointsPlugin implements WaypointsPlugin {
    public static final Identifier KIND_ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "biomes");

    private static WaypointKind kind;

    @Override
    public void register(WaypointRegistry registry) {
        kind = registry.register(KindDefinition.builder(KIND_ID, Component.translatable("biomewaypoints.kind.biomes"))
                .icon(Items.OAK_SAPLING)
                .groupName(BiomeWaypointsPlugin::biomeName)
                .groupIcon(BiomeIcons::of)
                // A biome spans heights: the height a place was found at says nothing.
                .heightless()
                .settings(new BiomeKindSettings())
                .build());
        BiomeFinder.setKind(kind);
    }

    /** The kind, registered while the client starts. */
    public static WaypointKind kind() {
        if (kind == null) throw new IllegalStateException("The kind of " + Constants.MOD_NAME + " is not registered yet");
        return kind;
    }

    /** The biome's name as the game shows it, or its id for a biome without one. */
    static Component biomeName(Identifier biome) {
        return Component.translatableWithFallback("biome." + biome.getNamespace() + "." + biome.getPath(), biome.toString());
    }
}

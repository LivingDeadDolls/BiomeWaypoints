package biomewaypoints.search;

import java.util.Optional;
import net.minecraft.resources.Identifier;
import seedengine.api.SeedEngineApi;
import seedengine.api.WorldCheck;

/** The world type the biomes of the world the client is in are worked out with: the one place that decides it. */
final class WorldTypes {
    private WorldTypes() {
    }

    /**
     * Seed Engine API's world type of the world the client is in. Empty while it is not known (on a server, until
     * enough terrain has arrived), and when the seed and the world type do not fit the terrain: the places would not
     * be where the world has them.
     */
    static Optional<Identifier> current() {
        if (SeedEngineApi.currentWorldCheck() == WorldCheck.MISMATCH) return Optional.empty();
        return SeedEngineApi.currentWorldType();
    }
}

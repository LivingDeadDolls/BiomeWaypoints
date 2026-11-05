package biomewaypoints.search;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * What a search for the biomes is asked with: the world (server, seed and world type), the dimension, where the
 * player is and which biomes are skipped.
 */
public record SearchRequest(String server, long seed, Identifier worldType, ResourceKey<Level> dimension, BlockPos origin, Set<Identifier> skipped) {
    public SearchRequest {
        origin = origin.immutable();
        skipped = Set.copyOf(skipped);
    }

    // Level.OVERWORLD and Level.NETHER (the same keys), without loading Level, which needs the game set up.
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace("overworld"));
    private static final ResourceKey<Level> NETHER = ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace("the_nether"));

    /** A dimension to search and where from. */
    public record Target(ResourceKey<Level> dimension, BlockPos origin) {
    }

    /**
     * Where to search: the player's dimension from where they are, and with it the other of the Overworld and the
     * Nether, from the place there that goes with it (x and z divided by 8 into the Nether, times 8 out of it; the same
     * height). The End goes with no other.
     */
    public List<Target> targets() {
        List<Target> targets = new ArrayList<>();
        targets.add(new Target(dimension, origin));
        if (dimension.equals(OVERWORLD)) {
            targets.add(new Target(NETHER, new BlockPos(Math.floorDiv(origin.getX(), 8), origin.getY(), Math.floorDiv(origin.getZ(), 8))));
        } else if (dimension.equals(NETHER)) {
            targets.add(new Target(OVERWORLD, new BlockPos(origin.getX() * 8, origin.getY(), origin.getZ() * 8)));
        }
        return targets;
    }

    /** The biomes to search for: those of the dimension, but the skipped ones. */
    public Set<Identifier> biomesToSearch(Set<Identifier> dimensionBiomes) {
        Set<Identifier> biomes = new LinkedHashSet<>(dimensionBiomes);
        biomes.removeAll(skipped);
        return biomes;
    }

    /**
     * Whether this needs a search again after {@code last} (null for none yet): the world, the dimension or the
     * skipped biomes are not the same, or the player has gone {@code distance} blocks or more from where {@code last} was
     * asked (along x and z: going up and down does not count).
     */
    public boolean needsSearchAfter(@Nullable SearchRequest last, int distance) {
        if (last == null || !server.equals(last.server) || seed != last.seed || !worldType.equals(last.worldType) || !dimension.equals(last.dimension) || !skipped.equals(last.skipped)) {
            return true;
        }
        long dx = origin.getX() - last.origin.getX();
        long dz = origin.getZ() - last.origin.getZ();
        return dx * dx + dz * dz >= (long) distance * distance;
    }
}

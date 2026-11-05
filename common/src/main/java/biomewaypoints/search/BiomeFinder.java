package biomewaypoints.search;

import biomewaypoints.Constants;
import biomewaypoints.config.BiomeWaypointsConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import seedengine.api.BiomeArea;
import seedengine.api.FoundBiome;
import seedengine.api.SeedEngineApi;
import waypointsapi.api.Place;
import waypointsapi.api.WaypointKind;
import waypointsapi.api.WaypointScope;

/**
 * Finds the nearest place of each biome of the dimension (but the skipped ones and those whose waypoints are not
 * shown) from where the player is, the way {@code /locate biome} does, and
 * gives it to the kind's scope with the patch around it. Searches on a thread of its own, one search at a time, again
 * once the player has gone {@link BiomeWaypointsConfig#searchAgainDistance} blocks (see {@link SearchRequest}). Not
 * while the world type is not known or the seed does not fit the terrain ({@link WorldTypes}).
 */
public final class BiomeFinder {
    /** {@code /locate biome}'s radius and steps, so that a place found is the one the command tells. */
    public static final int RADIUS = 6400;
    public static final int HORIZONTAL_STEP = 32;
    public static final int VERTICAL_STEP = 64;
    /** The patch around a place found is measured this far from it along x and z (257x257 samples). */
    static final int PATCH_RADIUS = 2048;
    static final int PATCH_STEP = 16;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, Constants.MOD_NAME + " search");
        thread.setDaemon(true);
        return thread;
    });

    private static WaypointKind kind;
    private static SearchRequest last;
    private static Future<?> running;
    /** The patches measured in this world, so that another place found in one of them keeps the patch's id. */
    private static final List<Known> KNOWN = new ArrayList<>();

    private record Known(String server, long seed, ResourceKey<Level> dimension, Identifier biome, BiomePatch patch) {
    }

    private BiomeFinder() {
    }

    /** The kind the places go to, from the plugin. */
    public static void setKind(WaypointKind registered) {
        kind = registered;
    }

    /** Every client tick: starts a search when one is needed and none is running. */
    public static void tick(Minecraft minecraft) {
        Optional<WaypointScope> scope = kind == null ? Optional.empty() : kind.scope();
        if (minecraft.player == null || minecraft.level == null || scope.isEmpty()) {
            last = null;
            return;
        }
        if (running != null && !running.isDone()) return;
        Optional<Identifier> worldType = WorldTypes.current();
        if (worldType.isEmpty()) return;
        WaypointScope current = scope.get();
        SearchRequest request = new SearchRequest(current.server(), current.seed(), worldType.get(), minecraft.level.dimension(),
                minecraft.player.blockPosition(), BiomeWaypointsConfig.get().notSearchedBiomeIds());
        if (!request.needsSearchAfter(last, BiomeWaypointsConfig.get().searchAgainDistance)) return;
        last = request;
        running = EXECUTOR.submit(() -> search(current, request));
    }

    private static void search(WaypointScope scope, SearchRequest request) {
        for (SearchRequest.Target target : request.targets()) {
            Set<Identifier> biomes;
            try {
                biomes = request.biomesToSearch(SeedEngineApi.biomes(request.seed(), request.worldType(), target.dimension()));
            } catch (RuntimeException e) {
                Constants.LOG.error("Could not tell the biomes of {}", target.dimension().identifier(), e);
                continue;
            }
            for (Identifier biome : biomes) {
                if (!scope.isCurrent()) return;
                try {
                    Optional<FoundBiome> found = SeedEngineApi.nearestBiome(request.seed(), request.worldType(), target.dimension(), Set.of(biome), target.origin(),
                            RADIUS, HORIZONTAL_STEP, VERTICAL_STEP);
                    if (found.isEmpty()) continue;
                    BlockPos pos = found.get().pos();
                    BiomePatch patch = known(request, target.dimension(), biome, pos).orElseGet(() -> measure(request, target.dimension(), biome, pos));
                    scope.put(Place.builder(patch.id(), target.dimension(), pos).group(biome).areas(patch.areas()).build());
                } catch (RuntimeException e) {
                    Constants.LOG.error("Could not search for {} in {}", biome, target.dimension().identifier(), e);
                }
            }
        }
    }

    private static Optional<BiomePatch> known(SearchRequest request, ResourceKey<Level> dimension, Identifier biome, BlockPos pos) {
        synchronized (KNOWN) {
            KNOWN.removeIf(known -> !known.server().equals(request.server()) || known.seed() != request.seed());
            return KNOWN.stream()
                    .filter(known -> known.dimension().equals(dimension) && known.biome().equals(biome) && known.patch().covers(pos))
                    .map(Known::patch)
                    .findFirst();
        }
    }

    private static BiomePatch measure(SearchRequest request, ResourceKey<Level> dimension, Identifier biome, BlockPos pos) {
        // On a grid of the world's own (multiples of the step), so that the same patch gets the same id from anywhere.
        int x = Math.floorDiv(pos.getX(), PATCH_STEP) * PATCH_STEP;
        int z = Math.floorDiv(pos.getZ(), PATCH_STEP) * PATCH_STEP;
        BiomeArea area = SeedEngineApi.biomeArea(request.seed(), request.worldType(), dimension, x - PATCH_RADIUS, z - PATCH_RADIUS,
                x + PATCH_RADIUS, z + PATCH_RADIUS, patchHeight(request, dimension, biome, pos), PATCH_STEP);
        BiomePatch patch = BiomePatch.around(area, biome, pos);
        synchronized (KNOWN) {
            KNOWN.add(new Known(request.server(), request.seed(), dimension, biome, patch));
        }
        return patch;
    }

    /**
     * The height the patch is measured at: the surface where the place's column is of the biome there (whatever height
     * the search came upon it at, so the patch and its id are the same from anywhere), else the place's own (a cave
     * biome, under the surface).
     */
    private static int patchHeight(SearchRequest request, ResourceKey<Level> dimension, Identifier biome, BlockPos pos) {
        int surface = SeedEngineApi.height(request.seed(), request.worldType(), dimension, Heightmap.Types.WORLD_SURFACE_WG, pos.getX(), pos.getZ()) - 1;
        boolean atSurface = SeedEngineApi.biome(request.seed(), request.worldType(), dimension, pos.getX(), surface, pos.getZ())
                .map(biome::equals).orElse(false);
        return atSurface ? surface : pos.getY();
    }
}

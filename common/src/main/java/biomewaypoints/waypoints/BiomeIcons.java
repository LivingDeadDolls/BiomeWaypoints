package biomewaypoints.waypoints;

import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * The item of each vanilla biome's waypoints, drawn in Waypoints API's list and on JourneyMap's marks: a block, plant
 * or creature the biome is known for. Other biomes have none.
 */
final class BiomeIcons {
    private static final Map<String, Item> VANILLA = Map.ofEntries(
            Map.entry("ocean", Items.KELP),
            Map.entry("deep_ocean", Items.NAUTILUS_SHELL),
            Map.entry("warm_ocean", Items.BRAIN_CORAL),
            Map.entry("lukewarm_ocean", Items.TROPICAL_FISH),
            Map.entry("deep_lukewarm_ocean", Items.PUFFERFISH),
            Map.entry("cold_ocean", Items.SALMON),
            Map.entry("deep_cold_ocean", Items.COD),
            Map.entry("frozen_ocean", Items.BLUE_ICE),
            Map.entry("deep_frozen_ocean", Items.BLUE_ICE),
            Map.entry("river", Items.WATER_BUCKET),
            Map.entry("frozen_river", Items.ICE),
            Map.entry("beach", Items.SAND),
            Map.entry("snowy_beach", Items.SNOW),
            Map.entry("stony_shore", Items.STONE),
            Map.entry("plains", Items.SHORT_GRASS),
            Map.entry("sunflower_plains", Items.SUNFLOWER),
            Map.entry("snowy_plains", Items.SNOW_BLOCK),
            Map.entry("ice_spikes", Items.PACKED_ICE),
            Map.entry("desert", Items.CACTUS),
            Map.entry("swamp", Items.LILY_PAD),
            Map.entry("mangrove_swamp", Items.MANGROVE_PROPAGULE),
            Map.entry("forest", Items.OAK_SAPLING),
            Map.entry("flower_forest", Items.ALLIUM),
            Map.entry("birch_forest", Items.BIRCH_SAPLING),
            Map.entry("old_growth_birch_forest", Items.BIRCH_LOG),
            Map.entry("dark_forest", Items.DARK_OAK_SAPLING),
            Map.entry("dappled_forest", Items.BROWN_MUSHROOM),
            Map.entry("pale_garden", Items.PALE_OAK_SAPLING),
            Map.entry("taiga", Items.SPRUCE_SAPLING),
            Map.entry("snowy_taiga", Items.SWEET_BERRIES),
            Map.entry("old_growth_pine_taiga", Items.PODZOL),
            Map.entry("old_growth_spruce_taiga", Items.SPRUCE_LOG),
            Map.entry("jungle", Items.JUNGLE_SAPLING),
            Map.entry("sparse_jungle", Items.MELON_SLICE),
            Map.entry("bamboo_jungle", Items.BAMBOO),
            Map.entry("savanna", Items.ACACIA_SAPLING),
            Map.entry("savanna_plateau", Items.ACACIA_LOG),
            Map.entry("windswept_savanna", Items.ACACIA_LEAVES),
            Map.entry("badlands", Items.RED_SAND),
            Map.entry("wooded_badlands", Items.COARSE_DIRT),
            Map.entry("eroded_badlands", Items.TERRACOTTA),
            Map.entry("windswept_hills", Items.EMERALD),
            Map.entry("windswept_gravelly_hills", Items.GRAVEL),
            Map.entry("windswept_forest", Items.SPRUCE_LEAVES),
            Map.entry("meadow", Items.CORNFLOWER),
            Map.entry("cherry_grove", Items.CHERRY_SAPLING),
            Map.entry("grove", Items.POWDER_SNOW_BUCKET),
            Map.entry("snowy_slopes", Items.SNOWBALL),
            Map.entry("frozen_peaks", Items.PACKED_ICE),
            Map.entry("jagged_peaks", Items.GOAT_HORN),
            Map.entry("stony_peaks", Items.CALCITE),
            Map.entry("mushroom_fields", Items.RED_MUSHROOM),
            Map.entry("dripstone_caves", Items.POINTED_DRIPSTONE),
            Map.entry("lush_caves", Items.GLOW_BERRIES),
            Map.entry("deep_dark", Items.SCULK),
            Map.entry("nether_wastes", Items.NETHERRACK),
            Map.entry("soul_sand_valley", Items.SOUL_SAND),
            Map.entry("crimson_forest", Items.CRIMSON_FUNGUS),
            Map.entry("warped_forest", Items.WARPED_FUNGUS),
            Map.entry("basalt_deltas", Items.BASALT),
            Map.entry("the_end", Items.END_STONE),
            Map.entry("end_highlands", Items.CHORUS_FLOWER),
            Map.entry("end_midlands", Items.CHORUS_FRUIT),
            Map.entry("small_end_islands", Items.END_STONE_BRICKS),
            Map.entry("end_barrens", Items.ENDER_PEARL));

    private BiomeIcons() {
    }

    /** The biome's item, or null for a biome that is not vanilla (or the void, never searched). */
    static Item of(Identifier biome) {
        return biome.getNamespace().equals(Identifier.DEFAULT_NAMESPACE) ? VANILLA.get(biome.getPath()) : null;
    }
}

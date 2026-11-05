package biomewaypoints.search;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import seedengine.api.BiomeArea;

/**
 * The patch of a biome around a place found in it: the samples of a {@link BiomeArea} of that biome that connect to
 * the found place (side by side), as boxes. Only the area sampled is measured: a patch reaching its edge goes on past
 * the boxes.
 *
 * @param id    stable for the patch, whichever of its places was found: the biome and the patch's first sample
 *              (smallest z, then smallest x), e.g. {@code minecraft:cherry_grove@-320,96}
 * @param areas the blocks of the patch, a box for each run of samples along x (runs that repeat in the next rows
 *              merged into one box); each sample stands for the {@code step x step} square around it, and the boxes
 *              reach {@link #HALF_HEIGHT} above and below the found place. When the found place's sample is not of
 *              the biome, the patch of the nearest neighbouring sample that is; with none, the square of the place's
 *              own sample (a patch smaller than the samples)
 */
public record BiomePatch(String id, List<BoundingBox> areas) {
    /** How far the boxes reach above and below the found place: half of {@code /locate biome}'s vertical step. */
    public static final int HALF_HEIGHT = 32;

    public BiomePatch {
        areas = List.copyOf(areas);
    }

    /** Measures the patch of {@code biome} at {@code found} in {@code area}, which should be sampled around it. */
    public static BiomePatch around(BiomeArea area, Identifier biome, BlockPos found) {
        int step = area.step();
        int column = Math.clamp(Math.round((found.getX() - area.minX()) / (float) step), 0, area.width() - 1);
        int row = Math.clamp(Math.round((found.getZ() - area.minZ()) / (float) step), 0, area.depth() - 1);
        if (!area.biome(column, row).equals(biome)) {
            // The place falls between samples of the biome: the patch of the nearest of its neighbouring samples that is.
            int[] nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            for (int[] next : new int[][]{{column + 1, row}, {column - 1, row}, {column, row + 1}, {column, row - 1}}) {
                if (next[0] < 0 || next[1] < 0 || next[0] >= area.width() || next[1] >= area.depth() || !area.biome(next[0], next[1]).equals(biome)) continue;
                double dx = area.x(next[0]) - found.getX();
                double dz = area.z(next[1]) - found.getZ();
                if (dx * dx + dz * dz < nearestDistance) {
                    nearestDistance = dx * dx + dz * dz;
                    nearest = next;
                }
            }
            if (nearest == null) {
                // Smaller than the samples: the square of the place's own sample, so that it is still remembered.
                int low = step / 2;
                int high = step - low - 1;
                return new BiomePatch(id(biome, area.x(column), area.z(row)), List.of(new BoundingBox(area.x(column) - low, found.getY() - HALF_HEIGHT,
                        area.z(row) - low, area.x(column) + high, found.getY() + HALF_HEIGHT, area.z(row) + high)));
            }
            column = nearest[0];
            row = nearest[1];
        }

        boolean[] inPatch = new boolean[area.width() * area.depth()];
        ArrayDeque<int[]> open = new ArrayDeque<>();
        inPatch[row * area.width() + column] = true;
        open.add(new int[]{column, row});
        while (!open.isEmpty()) {
            int[] sample = open.poll();
            for (int[] next : new int[][]{{sample[0] + 1, sample[1]}, {sample[0] - 1, sample[1]}, {sample[0], sample[1] + 1}, {sample[0], sample[1] - 1}}) {
                if (next[0] < 0 || next[1] < 0 || next[0] >= area.width() || next[1] >= area.depth()) continue;
                int index = next[1] * area.width() + next[0];
                if (inPatch[index] || !area.biome(next[0], next[1]).equals(biome)) continue;
                inPatch[index] = true;
                open.add(next);
            }
        }

        int low = step / 2;
        int high = step - low - 1;
        int minY = found.getY() - HALF_HEIGHT;
        int maxY = found.getY() + HALF_HEIGHT;
        String id = null;
        List<BoundingBox> areas = new ArrayList<>();
        // The runs of the row before, by their first and last column, with the row their box started at.
        Map<Long, Integer> runs = new HashMap<>();
        for (int z = 0; z <= area.depth(); z++) {
            Map<Long, Integer> next = new HashMap<>();
            for (int x = 0; z < area.depth() && x < area.width(); x++) {
                if (!inPatch[z * area.width() + x]) continue;
                if (id == null) id = id(biome, area.x(x), area.z(z));
                int first = x;
                while (x + 1 < area.width() && inPatch[z * area.width() + x + 1]) x++;
                long run = ((long) first << 32) | x;
                next.put(run, runs.getOrDefault(run, z));
            }
            for (Map.Entry<Long, Integer> run : runs.entrySet()) {
                if (next.containsKey(run.getKey())) continue;
                int first = (int) (run.getKey() >> 32);
                int last = (int) (long) run.getKey();
                areas.add(new BoundingBox(area.x(first) - low, minY, area.z(run.getValue()) - low, area.x(last) + high, maxY, area.z(z - 1) + high));
            }
            runs = next;
        }
        return new BiomePatch(id, areas);
    }

    /** Whether one of the boxes holds the column of {@code pos} (its height is not looked at). */
    public boolean covers(BlockPos pos) {
        for (BoundingBox box : areas) {
            if (pos.getX() >= box.minX() && pos.getX() <= box.maxX() && pos.getZ() >= box.minZ() && pos.getZ() <= box.maxZ()) return true;
        }
        return false;
    }

    private static String id(Identifier biome, int x, int z) {
        return biome + "@" + x + "," + z;
    }
}

package biomewaypoints.border;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Where neighbouring cells of a grid of biomes differ: the edges between them, to draw as the borders of the biomes. */
public final class BiomeBorders {
    /**
     * An edge between two cells, from corner to corner, in cells counted from the grid's first corner: {@code x1 == x2}
     * for one between cells side by side along x, {@code z1 == z2} for one between cells side by side along z.
     */
    public record Edge(int x1, int z1, int x2, int z2) {
    }

    private BiomeBorders() {
    }

    /**
     * The edges between neighbouring cells whose biomes differ. {@code cells[x][z]} is the biome of each cell (any
     * object compared with {@code equals}), or null where it is not known (not loaded): no edge is drawn next to it.
     */
    public static List<Edge> edges(Object[][] cells) {
        List<Edge> edges = new ArrayList<>();
        for (int x = 0; x < cells.length; x++) {
            for (int z = 0; z < cells[x].length; z++) {
                Object biome = cells[x][z];
                if (biome == null) continue;
                if (x + 1 < cells.length && z < cells[x + 1].length && differs(biome, cells[x + 1][z])) edges.add(new Edge(x + 1, z, x + 1, z + 1));
                if (z + 1 < cells[x].length && differs(biome, cells[x][z + 1])) edges.add(new Edge(x, z + 1, x + 1, z + 1));
            }
        }
        return edges;
    }

    private static boolean differs(Object biome, Object other) {
        return other != null && !Objects.equals(biome, other);
    }
}

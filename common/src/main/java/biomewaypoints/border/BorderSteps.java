package biomewaypoints.border;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntBinaryOperator;

/**
 * The lines of borders that follow the ground block by block: along each block of an edge, a level line on top of the
 * higher of the two blocks beside it, and where the height changes from one block to the next (within an edge or where
 * edges meet), an upright line between the two heights. Nothing slants.
 */
public final class BorderSteps {
    /** A line from one point to another, in block coordinates. */
    public record Line(double x1, double y1, double z1, double x2, double y2, double z2) {
    }

    private BorderSteps() {
    }

    /**
     * @param edges   the edges between cells ({@link BiomeBorders#edges}), counted in cells from {@code minX, minZ}
     * @param cell    how many blocks a cell is across
     * @param surface the height of the top of a column of blocks (the y the ground ends at), by x and z
     */
    public static List<Line> lines(List<BiomeBorders.Edge> edges, int minX, int minZ, int cell, IntBinaryOperator surface) {
        List<Line> lines = new ArrayList<>();
        // The heights of the level lines ending at each point, for the upright lines between them.
        Map<Long, double[]> ends = new HashMap<>();
        for (BiomeBorders.Edge edge : edges) {
            boolean alongZ = edge.x1() == edge.x2();
            int x = minX + edge.x1() * cell;
            int z = minZ + edge.z1() * cell;
            for (int i = 0; i < cell; i++) {
                // The block on either side of the edge at this step, and the higher of the two.
                int y = alongZ
                        ? Math.max(surface.applyAsInt(x - 1, z + i), surface.applyAsInt(x, z + i))
                        : Math.max(surface.applyAsInt(x + i, z - 1), surface.applyAsInt(x + i, z));
                int x1 = alongZ ? x : x + i;
                int z1 = alongZ ? z + i : z;
                int x2 = alongZ ? x : x + i + 1;
                int z2 = alongZ ? z + i + 1 : z;
                lines.add(new Line(x1, y, z1, x2, y, z2));
                reach(ends, x1, z1, y);
                reach(ends, x2, z2, y);
            }
        }
        for (Map.Entry<Long, double[]> end : ends.entrySet()) {
            double[] heights = end.getValue();
            if (heights[0] == heights[1]) continue;
            int x = (int) (end.getKey() >> 32);
            int z = (int) (long) end.getKey();
            lines.add(new Line(x, heights[0], z, x, heights[1], z));
        }
        return lines;
    }

    /** Widens the lowest and highest heights of the lines ending at a point. */
    private static void reach(Map<Long, double[]> ends, int x, int z, double y) {
        double[] heights = ends.computeIfAbsent(((long) x << 32) | (z & 0xFFFFFFFFL), key -> new double[]{y, y});
        heights[0] = Math.min(heights[0], y);
        heights[1] = Math.max(heights[1], y);
    }
}

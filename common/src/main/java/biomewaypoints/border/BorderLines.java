package biomewaypoints.border;

import biomewaypoints.config.BiomeWaypointsConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Thin lines on the ground where the biomes around the player meet: the biomes of the chunks the client has, at the
 * surface, in cells of 4 blocks (the size biomes come in), within {@link #RADIUS} blocks, fainter farther away. Lines
 * are hidden behind blocks (a hill, a tree) like anything else in the world, and none are drawn in a dimension with a
 * ceiling (the Nether), whose surface is its roof.
 */
public final class BorderLines {
    /** Lines are drawn this far from the player (blocks, along x and z). */
    public static final int RADIUS = 64;
    private static final int CELL = 4;
    private static final int CELLS = 2 * RADIUS / CELL + 1;
    /** Worked out again at least this often (ticks), for the chunks that came in since. */
    private static final int REFRESH_TICKS = 20;
    private static final float WIDTH = 2.0f;
    /** The color of the lines, without alpha: a soft white. */
    private static final int RGB = 0xF0F0F0;
    /** The alpha of a line next to the player; it fades to 0 at the radius. */
    private static final int NEAR_ALPHA = 0xC0;
    /** Lines are this far above the surface, so they are not inside it. */
    private static final double LIFT = 0.03;

    /** A line's ends. */
    private record Segment(Vec3 from, Vec3 to) {
    }

    private static List<Segment> segments = List.of();
    private static int lastCellX = Integer.MIN_VALUE;
    private static int lastCellZ = Integer.MIN_VALUE;
    private static ClientLevel lastLevel;
    private static int ticks;

    private BorderLines() {
    }

    /** Every client tick: works the lines out again when the player has gone into another cell, or now and then. */
    public static void tick(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || !BiomeWaypointsConfig.get().borderLines || level.dimensionType().hasCeiling()) {
            segments = List.of();
            lastLevel = null;
            return;
        }
        int cellX = Math.floorDiv(minecraft.player.getBlockX(), CELL);
        int cellZ = Math.floorDiv(minecraft.player.getBlockZ(), CELL);
        if (level == lastLevel && cellX == lastCellX && cellZ == lastCellZ && ++ticks < REFRESH_TICKS) return;
        ticks = 0;
        lastLevel = level;
        lastCellX = cellX;
        lastCellZ = cellZ;
        segments = segments(level, (cellX - CELLS / 2) * CELL, (cellZ - CELLS / 2) * CELL);
    }

    private static List<Segment> segments(ClientLevel level, int minX, int minZ) {
        Object[][] cells = new Object[CELLS][CELLS];
        for (int x = 0; x < CELLS; x++) {
            for (int z = 0; z < CELLS; z++) {
                int blockX = minX + x * CELL + CELL / 2;
                int blockZ = minZ + z * CELL + CELL / 2;
                if (!level.hasChunk(SectionPos.blockToSectionCoord(blockX), SectionPos.blockToSectionCoord(blockZ))) continue;
                int y = surface(level, blockX, blockZ);
                cells[x][z] = level.getNoiseBiome(QuartPos.fromBlock(blockX), QuartPos.fromBlock(y), QuartPos.fromBlock(blockZ)).unwrapKey().orElse(null);
            }
        }
        List<Segment> lines = new ArrayList<>();
        // Following the ground block by block, on the higher side of the border, so the lines lie on it rather than in it.
        for (BorderSteps.Line line : BorderSteps.lines(BiomeBorders.edges(cells), minX, minZ, CELL, (x, z) -> surface(level, x, z))) {
            lines.add(new Segment(new Vec3(line.x1(), line.y1() + LIFT, line.z1()), new Vec3(line.x2(), line.y2() + LIFT, line.z2())));
        }
        return lines;
    }

    /**
     * The height above the top block that stops movement (water too) of a column, under any leaves and logs: the ground
     * under a tree rather than its top. The client has no heightmap without them, so they are gone down through here.
     */
    private static int surface(ClientLevel level, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
        while (pos.getY() > level.getMinY() && level.getBlockState(pos.move(Direction.DOWN)).is(BlockTags.LEAVES) || level.getBlockState(pos).is(BlockTags.LOGS)) {
            // Keep going down through the tree.
        }
        return pos.getY() + 1;
    }

    /** Every frame, while the level renderer collects the frame's gizmos: the lines, fading with their distance from the camera. */
    public static void frame(Vec3 camera) {
        for (Segment segment : segments) {
            double dx = (segment.from().x + segment.to().x) / 2 - camera.x;
            double dz = (segment.from().z + segment.to().z) / 2 - camera.z;
            double fade = 1.0 - Math.sqrt(dx * dx + dz * dz) / RADIUS;
            if (fade <= 0) continue;
            int alpha = (int) Math.round(NEAR_ALPHA * fade);
            Gizmos.line(segment.from(), segment.to(), alpha << 24 | RGB, WIDTH);
        }
    }
}

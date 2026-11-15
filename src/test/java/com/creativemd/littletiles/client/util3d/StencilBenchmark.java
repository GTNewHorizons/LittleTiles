package com.creativemd.littletiles.client.util3d;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Vector3i;
import org.junit.Assume;
import org.junit.Test;

import com.creativemd.littletiles.client.util3d.Mesh3dUtil.TileCoverage;
import com.creativemd.littletiles.common.utils.LittleTile;
import com.creativemd.littletiles.common.utils.LittleTileBlock;
import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.LittleTileShapeMode;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;

/**
 * Places a structure dumped by {@code /ltdumpmesh}, then stencils it into itself at random offsets, the way placement
 * does: split into blocks, fit each fragment, check for space, then cut what it overlaps. Prints the time of each step
 * and a checksum of what is left, which a change that keeps the results must not change.
 * <p>
 * Opt-in like the sweeps: set {@value #OPTION}=1 to run it as a test. Run as a program, it takes the stencils per run,
 * the measured runs and the structure resource as arguments.
 */
public class StencilBenchmark {

    private static final String OPTION = "TEST_BENCHMARK_STENCIL";

    private static final int PIXELS = 16;
    /** How far a stencil is moved from where the structure was placed, at most, in pixels along each axis. */
    private static final Vector3i MAX_OFFSET = new Vector3i(24, 6, 16);

    private long fitNanos, spaceNanos, cutNanos;

    /** A tile of the structure: its box in world pixels, and its cutout relative to the box. */
    private static final class StructureTile {

        final LittleTileBox box;
        final LittleTileCutoutInfo cutout;

        StructureTile(LittleTileBox box, LittleTileCutoutInfo cutout) {
            this.box = box;
            this.cutout = cutout;
        }
    }

    @Test
    public void stencilStructure() throws IOException {
        Assume.assumeTrue("Set " + OPTION + "=1 to run this benchmark", "1".equals(System.getenv(OPTION)));
        run(10, 5, "stencil_structure.meta");
    }

    public static void main(String[] args) throws IOException {
        run(
                args.length > 0 ? Integer.parseInt(args[0]) : 10,
                args.length > 1 ? Integer.parseInt(args[1]) : 5,
                args.length > 2 ? args[2] : "stencil_structure.meta");
    }

    private static void run(int stencils, int runs, String resource) throws IOException {
        Mesh3dUtil.initializeMeshes();
        List<StructureTile> structure = load(resource);

        // Other seeds than the measured ones, so the cache doesn't already hold their shapes
        for (int run = 0; run < runs; run++) new StencilBenchmark().run(structure, 1000 + run, stencils);

        StencilBenchmark total = new StencilBenchmark();
        long checksum = 0;
        long start = System.nanoTime();
        for (int run = 0; run < runs; run++) checksum = checksum * 31 + total.run(structure, run, stencils);
        long elapsed = System.nanoTime() - start;
        System.out.printf(
                "total %d ms: fit %d ms, space %d ms, cut %d ms, checksum %d%n",
                elapsed / 1_000_000,
                total.fitNanos / 1_000_000,
                total.spaceNanos / 1_000_000,
                total.cutNanos / 1_000_000,
                checksum);
    }

    /** Places the structure and stencils it, returning a checksum of the tiles left. */
    private long run(List<StructureTile> structure, long seed, int stencils) {
        Map<Vector3i, List<LittleTile>> blocks = new HashMap<>();
        for (StructureTile tile : structure) {
            for (Map.Entry<Vector3i, LittleTileBlock> fragment : fragments(tile, new Vector3i(), null).entrySet()) {
                blocks.computeIfAbsent(fragment.getKey(), key -> new ArrayList<>()).add(fragment.getValue());
            }
        }

        Random random = new Random(seed);
        for (int i = 0; i < stencils; i++) {
            Vector3i offset = new Vector3i(
                    random.nextInt(2 * MAX_OFFSET.x + 1) - MAX_OFFSET.x,
                    random.nextInt(2 * MAX_OFFSET.y + 1) - MAX_OFFSET.y,
                    random.nextInt(2 * MAX_OFFSET.z + 1) - MAX_OFFSET.z);
            for (StructureTile tile : structure) {
                for (Map.Entry<Vector3i, LittleTileBlock> fragment : fragments(tile, offset, blocks).entrySet()) {
                    stencil(blocks.get(fragment.getKey()), fragment.getValue());
                }
            }
        }

        long checksum = 0;
        for (List<LittleTile> tiles : blocks.values()) {
            for (LittleTile tile : tiles) {
                checksum += tile.boundingBox.hashCode() * 31L + checksum(tile.getCutoutInfo());
            }
        }
        return checksum;
    }

    /** Like {@link LittleTileCutoutInfo#hashCode}, but the same in every run, as enums hash by identity. */
    private static long checksum(LittleTileCutoutInfo cutout) {
        if (cutout == null) return 0;
        long checksum = Objects.hash(
                cutout.type.ordinal(),
                cutout.size,
                cutout.pos,
                cutout.orientation,
                cutout.thickness,
                cutout.faceStart.ordinal(),
                cutout.faceEnd.ordinal(),
                cutout.negX,
                cutout.negY,
                cutout.negZ,
                Arrays.hashCode(cutout.corners),
                cutout.inverted);
        for (LittleTileCutoutInfo further : cutout.furtherShapes) checksum = checksum * 31 + checksum(further);
        return checksum;
    }

    /**
     * The tile moved by the offset, split into the blocks it reaches into and fitted, see createFragment. Stencil only
     * cuts into blocks holding tiles, and placement skips the others before fitting: with {@code blocks}, only those
     * still holding tiles get a fragment.
     */
    private Map<Vector3i, LittleTileBlock> fragments(StructureTile tile, Vector3i offset,
            Map<Vector3i, List<LittleTile>> blocks) {
        long start = System.nanoTime();
        Map<Vector3i, LittleTileBlock> fragments = new LinkedHashMap<>();
        LittleTileBox moved = new LittleTileBox(
                tile.box.minX + offset.x,
                tile.box.minY + offset.y,
                tile.box.minZ + offset.z,
                tile.box.maxX + offset.x,
                tile.box.maxY + offset.y,
                tile.box.maxZ + offset.z);
        for (int x = Math.floorDiv(moved.minX, PIXELS); x * PIXELS < moved.maxX; x++) {
            for (int y = Math.floorDiv(moved.minY, PIXELS); y * PIXELS < moved.maxY; y++) {
                for (int z = Math.floorDiv(moved.minZ, PIXELS); z * PIXELS < moved.maxZ; z++) {
                    Vector3i pos = new Vector3i(x, y, z);
                    if (blocks != null && (!blocks.containsKey(pos) || blocks.get(pos).isEmpty())) continue;
                    LittleTileBox box = new LittleTileBox(
                            Math.max(moved.minX, x * PIXELS) - x * PIXELS,
                            Math.max(moved.minY, y * PIXELS) - y * PIXELS,
                            Math.max(moved.minZ, z * PIXELS) - z * PIXELS,
                            Math.min(moved.maxX, x * PIXELS + PIXELS) - x * PIXELS,
                            Math.min(moved.maxY, y * PIXELS + PIXELS) - y * PIXELS,
                            Math.min(moved.maxZ, z * PIXELS + PIXELS) - z * PIXELS);
                    LittleTileCutoutInfo cutout = null;
                    if (tile.cutout != null) {
                        // Relative to the fragment's box rather than the whole tile's
                        cutout = new LittleTileCutoutInfo(tile.cutout);
                        cutout.pos.add(
                                moved.minX - x * PIXELS - box.minX,
                                moved.minY - y * PIXELS - box.minY,
                                moved.minZ - z * PIXELS - box.minZ);
                        TileCoverage coverage = Mesh3dUtil.fitTile(box, cutout);
                        if (coverage == TileCoverage.EMPTY) continue;
                        if (coverage == TileCoverage.FULL) cutout = null;
                    }
                    LittleTileBlock fragment = new LittleTileBlock();
                    fragment.boundingBox = box;
                    fragment.setCutoutInfo(cutout);
                    fragment.updateCorner();
                    fragments.put(pos, fragment);
                }
            }
        }
        fitNanos += System.nanoTime() - start;
        return fragments;
    }

    /** What stencil does in a block, see PreviewTile#placeTile and placeTileModeOverwrite. */
    private void stencil(List<LittleTile> tiles, LittleTileBlock stencil) {
        long start = System.nanoTime();
        boolean space = true;
        for (LittleTile tile : tiles) {
            if (MeshCollision.intersects(
                    stencil.boundingBox,
                    stencil.getCutoutInfo(),
                    tile.boundingBox,
                    tile.getCutoutInfo())) {
                space = false;
                break;
            }
        }
        long cutStart = System.nanoTime();
        spaceNanos += cutStart - start;
        if (space) return;

        List<LittleTile> overlapping = new ArrayList<>();
        for (LittleTile tile : tiles) {
            if (tile.collidesWith(stencil)) overlapping.add(tile);
        }
        for (LittleTile tile : overlapping) {
            tiles.remove(tile);
            tiles.addAll(tile.splitByTile(stencil));
            tiles.addAll(tile.antiTilesAgainst(stencil));
        }
        cutNanos += System.nanoTime() - cutStart;
    }

    /** The tiles of a structure dumped by {@code /ltdumpmesh}, in world pixels. */
    private static List<StructureTile> load(String resource) throws IOException {
        List<Map<String, String>> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(StencilBenchmark.class.getResourceAsStream(resource)))) {
            Map<String, String> entry = null;
            for (String line; (line = reader.readLine()) != null;) {
                int split = line.indexOf('=');
                if (split < 0) continue;
                String key = line.substring(0, split), value = line.substring(split + 1);
                if (key.equals("tile.index")) entries.add(entry = new HashMap<>());
                else if (entry != null) entry.put(key, value);
            }
        }
        List<StructureTile> tiles = new ArrayList<>();
        for (Map<String, String> entry : entries) {
            Vector3i block = vector(entry.get("tileEntity.blockPos"));
            Vector3i min = vector(entry.get("boundingBox.grid.min")).add(new Vector3i(block).mul(PIXELS));
            Vector3i max = vector(entry.get("boundingBox.grid.max")).add(new Vector3i(block).mul(PIXELS));
            LittleTileBox box = new LittleTileBox(min.x, min.y, min.z, max.x, max.y, max.z);
            LittleTileCutoutInfo cutout = null;
            if (entry.containsKey("cutout.type")) {
                cutout = cutout(entry, "cutout.");
                for (int i = 0; entry.containsKey("cutout.further." + i + ".type"); i++) {
                    cutout.furtherShapes.add(cutout(entry, "cutout.further." + i + "."));
                }
            }
            tiles.add(new StructureTile(box, cutout));
        }
        return tiles;
    }

    private static LittleTileCutoutInfo cutout(Map<String, String> entry, String prefix) {
        LittleTileCutoutInfo cutout = new LittleTileCutoutInfo();
        cutout.type = LittleTileShapeMode.valueOf(entry.get(prefix + "type"));
        cutout.size = vector(entry.get(prefix + "size"));
        cutout.pos = vector(entry.get(prefix + "pos"));
        cutout.orientation = Integer.parseInt(entry.get(prefix + "orientation"));
        cutout.thickness = Integer.parseInt(entry.get(prefix + "thickness"));
        cutout.faceStart = ForgeDirection.valueOf(entry.get(prefix + "faceStart"));
        cutout.faceEnd = ForgeDirection.valueOf(entry.get(prefix + "faceEnd"));
        String[] negative = entry.get(prefix + "negativeAxes").split(",");
        cutout.negX = Boolean.parseBoolean(negative[0]);
        cutout.negY = Boolean.parseBoolean(negative[1]);
        cutout.negZ = Boolean.parseBoolean(negative[2]);
        cutout.inverted = Boolean.parseBoolean(entry.get(prefix + "inverted"));
        if (entry.containsKey(prefix + "corner.0")) {
            cutout.corners = new Vector3i[Mesh3dUtil.DEFORMED_BOX_CORNER_COUNT];
            for (int i = 0; i < cutout.corners.length; i++) {
                cutout.corners[i] = vector(entry.get(prefix + "corner." + i));
            }
        }
        return cutout;
    }

    private static Vector3i vector(String value) {
        String[] parts = value.split(",");
        return new Vector3i(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }
}

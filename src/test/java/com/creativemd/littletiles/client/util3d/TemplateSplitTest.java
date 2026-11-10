package com.creativemd.littletiles.client.util3d;

import static com.creativemd.littletiles.client.util3d.Message.check;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.creativemd.creativecore.lib.Vector3d;

/**
 * The convex pieces templates are split into when loading, see {@link Mesh3dTemplateLoader#load}, checked against the
 * concave slope as it was split offline before.
 */
public class TemplateSplitTest {

    /** The rounding of doubles, in a template's unit cube. */
    private static final double EPSILON = 1.0E-9;
    private static final String[] TEMPLATES = { "slope", "slope_concave", "slope_convex", "slope_convex_inner",
            "slope_convex_outer", "slope_triangle", "slope_triangle_corner", "slope_triangle_alternate", "slope_outer",
            "slope_inner" };
    /** The surface of the concave slope, then the convex pieces it was split into offline. */
    private static final String OFFLINE_SPLIT = "slope_concave_pieces.obj";

    @Test
    public void offlineSplitIsValid() throws IOException {
        List<Mesh3d> groups = loadGroups(OFFLINE_SPLIT);
        checkSplit("offline", groups.get(0), groups.subList(1, groups.size()));
    }

    @Test(expected = AssertionError.class)
    public void splitMissingAPieceIsInvalid() throws IOException {
        List<Mesh3d> groups = loadGroups(OFFLINE_SPLIT);
        checkSplit("offline less a piece", groups.get(0), groups.subList(1, groups.size() - 1));
    }

    @Test
    public void concaveSlopeSplitsIntoNoMorePiecesThanOffline() throws IOException {
        int offline = loadGroups(OFFLINE_SPLIT).size() - 1;
        int pieces = Mesh3dTemplateLoader.load("slope_concave").pieces.size();
        check(pieces > 0 && pieces <= offline, "concave slope split into " + pieces + " pieces, offline " + offline);
    }

    @Test
    public void everyTemplateSplitsIntoConvexPiecesFillingIt() {
        for (String name : TEMPLATES) {
            Mesh3dTemplateLoader.Template template = Mesh3dTemplateLoader.load(name);
            checkSplit(name, template.surface, template.pieces);
        }
    }

    /** Each piece convex, wound outwards and within the surface, no two overlapping, and together filling it. */
    private static void checkSplit(String name, Mesh3d surface, List<Mesh3d> pieces) {
        double volume = 0;
        for (int i = 0; i < pieces.size(); i++) {
            Mesh3d piece = pieces.get(i);
            String prefix = name + ": piece " + i;
            check(Mesh3dTemplateLoader.isConvex(piece.getTriangles()), prefix + " is not convex");
            check(piece.volume() > EPSILON, prefix + " is wound inside out or flat");
            double within = volumeWithin(surface, piece);
            check(
                    Math.abs(within - piece.volume()) <= EPSILON,
                    prefix + " reaches out of the surface: " + within + " of " + piece.volume() + " within");
            for (int j = 0; j < i; j++) {
                double shared = volumeWithin(pieces.get(j), piece);
                check(shared <= EPSILON, prefix + " overlaps piece " + j + " by " + shared);
            }
            volume += piece.volume();
        }
        check(
                Math.abs(volume - surface.volume()) <= EPSILON,
                name + ": pieces fill " + volume + " of " + surface.volume());
    }

    /** The volume of the closed mesh within the convex one, or NaN if a cut fails. */
    private static double volumeWithin(Mesh3d mesh, Mesh3d convex) {
        MeshCutter cutter = new MeshCutter(mesh.getTriangles());
        for (Triangle3d face : convex.getTriangles()) {
            if (!cutter.cut(face.getFacePlane())) return Double.NaN;
        }
        return cutter.volumeTimesSix() / 6;
    }

    /** The mesh of each group of faces in the OBJ file, starting with the faces before any group. */
    private static List<Mesh3d> loadGroups(String resource) throws IOException {
        List<Vector3d> vertices = new ArrayList<>();
        List<Triangle3d> triangles = new ArrayList<>();
        List<Mesh3d> groups = new ArrayList<>();
        groups.add(new Mesh3d(triangles));
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(TemplateSplitTest.class.getResourceAsStream(resource)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tok = line.trim().split("\\s+");
                switch (tok[0]) {
                    case "v" -> vertices.add(
                            new Vector3d(
                                    Double.parseDouble(tok[1]),
                                    Double.parseDouble(tok[2]),
                                    Double.parseDouble(tok[3])));
                    case "g" -> {
                        triangles = new ArrayList<>();
                        groups.add(new Mesh3d(triangles));
                    }
                    case "f" -> {
                        // A convex polygon, split into a fan
                        Vector3d first = vertices.get(Integer.parseInt(tok[1]) - 1);
                        for (int i = 2; i + 1 < tok.length; i++) {
                            triangles.add(
                                    new Triangle3d(
                                            new Vector3d(first),
                                            new Vector3d(vertices.get(Integer.parseInt(tok[i]) - 1)),
                                            new Vector3d(vertices.get(Integer.parseInt(tok[i + 1]) - 1))));
                        }
                    }
                    default -> {}
                }
            }
        }
        return groups;
    }
}

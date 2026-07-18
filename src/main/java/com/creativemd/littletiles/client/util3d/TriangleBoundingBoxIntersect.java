/*
 * Copyright (c) 2009-2010 jMonkeyEngine All rights reserved. Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following conditions are met: * Redistributions of source code
 * must retain the above copyright notice, this list of conditions and the following disclaimer. * Redistributions in
 * binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution. * Neither the name of 'jMonkeyEngine' nor the
 * names of its contributors may be used to endorse or promote products derived from this software without specific
 * prior written permission. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.creativemd.littletiles.client.util3d;

import static java.lang.Math.max;
import static java.lang.Math.min;

import org.joml.Vector3f;

import com.creativemd.littletiles.common.utils.small.LittleTileBox;

/**
 * This class includes some utility methods for computing intersection between bounding volumes and triangles.
 *
 * @author Kirill
 */
public class TriangleBoundingBoxIntersect {

    private static void findMinMax(float f1, float f2, float f3, Vector3f minMax) {
        minMax.set(f1, f1, 0);
        if (f2 < minMax.x) minMax.x = f2;
        if (f2 > minMax.y) minMax.y = f2;
        if (f3 < minMax.x) minMax.x = f3;
        if (f3 > minMax.y) minMax.y = f3;
    }

    public static boolean intersect(Mesh3d mesh, LittleTileBox box) {
        Vector3f center = new Vector3f(
                (box.maxX + box.minX) / 16f / 2,
                (box.maxY + box.minY) / 16f / 2,
                (box.maxZ + box.minZ) / 16f / 2);
        Vector3f extend = new Vector3f(
                (box.maxX - box.minX) / 16f / 2,
                (box.maxY - box.minY) / 16f / 2,
                (box.maxZ - box.minZ) / 16f / 2);
        BoundingBox bb = new BoundingBox(center, extend);
        for (Triangle3d triangle : mesh.getTriangles()) {
            if (intersect(
                    bb,
                    triangle.getP1().toVector3f(),
                    triangle.getP2().toVector3f(),
                    triangle.getP3().toVector3f())) {
                return true;
            }
        }
        return false;
    }

    public static boolean intersect(BoundingBox bbox, Vector3f v1, Vector3f v2, Vector3f v3) {
        // use separating axis theorem to test overlap between triangle and box
        // need to test for overlap in these directions:
        // 1) the {x,y,z}-directions (actually, since we use the AABB of the triangle
        // we do not even need to test these)
        // 2) normal of the triangle
        // 3) crossproduct(edge from tri, {x,y,z}-directin)
        // this gives 3x3=9 more tests

        Vector3f tmp0 = new Vector3f();
        Vector3f tmp1 = new Vector3f();
        Vector3f tmp2 = new Vector3f();

        Vector3f e0 = new Vector3f();
        Vector3f e1 = new Vector3f();
        Vector3f e2 = new Vector3f();

        Vector3f center = bbox.getCenter();
        Vector3f extent = bbox.getExtent();

        // This is the fastest branch on Sun
        // move everything so that the boxcenter is in (0,0,0)
        v1.sub(center, tmp0);
        v2.sub(center, tmp1);
        v3.sub(center, tmp2);

        // compute triangle edges
        tmp1.sub(tmp0, e0); // tri edge 0
        tmp2.sub(tmp1, e1); // tri edge 1
        tmp0.sub(tmp2, e2); // tri edge 2

        // Bullet 3:
        // test the 9 tests first (this was faster)
        float min, max;
        float p0, p1, p2, rad;
        float fex = Math.abs(e0.x);
        float fey = Math.abs(e0.y);
        float fez = Math.abs(e0.z);

        // AXISTEST_X01(e0[Z], e0[Y], fez, fey);
        p0 = e0.z * tmp0.y - e0.y * tmp0.z;
        p2 = e0.z * tmp2.y - e0.y * tmp2.z;
        min = min(p0, p2);
        max = max(p0, p2);
        rad = fez * extent.y + fey * extent.z;
        if (min > rad || max < -rad) {
            return false;
        }

        // AXISTEST_Y02(e0[Z], e0[X], fez, fex);
        p0 = -e0.z * tmp0.x + e0.x * tmp0.z;
        p2 = -e0.z * tmp2.x + e0.x * tmp2.z;
        min = min(p0, p2);
        max = max(p0, p2);
        rad = fez * extent.x + fex * extent.z;
        if (min > rad || max < -rad) {
            return false;
        }

        // AXISTEST_Z12(e0[Y], e0[X], fey, fex);
        p1 = e0.y * tmp1.x - e0.x * tmp1.y;
        p2 = e0.y * tmp2.x - e0.x * tmp2.y;
        min = min(p1, p2);
        max = max(p1, p2);
        rad = fey * extent.x + fex * extent.y;
        if (min > rad || max < -rad) {
            return false;
        }

        fex = Math.abs(e1.x);
        fey = Math.abs(e1.y);
        fez = Math.abs(e1.z);

        // AXISTEST_X01(e1[Z], e1[Y], fez, fey);
        p0 = e1.z * tmp0.y - e1.y * tmp0.z;
        p2 = e1.z * tmp2.y - e1.y * tmp2.z;
        min = min(p0, p2);
        max = max(p0, p2);
        rad = fez * extent.y + fey * extent.z;
        if (min > rad || max < -rad) {
            return false;
        }

        // AXISTEST_Y02(e1[Z], e1[X], fez, fex);
        p0 = -e1.z * tmp0.x + e1.x * tmp0.z;
        p2 = -e1.z * tmp2.x + e1.x * tmp2.z;
        min = min(p0, p2);
        max = max(p0, p2);
        rad = fez * extent.x + fex * extent.z;
        if (min > rad || max < -rad) {
            return false;
        }

        // AXISTEST_Z0(e1[Y], e1[X], fey, fex);
        p0 = e1.y * tmp0.x - e1.x * tmp0.y;
        p1 = e1.y * tmp1.x - e1.x * tmp1.y;
        min = min(p0, p1);
        max = max(p0, p1);
        rad = fey * extent.x + fex * extent.y;
        if (min > rad || max < -rad) {
            return false;
        }
        //
        fex = Math.abs(e2.x);
        fey = Math.abs(e2.y);
        fez = Math.abs(e2.z);

        // AXISTEST_X2(e2[Z], e2[Y], fez, fey);
        p0 = e2.z * tmp0.y - e2.y * tmp0.z;
        p1 = e2.z * tmp1.y - e2.y * tmp1.z;
        min = min(p0, p1);
        max = max(p0, p1);
        rad = fez * extent.y + fey * extent.z;
        if (min > rad || max < -rad) {
            return false;
        }

        // AXISTEST_Y1(e2[Z], e2[X], fez, fex);
        p0 = -e2.z * tmp0.x + e2.x * tmp0.z;
        p1 = -e2.z * tmp1.x + e2.x * tmp1.z;
        min = min(p0, p1);
        max = max(p0, p1);
        rad = fez * extent.x + fex * extent.y;
        if (min > rad || max < -rad) {
            return false;
        }

        // AXISTEST_Z12(e2[Y], e2[X], fey, fex);
        p1 = e2.y * tmp1.x - e2.x * tmp1.y;
        p2 = e2.y * tmp2.x - e2.x * tmp2.y;
        min = min(p1, p2);
        max = max(p1, p2);
        rad = fey * extent.x + fex * extent.y;
        if (min > rad || max < -rad) {
            return false;
        }

        // Bullet 1:
        // first test overlap in the {x,y,z}-directions
        // find min, max of the triangle each direction, and test for overlap in
        // that direction -- this is equivalent to testing a minimal AABB around
        // the triangle against the AABB

        Vector3f minMax = new Vector3f();

        // test in X-direction
        findMinMax(tmp0.x, tmp1.x, tmp2.x, minMax);
        if (minMax.x > extent.x || minMax.y < -extent.x) {
            return false;
        }

        // test in Y-direction
        findMinMax(tmp0.y, tmp1.y, tmp2.y, minMax);
        if (minMax.x > extent.y || minMax.y < -extent.y) {
            return false;
        }

        // test in Z-direction
        findMinMax(tmp0.z, tmp1.z, tmp2.z, minMax);
        if (minMax.x > extent.z || minMax.y < -extent.z) {
            return false;
        }

        // // Bullet 2:
        // // test if the box intersects the plane of the triangle
        // // compute plane equation of triangle: normal * x + d = 0
        // Vector3f normal = new Vector3f();
        // e0.cross(e1, normal);
        Plane p = new Plane();

        p.setPlanePoints(v1, v2, v3);
        if (bbox.whichSide(p) == Plane.Side.Negative) {
            return false;
        }

        return true; /* box and triangle overlaps */
    }

    public static class BoundingBox {

        private final Vector3f center;
        private final Vector3f extend;

        public BoundingBox(Vector3f center, Vector3f extend) {
            this.center = center;
            this.extend = extend;
        }

        public final Vector3f getCenter() {
            return center;
        }

        public Vector3f getExtent() {
            return new Vector3f(extend);
        }

        public Plane.Side whichSide(Plane plane) {
            float radius = Math.abs(extend.x * plane.getNormal().x) + Math.abs(extend.y * plane.getNormal().y)
                    + Math.abs(extend.z * plane.getNormal().z);

            float distance = plane.pseudoDistance(center);

            // changed to < and > to prevent floating point precision problems
            if (distance < -radius) {
                return Plane.Side.Negative;
            } else if (distance > radius) {
                return Plane.Side.Positive;
            } else {
                return Plane.Side.None;
            }
        }
    }

    public static class Plane {

        public enum Side {
            None,
            Positive,
            Negative
        }

        private final Vector3f normal = new Vector3f();
        private float constant;

        public void setPlanePoints(Vector3f v1, Vector3f v2, Vector3f v3) {
            normal.set(v2).sub(v1);
            normal.cross(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z).normalize();
            constant = normal.dot(v1);
        }

        public float pseudoDistance(Vector3f point) {
            return normal.dot(point) - constant;
        }

        public Vector3f getNormal() {
            return normal;
        }
    }

}

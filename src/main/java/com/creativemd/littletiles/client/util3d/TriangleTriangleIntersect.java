/*
 * Copyright (c) 2003-2009 jMonkeyEngine All rights reserved. Redistribution and use in source and binary forms, with or
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

import org.joml.Vector2f;
import org.joml.Vector3f;

public class TriangleTriangleIntersect {

    /**
     * Floating-point tolerance for triangle distance.
     * Minor collisions smaller than this tolerance are ignored.
     */
    public static final float EPSILON = 1.0E-6f;

    private static final Vector3f tempVa = new Vector3f();

    private static final Vector3f tempVb = new Vector3f();

    private static final Vector3f tempVc = new Vector3f();

    private static final Vector3f tempVd = new Vector3f();

    private static final Vector3f tempVe = new Vector3f();

    private static final float[] tempFa = new float[2];

    private static final float[] tempFb = new float[2];

    private static final Vector2f tempV2a = new Vector2f();

    private static final Vector2f tempV2b = new Vector2f();

    /**
     * This is a <b>VERY </b> brute force method of detecting if two TriMesh objects intersect.
     *
     * @param mesh1 The first TriMesh.
     * @param mesh2 The second TriMesh.
     * @return True if the meshes overlap with volume or one mesh is contained by the other, false when they are separate
     *         or only touch at a shared surface/edge/vertex.
     */
    public static boolean meshIntersection(Mesh3d mesh1, Mesh3d mesh2) {
        Vector3f[] vertA = mesh1.getVertices();
        Vector3f[] vertB = mesh2.getVertices();

        for (int i = 0; i < mesh1.getTriangles().size(); i++) {
            for (int j = 0; j < mesh2.getTriangles().size(); j++) {
                if (intersection(
                        vertA[i * 3],
                        vertA[i * 3 + 1],
                        vertA[i * 3 + 2],
                        vertB[j * 3],
                        vertB[j * 3 + 1],
                        vertB[j * 3 + 2]))
                    return true;
            }
        }

        // No surface crossings: one mesh may still be fully contained in the other.
        // Use interior sample points (not raw vertices) so a vertex shared with the
        // other mesh's surface — e.g. two complementary slopes meeting at a face —
        // doesn't produce an ambiguous on-boundary ray test.
        if (vertA.length > 0 && mesh2.containsPoint(mesh1.getInteriorSamplePoint())) return true;
        if (vertB.length > 0 && mesh1.containsPoint(mesh2.getInteriorSamplePoint())) return true;

        return false;
    }

    /**
     * This method tests for the intersection between two triangles defined by their vertexes. Converted to java from C
     * code found at http://www.acm.org/jgt/papers/Moller97/tritri.html
     *
     * @param v0 First triangle's first vertex.
     * @param v1 First triangle's second vertex.
     * @param v2 First triangle's third vertex.
     * @param u0 Second triangle's first vertex.
     * @param u1 Second triangle's second vertex.
     * @param u2 Second triangle's third vertex.
     * @return True if the two triangles intersect, false otherwise. Just touching does not count as intersecting.
     */
    public static boolean intersection(Vector3f v0, Vector3f v1, Vector3f v2, Vector3f u0, Vector3f u1, Vector3f u2) {
        Vector3f e1 = tempVa;
        Vector3f e2 = tempVb;
        Vector3f n1 = tempVc;
        Vector3f n2 = tempVd;
        float d1, d2;
        float du0, du1, du2, dv0, dv1, dv2;
        Vector3f d = tempVe;
        float[] isect1 = tempFa;
        float[] isect2 = tempFb;
        float du0du1, du0du2, dv0dv1, dv0dv2;
        short index;
        float vp0, vp1, vp2;
        float up0, up1, up2;
        float bb, cc, max;
        float xx, yy, xxyy, tmp;

        /* compute plane equation of triangle(v0,v1,v2) */
        v1.sub(v0, e1);
        v2.sub(v0, e2);
        e1.cross(e2, n1);
        d1 = -n1.dot(v0);
        /* plane equation 1: n1.X+d1=0 */

        /*
         * put u0,u1,u2 into plane equation 1 to compute signed distances to the plane
         */
        du0 = n1.dot(u0) + d1;
        du1 = n1.dot(u1) + d1;
        du2 = n1.dot(u2) + d1;

        // Distances this small are just floating-point noise. Treat them as exactly on the plane so touching faces do
        // not look like one triangle crosses the other.
        if (Math.abs(du0) < EPSILON) du0 = 0.0f;
        if (Math.abs(du1) < EPSILON) du1 = 0.0f;
        if (Math.abs(du2) < EPSILON) du2 = 0.0f;

        du0du1 = du0 * du1;
        du0du2 = du0 * du2;

        if (!straddlesPlane(du0, du1, du2)) {
            return false;
        }

        /* compute plane of triangle (u0,u1,u2) */
        u1.sub(u0, e1);
        u2.sub(u0, e2);
        e1.cross(e2, n2);
        d2 = -n2.dot(u0);
        /* plane equation 2: n2.X+d2=0 */

        /* put v0,v1,v2 into plane equation 2 */
        dv0 = n2.dot(v0) + d2;
        dv1 = n2.dot(v1) + d2;
        dv2 = n2.dot(v2) + d2;

        // Same as above, but checking the first triangle against the second triangle's plane.
        if (Math.abs(dv0) < EPSILON) dv0 = 0.0f;
        if (Math.abs(dv1) < EPSILON) dv1 = 0.0f;
        if (Math.abs(dv2) < EPSILON) dv2 = 0.0f;

        dv0dv1 = dv0 * dv1;
        dv0dv2 = dv0 * dv2;

        if (!straddlesPlane(dv0, dv1, dv2)) {
            return false;
        }

        /* compute direction of intersection line */
        n1.cross(n2, d);

        /* compute and index to the largest component of d */
        max = Math.abs(d.x);
        index = 0;
        bb = Math.abs(d.y);
        cc = Math.abs(d.z);
        if (bb > max) {
            max = bb;
            index = 1;
        }
        if (cc > max) {
            max = cc;
            vp0 = v0.z;
            vp1 = v1.z;
            vp2 = v2.z;

            up0 = u0.z;
            up1 = u1.z;
            up2 = u2.z;

        } else if (index == 1) {
            vp0 = v0.y;
            vp1 = v1.y;
            vp2 = v2.y;

            up0 = u0.y;
            up1 = u1.y;
            up2 = u2.y;
        } else {
            vp0 = v0.x;
            vp1 = v1.x;
            vp2 = v2.x;

            up0 = u0.x;
            up1 = u1.x;
            up2 = u2.x;
        }

        /* compute interval for triangle 1 */
        Vector3f abc = tempVa;
        Vector2f x0x1 = tempV2a;
        if (newComputeIntervals(vp0, vp1, vp2, dv0, dv1, dv2, dv0dv1, dv0dv2, abc, x0x1)) {
            return coplanarTriTri(n1, v0, v1, v2, u0, u1, u2);
        }

        /* compute interval for triangle 2 */
        Vector3f def = tempVb;
        Vector2f y0y1 = tempV2b;
        if (newComputeIntervals(up0, up1, up2, du0, du1, du2, du0du1, du0du2, def, y0y1)) {
            return coplanarTriTri(n1, v0, v1, v2, u0, u1, u2);
        }

        xx = x0x1.x * x0x1.y;
        yy = y0y1.x * y0y1.y;
        xxyy = xx * yy;

        tmp = abc.x * xxyy;
        isect1[0] = tmp + abc.y * x0x1.y * yy;
        isect1[1] = tmp + abc.z * x0x1.x * yy;

        tmp = def.x * xxyy;
        isect2[0] = tmp + def.y * xx * y0y1.y;
        isect2[1] = tmp + def.z * xx * y0y1.x;

        sort(isect1);
        sort(isect2);

        // Treat endpoint contact as non-overlap. This allows complementary slopes to touch without blocking placement.
        if (isect1[1] <= isect2[0] + EPSILON || isect2[1] <= isect1[0] + EPSILON) {
            return false;
        }

        return true;
    }

    /**
     * Returns true only when the triangle crosses the plane.
     *
     * At least one vertex must be in front of the plane and at least one vertex must be behind it. Vertices exactly on
     * the plane count as touching, not crossing.
     */
    private static boolean straddlesPlane(float d0, float d1, float d2) {
        boolean hasPositive = d0 > 0.0f || d1 > 0.0f || d2 > 0.0f;
        boolean hasNegative = d0 < 0.0f || d1 < 0.0f || d2 < 0.0f;
        return hasPositive && hasNegative;
    }

    private static void sort(float[] f) {
        if (f[0] > f[1]) {
            float c = f[0];
            f[0] = f[1];
            f[1] = c;
        }
    }

    private static boolean newComputeIntervals(float vv0, float vv1, float vv2, float d0, float d1, float d2,
            float d0d1, float d0d2, Vector3f abc, Vector2f x0x1) {
        if (d0d1 > 0.0f) {
            /* here we know that d0d2 <=0.0 */
            /*
             * that is d0, d1 are on the same side, d2 on the other or on the plane
             */
            abc.x = vv2;
            abc.y = (vv0 - vv2) * d2;
            abc.z = (vv1 - vv2) * d2;
            x0x1.x = d2 - d0;
            x0x1.y = d2 - d1;
        } else if (d0d2 > 0.0f) {
            /* here we know that d0d1 <=0.0 */
            abc.x = vv1;
            abc.y = (vv0 - vv1) * d1;
            abc.z = (vv2 - vv1) * d1;
            x0x1.x = d1 - d0;
            x0x1.y = d1 - d2;
        } else if (d1 * d2 > 0.0f || d0 != 0.0f) {
            /* here we know that d0d1 <=0.0 or that d0!=0.0 */
            abc.x = vv0;
            abc.y = (vv1 - vv0) * d0;
            abc.z = (vv2 - vv0) * d0;
            x0x1.x = d0 - d1;
            x0x1.y = d0 - d2;
        } else if (d1 != 0.0f) {
            abc.x = vv1;
            abc.y = (vv0 - vv1) * d1;
            abc.z = (vv2 - vv1) * d1;
            x0x1.x = d1 - d0;
            x0x1.y = d1 - d2;
        } else if (d2 != 0.0f) {
            abc.x = vv2;
            abc.y = (vv0 - vv2) * d2;
            abc.z = (vv1 - vv2) * d2;
            x0x1.x = d2 - d0;
            x0x1.y = d2 - d1;
        } else {
            /* triangles are coplanar */
            return true;
        }
        return false;
    }

    private static boolean coplanarTriTri(Vector3f n, Vector3f v0, Vector3f v1, Vector3f v2, Vector3f u0, Vector3f u1,
            Vector3f u2) {
        if (true) {
            return false; // We treat coplanar as nontouching
        }
        Vector3f a = new Vector3f();
        short i0, i1;
        a.x = Math.abs(n.x);
        a.y = Math.abs(n.y);
        a.z = Math.abs(n.z);

        if (a.x > a.y) {
            if (a.x > a.z) {
                i0 = 1; /* a[0] is greatest */
                i1 = 2;
            } else {
                i0 = 0; /* a[2] is greatest */
                i1 = 1;
            }
        } else /* a[0] <=a[1] */ {
            if (a.z > a.y) {
                i0 = 0; /* a[2] is greatest */
                i1 = 1;
            } else {
                i0 = 0; /* a[1] is greatest */
                i1 = 2;
            }
        }

        /* test all edges of triangle 1 against the edges of triangle 2 */
        float[] v0f = new float[3];
        toArray(v0, v0f);
        float[] v1f = new float[3];
        toArray(v1, v1f);
        float[] v2f = new float[3];
        toArray(v2, v2f);
        float[] u0f = new float[3];
        toArray(u0, u0f);
        float[] u1f = new float[3];
        toArray(u1, u1f);
        float[] u2f = new float[3];
        toArray(u2, u2f);
        if (edgeAgainstTriEdges(v0f, v1f, u0f, u1f, u2f, i0, i1)) {
            return true;
        }

        if (edgeAgainstTriEdges(v1f, v2f, u0f, u1f, u2f, i0, i1)) {
            return true;
        }

        if (edgeAgainstTriEdges(v2f, v0f, u0f, u1f, u2f, i0, i1)) {
            return true;
        }

        /* finally, test if tri1 is totally contained in tri2 or vice versa */
        pointInTri(v0f, u0f, u1f, u2f, i0, i1);
        pointInTri(u0f, v0f, v1f, v2f, i0, i1);

        return false;
    }

    private static boolean pointInTri(float[] V0, float[] U0, float[] U1, float[] U2, int i0, int i1) {
        float a, b, c, d0, d1, d2;
        /* is T1 completly inside T2? */
        /* check if V0 is inside tri(U0,U1,U2) */
        a = U1[i1] - U0[i1];
        b = -(U1[i0] - U0[i0]);
        c = -a * U0[i0] - b * U0[i1];
        d0 = a * V0[i0] + b * V0[i1] + c;

        a = U2[i1] - U1[i1];
        b = -(U2[i0] - U1[i0]);
        c = -a * U1[i0] - b * U1[i1];
        d1 = a * V0[i0] + b * V0[i1] + c;

        a = U0[i1] - U2[i1];
        b = -(U0[i0] - U2[i0]);
        c = -a * U2[i0] - b * U2[i1];
        d2 = a * V0[i0] + b * V0[i1] + c;
        if (d0 * d1 > 0.0 && d0 * d2 > 0.0) return true;

        return false;
    }

    private static boolean edgeAgainstTriEdges(float[] v0, float[] v1, float[] u0, float[] u1, float[] u2, int i0,
            int i1) {
        float aX, aY;
        aX = v1[i0] - v0[i0];
        aY = v1[i1] - v0[i1];
        /* test edge u0,u1 against v0,v1 */
        if (edgeEdgeTest(v0, u0, u1, i0, i1, aX, aY)) {
            return true;
        }
        /* test edge u1,u2 against v0,v1 */
        if (edgeEdgeTest(v0, u1, u2, i0, i1, aX, aY)) {
            return true;
        }
        /* test edge u2,u1 against v0,v1 */
        if (edgeEdgeTest(v0, u2, u0, i0, i1, aX, aY)) {
            return true;
        }
        return false;
    }

    private static boolean edgeEdgeTest(float[] v0, float[] u0, float[] u1, int i0, int i1, float aX, float Ay) {
        float Bx = u0[i0] - u1[i0];
        float By = u0[i1] - u1[i1];
        float Cx = v0[i0] - u0[i0];
        float Cy = v0[i1] - u0[i1];
        float f = Ay * Bx - aX * By;
        float d = By * Cx - Bx * Cy;
        if ((f > 0 && d >= 0 && d <= f) || (f < 0 && d <= 0 && d >= f)) {
            float e = aX * Cy - Ay * Cx;
            if (f > 0) {
                if (e >= 0 && e <= f) return true;
            } else {
                if (e <= 0 && e >= f) return true;
            }
        }
        return false;
    }

    static void toArray(Vector3f vec, float[] arr) {
        arr[0] = vec.x;
        arr[1] = vec.y;
        arr[2] = vec.z;
    }
}

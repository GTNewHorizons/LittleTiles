/*
 * Copyright (c) 2009-2021 jMonkeyEngine All rights reserved. Redistribution and use in source and binary forms, with or
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

import net.minecraft.util.Vec3;

import org.joml.Vector3f;

public final class TriangleRayIntersect {

    private static float FLT_EPSILON = 0.0000001f;

    public static float intersects(Mesh3d mesh, int blockX, int blockY, int blockZ, Vec3 start, Vec3 end) {
        float ret = Float.POSITIVE_INFINITY;
        Vector3f origin = new Vector3f((float) start.xCoord, (float) start.yCoord, (float) start.zCoord);
        Vector3f direction = new Vector3f((float) end.xCoord, (float) end.yCoord, (float) end.zCoord);
        origin.sub(blockX, blockY, blockZ);
        direction.sub(blockX, blockY, blockZ);
        direction.sub(origin);

        for (Triangle3d triangle : mesh.getTriangles()) {
            float distance = intersects(
                    origin,
                    direction,
                    triangle.getP1().toVector3f(),
                    triangle.getP2().toVector3f(),
                    triangle.getP3().toVector3f());
            if (distance < ret) {
                ret = distance;
            }
        }

        return ret;
    }

    public static float intersects(Vector3f origin, Vector3f direction, Vector3f v0, Vector3f v1, Vector3f v2) {
        float edge1X = v1.x - v0.x;
        float edge1Y = v1.y - v0.y;
        float edge1Z = v1.z - v0.z;

        float edge2X = v2.x - v0.x;
        float edge2Y = v2.y - v0.y;
        float edge2Z = v2.z - v0.z;

        float normX = ((edge1Y * edge2Z) - (edge1Z * edge2Y));
        float normY = ((edge1Z * edge2X) - (edge1X * edge2Z));
        float normZ = ((edge1X * edge2Y) - (edge1Y * edge2X));

        float dirDotNorm = direction.x * normX + direction.y * normY + direction.z * normZ;

        float diffX = origin.x - v0.x;
        float diffY = origin.y - v0.y;
        float diffZ = origin.z - v0.z;

        float sign;
        if (dirDotNorm > FLT_EPSILON) {
            sign = 1;
        } else if (dirDotNorm < -FLT_EPSILON) {
            sign = -1f;
            dirDotNorm = -dirDotNorm;
        } else {
            // ray and triangle/quad are parallel
            return Float.POSITIVE_INFINITY;
        }

        float diffEdge2X = ((diffY * edge2Z) - (diffZ * edge2Y));
        float diffEdge2Y = ((diffZ * edge2X) - (diffX * edge2Z));
        float diffEdge2Z = ((diffX * edge2Y) - (diffY * edge2X));

        float dirDotDiffxEdge2 = sign
                * (direction.x * diffEdge2X + direction.y * diffEdge2Y + direction.z * diffEdge2Z);

        if (dirDotDiffxEdge2 >= 0.0f) {
            diffEdge2X = ((edge1Y * diffZ) - (edge1Z * diffY));
            diffEdge2Y = ((edge1Z * diffX) - (edge1X * diffZ));
            diffEdge2Z = ((edge1X * diffY) - (edge1Y * diffX));

            float dirDotEdge1xDiff = sign
                    * (direction.x * diffEdge2X + direction.y * diffEdge2Y + direction.z * diffEdge2Z);

            if (dirDotEdge1xDiff >= 0.0f) {
                if (dirDotDiffxEdge2 + dirDotEdge1xDiff <= dirDotNorm) {
                    float diffDotNorm = -sign * (diffX * normX + diffY * normY + diffZ * normZ);
                    if (diffDotNorm >= 0.0f) {
                        // ray intersects triangle
                        // fill in.
                        float inv = 1f / dirDotNorm;
                        float t = diffDotNorm * inv;
                        return t;
                    }
                }
            }
        }

        return Float.POSITIVE_INFINITY;
    }
}

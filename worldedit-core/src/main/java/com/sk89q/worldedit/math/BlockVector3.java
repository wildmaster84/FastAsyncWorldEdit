/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.math;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ComparisonChain;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

import javax.annotation.Nullable;
import java.util.Comparator;

/**
 * An immutable 3-dimensional vector.
 */
public abstract class BlockVector3 {

    public static final BlockVector3 ZERO = BlockVector3.at(0, 0, 0);
    public static final BlockVector3 UNIT_X = BlockVector3.at(1, 0, 0);
    public static final BlockVector3 UNIT_Y = BlockVector3.at(0, 1, 0);
    public static final BlockVector3 UNIT_Z = BlockVector3.at(0, 0, 1);
    public static final BlockVector3 UNIT_MINUS_X = BlockVector3.at(-1, 0, 0);
    public static final BlockVector3 UNIT_MINUS_Y = BlockVector3.at(0, -1, 0);
    public static final BlockVector3 UNIT_MINUS_Z = BlockVector3.at(0, 0, -1);
    public static final BlockVector3 ONE = BlockVector3.at(1, 1, 1);

    public static BlockVector3 at(double x, double y, double z) {
        return at((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    public static BlockVector3 at(int x, int y, int z) {
        return new BlockVector3Imp(x, y, z);
    }

    static final Comparator<BlockVector3> YZX_ORDER = (a, b) -> ComparisonChain.start()
            .compare(a.getY(), b.getY())
            .compare(a.getZ(), b.getZ())
            .compare(a.getX(), b.getX())
            .result();

    /**
     * Returns a comparator that sorts vectors first by Y, then Z, then X.
     *
     * <p>
     * Useful for sorting by chunk block storage order.
     */
    public static Comparator<BlockVector3> sortByCoordsYzx() {
        return YZX_ORDER;
    }

    public MutableBlockVector3 setComponents(double x, double y, double z) {
        return new MutableBlockVector3((int) x, (int) y, (int) z);
    }

    public MutableBlockVector3 setComponents(int x, int y, int z) {
        return new MutableBlockVector3(x, y, z);
    }

    public MutableBlockVector3 mutX(double x) {
        return new MutableBlockVector3((int) x, getY(), getZ());
    }

    public MutableBlockVector3 mutY(double y) {
        return new MutableBlockVector3(getX(), (int) y, getZ());
    }

    public MutableBlockVector3 mutZ(double z) {
        return new MutableBlockVector3(getX(), getY(), (int) z);
    }

    public MutableBlockVector3 mutX(int x) {
        return new MutableBlockVector3(x, getY(), getZ());
    }

    public MutableBlockVector3 mutY(int y) {
        return new MutableBlockVector3(getX(), y, getZ());
    }

    public MutableBlockVector3 mutZ(int z) {
        return new MutableBlockVector3(getX(), getY(), z);
    }

    public BlockVector3 toImmutable() {
        return BlockVector3.at(getX(), getY(), getZ());
    }

//    /**
//     * Get the BlockVector3 to the north<br>
//     * Normal use you would use north(this),
//     * To avoid constructing a new Vector, pass e.g. north(some MutableBlockVector3)
//     * There is no gaurantee it will use this provided vector
//     * @param orDefault the vector to use as the result<br>
//     * @return BlockVector3
//     */
//    public BlockVector3 north(BlockVector3 orDefault) {
//        return orDefault.setComponents(getX(), getY(), getZ() - 1);
//    }
//
//    public BlockVector3 east(BlockVector3 orDefault) {
//        return orDefault.setComponents(getX() + 1, getY(), getZ());
//    }
//
//    public BlockVector3 south(BlockVector3 orDefault) {
//        return orDefault.setComponents(getX(), getY(), getZ() + 1);
//    }
//
//    public BlockVector3 west(BlockVector3 orDefault) {
//        return orDefault.setComponents(getX() - 1, getY(), getZ());
//    }
//
//    public BlockVector3 up(BlockVector3 orDefault) {
//        return orDefault.setComponents(getX(), getY() + 1, getZ());
//    }
//
//    public BlockVector3 down(BlockVector3 orDefault) {
//        return orDefault.setComponents(getX(), getY() - 1, getZ());
//    }

    /**
     * Get the X coordinate.
     *
     * @return the x coordinate
     */
    public abstract int getX();

    /**
     * Get the X coordinate.
     *
     * @return the x coordinate
     */
    public int getBlockX() {
        return getX();
    }

    /**
     * Set the X coordinate.
     *
     * @param x the new X
     * @return a new vector
     */
    public BlockVector3 withX(int x) {
        return BlockVector3.at(x, getY(), getZ());
    }

    /**
     * Get the Y coordinate.
     *
     * @return the y coordinate
     */
    public abstract int getY();

    /**
     * Get the Y coordinate.
     *
     * @return the y coordinate
     */
    public int getBlockY() {
        return getY();
    }

    /**
     * Set the Y coordinate.
     *
     * @param y the new Y
     * @return a new vector
     */
    public BlockVector3 withY(int y) {
        return BlockVector3.at(getX(), y, getZ());
    }

    /**
     * Get the Z coordinate.
     *
     * @return the z coordinate
     */
    public abstract int getZ();

    /**
     * Get the Z coordinate.
     *
     * @return the z coordinate
     */
    public int getBlockZ() {
        return getZ();
    }

    /**
     * Set the Z coordinate.
     *
     * @param z the new Z
     * @return a new vector
     */
    public BlockVector3 withZ(int z) {
        return BlockVector3.at(getX(), getY(), z);
    }

    /**
     * Add another vector to this vector and return the result as a new vector.
     *
     * @param other the other vector
     * @return a new vector
     */
    public BlockVector3 add(BlockVector3 other) {
        return add(other.getX(), other.getY(), other.getZ());
    }

    /**
     * Add another vector to this vector and return the result as a new vector.
     *
     * @param x the value to add
     * @param y the value to add
     * @param z the value to add
     * @return a new vector
     */
    public BlockVector3 add(int x, int y, int z) {
        return BlockVector3.at(this.getX() + x, this.getY() + y, this.getZ() + z);
    }

    /**
     * Add a list of vectors to this vector and return the
     * result as a new vector.
     *
     * @param others an array of vectors
     * @return a new vector
     */
    public BlockVector3 add(BlockVector3... others) {
        int newX = getX(), newY = getY(), newZ = getZ();

        for (BlockVector3 other : others) {
            newX += other.getX();
            newY += other.getY();
            newZ += other.getZ();
        }

        return BlockVector3.at(newX, newY, newZ);
    }

    /**
     * Subtract another vector from this vector and return the result
     * as a new vector.
     *
     * @param other the other vector
     * @return a new vector
     */
    public BlockVector3 subtract(BlockVector3 other) {
        return subtract(other.getX(), other.getY(), other.getZ());
    }

    /**
     * Subtract another vector from this vector and return the result
     * as a new vector.
     *
     * @param x the value to subtract
     * @param y the value to subtract
     * @param z the value to subtract
     * @return a new vector
     */
    public BlockVector3 subtract(int x, int y, int z) {
        return BlockVector3.at(this.getX() - x, this.getY() - y, this.getZ() - z);
    }

    /**
     * Subtract a list of vectors from this vector and return the result
     * as a new vector.
     *
     * @param others an array of vectors
     * @return a new vector
     */
    public BlockVector3 subtract(BlockVector3... others) {
        int newX = getX(), newY = getY(), newZ = getZ();

        for (BlockVector3 other : others) {
            newX -= other.getX();
            newY -= other.getY();
            newZ -= other.getZ();
        }

        return BlockVector3.at(newX, newY, newZ);
    }

    /**
     * Multiply this vector by another vector on each component.
     *
     * @param other the other vector
     * @return a new vector
     */
    public BlockVector3 multiply(BlockVector3 other) {
        return multiply(other.getX(), other.getY(), other.getZ());
    }

    /**
     * Multiply this vector by another vector on each component.
     *
     * @param x the value to multiply
     * @param y the value to multiply
     * @param z the value to multiply
     * @return a new vector
     */
    public BlockVector3 multiply(int x, int y, int z) {
        return BlockVector3.at(this.getX() * x, this.getY() * y, this.getZ() * z);
    }

    /**
     * Multiply this vector by zero or more vectors on each component.
     *
     * @param others an array of vectors
     * @return a new vector
     */
    public BlockVector3 multiply(BlockVector3... others) {
        int newX = getX(), newY = getY(), newZ = getZ();

        for (BlockVector3 other : others) {
            newX *= other.getX();
            newY *= other.getY();
            newZ *= other.getZ();
        }

        return BlockVector3.at(newX, newY, newZ);
    }

    /**
     * Perform scalar multiplication and return a new vector.
     *
     * @param n the value to multiply
     * @return a new vector
     */
    public BlockVector3 multiply(int n) {
        return multiply(n, n, n);
    }

    /**
     * Divide this vector by another vector on each component.
     *
     * @param other the other vector
     * @return a new vector
     */
    public BlockVector3 divide(BlockVector3 other) {
        return divide(other.getX(), other.getY(), other.getZ());
    }

    /**
     * Divide this vector by another vector on each component.
     *
     * @param x the value to divide by
     * @param y the value to divide by
     * @param z the value to divide by
     * @return a new vector
     */
    public BlockVector3 divide(int x, int y, int z) {
        return BlockVector3.at(this.getX() / x, this.getY() / y, this.getZ() / z);
    }

    /**
     * Perform scalar division and return a new vector.
     *
     * @param n the value to divide by
     * @return a new vector
     */
    public BlockVector3 divide(int n) {
        return divide(n, n, n);
    }

    /**
     * Get the length of the vector.
     *
     * @return length
     */
    public double length() {
        return Math.sqrt(lengthSq());
    }

    /**
     * Get the length, squared, of the vector.
     *
     * @return length, squared
     */
    public int lengthSq() {
        return getX() * getX() + getY() * getY() + getZ() * getZ();
    }

    /**
     * Get the distance between this vector and another vector.
     *
     * @param other the other vector
     * @return distance
     */
    public double distance(BlockVector3 other) {
        return Math.sqrt(distanceSq(other));
    }

    /**
     * Get the distance between this vector and another vector, squared.
     *
     * @param other the other vector
     * @return distance
     */
    public int distanceSq(BlockVector3 other) {
        int dx = other.getX() - getX();
        int dy = other.getY() - getY();
        int dz = other.getZ() - getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Get the normalized vector, which is the vector divided by its
     * length, as a new vector.
     *
     * @return a new vector
     */
    public BlockVector3 normalize() {
        double len = length();
        double x = this.getX() / len;
        double y = this.getY() / len;
        double z = this.getZ() / len;
        return BlockVector3.at(x, y, z);
    }

    /**
     * Gets the dot product of this and another vector.
     *
     * @param other the other vector
     * @return the dot product of this and the other vector
     */
    public double dot(BlockVector3 other) {
        return getX() * other.getX() + getY() * other.getY() + getZ() * other.getZ();
    }

    /**
     * Gets the cross product of this and another vector.
     *
     * @param other the other vector
     * @return the cross product of this and the other vector
     */
    public BlockVector3 cross(BlockVector3 other) {
        return new BlockVector3Imp(
            getY() * other.getZ() - getZ() * other.getY(),
            getZ() * other.getX() - getX() * other.getZ(),
            getX() * other.getY() - getY() * other.getX()
        );
    }

    /**
     * Checks to see if a vector is contained with another.
     *
     * @param min the minimum point (X, Y, and Z are the lowest)
     * @param max the maximum point (X, Y, and Z are the lowest)
     * @return true if the vector is contained
     */
    public boolean containedWithin(BlockVector3 min, BlockVector3 max) {
        return getX() >= min.getX() && getX() <= max.getX() && getY() >= min.getY() && getY() <= max.getY() && getZ() >= min.getZ() && getZ() <= max.getZ();
    }

    /**
     * Clamp the Y component.
     *
     * @param min the minimum value
     * @param max the maximum value
     * @return a new vector
     */
    public BlockVector3 clampY(int min, int max) {
        checkArgument(min <= max, "minimum cannot be greater than maximum");
        if (getY() < min) {
            return BlockVector3.at(getX(), min, getZ());
        }
        if (getY() > max) {
            return BlockVector3.at(getX(), max, getZ());
        }
        return this;
    }

    /**
     * Floors the values of all components.
     *
     * @return a new vector
     */
    public BlockVector3 floor() {
        // already floored, kept for feature parity with Vector3
        return this;
    }

    /**
     * Rounds all components up.
     *
     * @return a new vector
     */
    public BlockVector3 ceil() {
        // already raised, kept for feature parity with Vector3
        return this;
    }

    /**
     * Rounds all components to the closest integer.
     *
     * <p>Components &lt; 0.5 are rounded down, otherwise up.</p>
     *
     * @return a new vector
     */
    public BlockVector3 round() {
        // already rounded, kept for feature parity with Vector3
        return this;
    }

    /**
     * Returns a vector with the absolute values of the components of
     * this vector.
     *
     * @return a new vector
     */
    public BlockVector3 abs() {
        return BlockVector3.at(Math.abs(getX()), Math.abs(getY()), Math.abs(getZ()));
    }

    /**
     * Perform a 2D transformation on this vector and return a new one.
     *
     * @param angle in degrees
     * @param aboutX about which x coordinate to rotate
     * @param aboutZ about which z coordinate to rotate
     * @param translateX what to add after rotation
     * @param translateZ what to add after rotation
     * @return a new vector
     * @see AffineTransform another method to transform vectors
     */
    public BlockVector3 transform2D(double angle, double aboutX, double aboutZ, double translateX, double translateZ) {
        angle = Math.toRadians(angle);
        double x = this.getX() - aboutX;
        double z = this.getZ() - aboutZ;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x2 = x * cos - z * sin;
        double z2 = x * sin + z * cos;

        return BlockVector3.at(
            x2 + aboutX + translateX,
                getY(),
            z2 + aboutZ + translateZ
        );
    }

    /**
     * Get this vector's pitch as used within the game.
     *
     * @return pitch in radians
     */
    public double toPitch() {
        double x = getX();
        double z = getZ();

        if (x == 0 && z == 0) {
            return getY() > 0 ? -90 : 90;
        } else {
            double x2 = x * x;
            double z2 = z * z;
            double xz = Math.sqrt(x2 + z2);
            return Math.toDegrees(Math.atan(-getY() / xz));
        }
    }

    /**
     * Get this vector's yaw as used within the game.
     *
     * @return yaw in radians
     */
    public double toYaw() {
        double x = getX();
        double z = getZ();

        double t = Math.atan2(-x, z);
        double tau = 2 * Math.PI;

        return Math.toDegrees(((t + tau) % tau));
    }

    /**
     * Gets the minimum components of two vectors.
     *
     * @param v2 the second vector
     * @return minimum
     */
    public BlockVector3 getMinimum(BlockVector3 v2) {
        return new BlockVector3Imp(
                Math.min(getX(), v2.getX()),
                Math.min(getY(), v2.getY()),
                Math.min(getZ(), v2.getZ())
        );
    }

    /**
     * Gets the maximum components of two vectors.
     *
     * @param v2 the second vector
     * @return maximum
     */
    public BlockVector3 getMaximum(BlockVector3 v2) {
        return new BlockVector3Imp(
                Math.max(getX(), v2.getX()),
                Math.max(getY(), v2.getY()),
                Math.max(getZ(), v2.getZ())
        );
    }

    /*
    Methods for getting/setting blocks

    Why are these methods here?
        - Getting a block at a position requires various operations
            (bounds checks, cache checks, ensuring loaded chunk, get ChunkSection, etc.)
        - When iterating over a region, it will provide custom BlockVector3 positions
        - These override the below set/get and avoid lookups (as the iterator shifts it to the chunk level)
     */

    public boolean setOrdinal(Extent orDefault, int ordinal) {
        return orDefault.setBlock(this, BlockState.getFromOrdinal(ordinal));
    }

    public boolean setBlock(Extent orDefault, BlockState state) {
        return orDefault.setBlock(this, state);
    }

    public boolean setFullBlock(Extent orDefault, BaseBlock block) {
        return  orDefault.setBlock(this, block);
    }

    public boolean setBiome(Extent orDefault, BiomeType biome) {
        return orDefault.setBiome(getX(), getY(), getZ(), biome);
    }

    public int getOrdinal(Extent orDefault) {
        return getBlock(orDefault).getOrdinal();
    }

    public char getOrdinalChar(Extent orDefault) {
        return (char) getOrdinal(orDefault);
    }

    public BlockState getBlock(Extent orDefault) {
        return orDefault.getBlock(this);
    }

    public BaseBlock getFullBlock(Extent orDefault) {
        return orDefault.getFullBlock(this);
    }

    public CompoundTag getNbtData(Extent orDefault) {
        return orDefault.getFullBlock(getX(), getY(), getZ()).getNbtData();
    }

    public BlockState getOrdinalBelow(Extent orDefault) {
        return orDefault.getBlock(getX(), getY() - 1, getZ());
    }

    public BlockState getStateAbove(Extent orDefault) {
        return orDefault.getBlock(getX(), getY() + 1, getZ());
    }

    public BlockState getStateRelativeY(Extent orDefault, final int y) {
        return orDefault.getBlock(getX(), getY() + y, getZ());
    }


    /*
    Adapt
     */

    /**
     * Creates a 2D vector by dropping the Y component from this vector.
     *
     * @return a new {@link BlockVector2}
     */
    public BlockVector2 toBlockVector2() {
        return BlockVector2.at(getX(), getZ());
    }

    public Vector3 toVector3() {
        return Vector3.at(getX(), getY(), getZ());
    }

    @Override
    public final boolean equals(Object obj) {
        if (!(obj instanceof BlockVector3)) {
            return false;
        }

        return equals((BlockVector3) obj);
    }

    public final boolean equals(BlockVector3 other) {
        return other.getX() == this.getX() && other.getY() == this.getY() && other.getZ() == this.getZ();
    }

    @Override
    public int hashCode() {
        return (getX() ^ (getZ() << 12)) ^ (getY() << 24);
    }

    @Override
    public String toString() {
        return "(" + getX() + ", " + getY() + ", " + getZ() + ")";
    }

}

package com.example

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Architectural blueprint of the Hex-Grid Coordinate system math handler.
 * Uses Cube coordinates for 3D distance and movement on a hexagonal grid.
 */
data class HexCubeCoordinate(val q: Int, val r: Int, val s: Int) {
    init {
        require(q + r + s == 0) { "q + r + s must equal 0 for valid hex coordinates." }
    }

    operator fun plus(other: HexCubeCoordinate) = HexCubeCoordinate(q + other.q, r + other.r, s + other.s)
    operator fun minus(other: HexCubeCoordinate) = HexCubeCoordinate(q - other.q, r - other.r, s - other.s)
    
    fun distanceTo(other: HexCubeCoordinate): Int {
        return (abs(q - other.q) + abs(r - other.r) + abs(s - other.s)) / 2
    }
}

object HexGridMath {
    // 6 structural directions for hexagonal movement
    val directions = listOf(
        HexCubeCoordinate(1, 0, -1),
        HexCubeCoordinate(1, -1, 0),
        HexCubeCoordinate(0, -1, 1),
        HexCubeCoordinate(-1, 0, 1),
        HexCubeCoordinate(-1, 1, 0),
        HexCubeCoordinate(0, 1, -1)
    )

    fun getDirection(index: Int): HexCubeCoordinate = directions[index % 6]
    
    /**
     * Converts cube coordinates to pixel/world coordinates in 3D space
     * Assumes flat-topped hexagons.
     */
    fun cubeToWorld(hex: HexCubeCoordinate, size: Float): FloatArray {
        val x = size * (1.5f * hex.q)
        val z = size * ((sqrt(3f) / 2f) * hex.q + sqrt(3f) * hex.r)
        return floatArrayOf(x, 0f, z) // Y is up, movement is along X and Z axes
    }
}

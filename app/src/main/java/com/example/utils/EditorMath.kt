package com.example.utils

import android.graphics.PointF
import com.example.db.KeyframeEntity
import com.example.db.PropertyType

data class Point(val x: Float, val y: Float) {
    fun toPointF(): PointF = PointF(x, y)
}

object CurveUtils {
    
    /**
     * Ramer-Douglas-Peucker algorithm for path simplification.
     * Reduces redundant touch coordinates within an epsilon threshold
     */
    fun ramerDouglasPeucker(points: List<Point>, epsilon: Float): List<Point> {
        if (points.size < 3) return points

        var dmax = 0f
        var index = 0
        val end = points.size - 1
        for (i in 1 until end) {
            val d = perpendicularDistance(points[i], points[0], points[end])
            if (d > dmax) {
                index = i
                dmax = d
            }
        }

        return if (dmax > epsilon) {
            val recResults1 = ramerDouglasPeucker(points.subList(0, index + 1), epsilon)
            val recResults2 = ramerDouglasPeucker(points.subList(index, points.size), epsilon)
            recResults1.dropLast(1) + recResults2
        } else {
            listOf(points[0], points[end])
        }
    }

    private fun perpendicularDistance(p: Point, p1: Point, p2: Point): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        if (dx == 0f && dy == 0f) {
            return Math.hypot((p.x - p1.x).toDouble(), (p.y - p1.y).toDouble()).toFloat()
        }
        val num = Math.abs(dy * p.x - dx * p.y + p2.x * p1.y - p2.y * p1.x)
        val den = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
        return num / den
    }

    /**
     * Evaluates a single point on a Catmull-Rom spline at parameter t in [0..1]
     */
    fun catmullRomEvaluate(p0: Point, p1: Point, p2: Point, p3: Point, t: Float): Point {
        val t2 = t * t
        val t3 = t2 * t
        
        val x = 0.5f * ((2f * p1.x) +
                (-p0.x + p2.x) * t +
                (2f * p0.x - 5f * p1.x + 4f * p2.x - p3.x) * t2 +
                (-p0.x + 3f * p1.x - 3f * p2.x + p3.x) * t3)
                
        val y = 0.5f * ((2f * p1.y) +
                (-p0.y + p2.y) * t +
                (2f * p0.y - 5f * p1.y + 4f * p2.y - p3.y) * t2 +
                (-p0.y + 3f * p1.y - 3f * p2.y + p3.y) * t3)
                
        return Point(x, y)
    }

    /**
     * Generates a smooth Catmull-Rom spline from a list of simplified control points.
     */
    fun generateSpline(points: List<Point>, segmentsPerJoin: Int = 12): List<Point> {
        if (points.size < 2) return points
        
        val result = mutableListOf<Point>()
        
        val controlPoints = mutableListOf<Point>()
        controlPoints.add(points.first())
        controlPoints.addAll(points)
        controlPoints.add(points.last())
        
        for (i in 1..controlPoints.size - 3) {
            val p0 = controlPoints[i - 1]
            val p1 = controlPoints[i]
            val p2 = controlPoints[i + 1]
            val p3 = controlPoints[i + 2]
            
            val segments = if (i == controlPoints.size - 3) segmentsPerJoin else segmentsPerJoin - 1
            for (j in 0..segments) {
                val t = j.toFloat() / segmentsPerJoin
                result.add(catmullRomEvaluate(p0, p1, p2, p3, t))
            }
        }
        return result
    }

    /**
     * time progress lookup to curve speed y value
     */
    fun evaluateCurveValue(spline: List<Point>, progress: Float): Float {
        if (spline.isEmpty()) return progress
        if (spline.size == 1) return spline[0].y
        
        val targetX = progress.coerceIn(0f, 1f)
        val sorted = spline.sortedBy { it.x }
        
        if (targetX <= sorted.first().x) return sorted.first().y
        if (targetX >= sorted.last().x) return sorted.last().y
        
        for (i in 0 until sorted.size - 1) {
            val p1 = sorted[i]
            val p2 = sorted[i + 1]
            if (targetX >= p1.x && targetX <= p2.x) {
                val span = p2.x - p1.x
                if (span == 0f) return p1.y
                val k = (targetX - p1.x) / span
                return p1.y + k * (p2.y - p1.y)
            }
        }
        return targetX
    }
}

object InterpolationEngine {

    fun getDefaultValue(propertyType: PropertyType): Float {
        return when (propertyType) {
            PropertyType.POSITION_X -> 0f
            PropertyType.POSITION_Y -> 0f
            PropertyType.SCALE -> 1f
            PropertyType.ROTATION -> 0f
            PropertyType.OPACITY -> 1f
            PropertyType.ANCHOR_X -> 0.5f
            PropertyType.ANCHOR_Y -> 0.5f
            PropertyType.BRIGHTNESS -> 1f
            PropertyType.CONTRAST -> 1f
            PropertyType.SATURATION -> 1f
            PropertyType.BLUR -> 0f
            PropertyType.DISTORT -> 0f
        }
    }

    fun interpolate(
        timestampUs: Long,
        keyframes: List<KeyframeEntity>,
        propertyType: PropertyType,
        curveGetter: (Long) -> List<Point>?
    ): Float {
        val filtered = keyframes
            .filter { it.propertyType == propertyType }
            .sortedBy { it.timestampUs }

        if (filtered.isEmpty()) {
            return getDefaultValue(propertyType)
        }

        val first = filtered.first()
        if (timestampUs <= first.timestampUs) {
            return first.value
        }

        val last = filtered.last()
        if (timestampUs >= last.timestampUs) {
            return last.value
        }

        for (i in 0 until filtered.size - 1) {
            val k1 = filtered[i]
            val k2 = filtered[i + 1]
            if (timestampUs >= k1.timestampUs && timestampUs <= k2.timestampUs) {
                val span = k2.timestampUs - k1.timestampUs
                if (span == 0L) return k1.value
                val tLinear = (timestampUs - k1.timestampUs).toFloat() / span
                
                val curvePoints = curveGetter(k1.keyframeId)
                val tInterpolated = if (curvePoints != null && curvePoints.size >= 2) {
                    val spline = CurveUtils.generateSpline(curvePoints)
                    CurveUtils.evaluateCurveValue(spline, tLinear)
                } else {
                    tLinear
                }
                
                return k1.value + tInterpolated * (k2.value - k1.value)
            }
        }

        return last.value
    }
}

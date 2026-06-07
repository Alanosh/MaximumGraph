package com.example.math

import android.graphics.PointF

object CurveFitting {

    /**
     * Simplifies a list of 2D points using the Ramer-Douglas-Peucker algorithm.
     * Epsilon is the distance tolerance.
     */
    fun ramerDouglasPeucker(points: List<PointF>, epsilon: Float): List<PointF> {
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
            val results1 = ramerDouglasPeucker(points.subList(0, index + 1), epsilon)
            val results2 = ramerDouglasPeucker(points.subList(index, points.size), epsilon)
            results1.dropLast(1) + results2
        } else {
            listOf(points[0], points[end])
        }
    }

    private fun perpendicularDistance(p: PointF, lineStart: PointF, lineEnd: PointF): Float {
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y
        val mag = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (mag == 0f) {
            return Math.hypot((p.x - lineStart.x).toDouble(), (p.y - lineStart.y).toDouble()).toFloat()
        }
        return Math.abs(dy * p.x - dx * p.y + lineEnd.x * lineStart.y - lineEnd.y * lineStart.x) / mag
    }

    /**
     * Interpolates between simplified control points using Catmull-Rom Spline.
     * Generates a smooth list of points. Each segment between Q_i and Q_{i+1}
     * is divided into [subdivisions] steps.
     */
    fun catmullRomSpline(points: List<PointF>, subdivisions: Int = 20): List<PointF> {
        if (points.size < 2) return points

        // To support interpolation through all points, pad the ends
        val padded = ArrayList<PointF>(points.size + 2)
        padded.add(points.first())
        padded.addAll(points)
        padded.add(points.last())

        val result = ArrayList<PointF>()
        
        // Loop over the original points (which are in index 1 to points.size)
        for (i in 1 until padded.size - 2) {
            val p0 = padded[i - 1]
            val p1 = padded[i]
            val p2 = padded[i + 1]
            val p3 = padded[i + 2]

            // Interpolate between p1 and p2
            for (step in 0..subdivisions) {
                // If it's not the last point, skip step == subdivisions to avoid duplication
                if (step == subdivisions && i < padded.size - 3) continue
                
                val t = step.toFloat() / subdivisions
                val x = catmullRomEvaluate(p0.x, p1.x, p2.x, p3.x, t)
                val y = catmullRomEvaluate(p0.y, p1.y, p2.y, p3.y, t)
                result.add(PointF(x, y))
            }
        }

        return result
    }

    private fun catmullRomEvaluate(p0: Float, p1: Float, p2: Float, p3: Float, t: Float): Float {
        val t2 = t * t
        val t3 = t2 * t
        return 0.5f * (
            (2f * p1) +
            (-p0 + p2) * t +
            (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2 +
            (-p0 + 3f * p1 - 3f * p2 + p3) * t3
        )
    }

    /**
     * Custom speed curve evaluator. Given a custom curve (which is a list of points
     * from x=0..1 normalized to y=0..1), maps a normalized input t to an output speed t_out.
     * Uses linear lookup or closest point match for extreme low-RAM CPU safety.
     */
    fun evaluateSpeedCurve(curve: List<PointF>, t: Float): Float {
        if (curve.isEmpty()) return t
        if (curve.size == 1) return curve[0].y
        
        // Clamp t to boundary
        val ct = t.coerceIn(0f, 1f)
        
        // Find segment
        // Sort curve by x to be double safe
        val sortedCurve = curve.sortedBy { it.x }
        if (ct <= sortedCurve.first().x) return sortedCurve.first().y
        if (ct >= sortedCurve.last().x) return sortedCurve.last().y

        for (i in 0 until sortedCurve.size - 1) {
            val pA = sortedCurve[i]
            val pB = sortedCurve[i + 1]
            if (ct >= pA.x && ct <= pB.x) {
                val denom = pB.x - pA.x
                if (denom == 0f) return pA.y
                val factor = (ct - pA.x) / denom
                return pA.y + factor * (pB.y - pA.y)
            }
        }
        return ct
    }
}

// Dumps real LampRig motion to CSV for render_preview.py. Compile together with CordSimulation.kt and LampRig.kt.
package com.raiyanshahid.pulllamp
import java.io.File
fun main(args: Array<String>) {
    val rig = LampRig(cordLength = 118f, pivotX = 64f, pivotY = 0f, anchorOffsetX = 36f, anchorOffsetY = 100f)
    val out = StringBuilder()
    val fps = 60
    val total = (6.6f * fps).toInt()
    // two pulls: (start s, drag dx)
    val pulls = listOf(0.8f to 6f, 3.7f to -18f)
    for (f in 0 until total) {
        val t = f / fps.toFloat()
        var click = 0
        for ((start, dx) in pulls) {
            val a = start; val b = start + 0.25f; val c = start + 0.55f
            if (t >= a && t < c) {
                if (t - 1f / fps < a) rig.grab(rig.cord.handleX, rig.cord.handleY)
                val k = ((t - a) / (b - a)).coerceIn(0f, 1f); val e = k * k * (3 - 2 * k)
                val tx = rig.cord.x[0] + dx * e
                val ty = rig.cord.y[0] + 118f * (1f + 0.36f * e)
                rig.drag(tx, ty)
            }
            if (t >= c && t - 1f / fps < c) rig.release()
        }
        rig.step(1f / fps)
        if (rig.consumePull()) click = 1
        out.append("${rig.pendulum.angle},$click")
        for (i in 0..14) out.append(",${rig.cord.x[i]},${rig.cord.y[i]}")
        out.append("\n")
    }
    File(args[0]).writeText(out.toString())
    println("frames=$total")
}
